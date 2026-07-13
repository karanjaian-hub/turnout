package com.turnout.android.presentation.events.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.turnout.android.core.utils.AuthStateManager
import com.turnout.android.core.utils.Result
import com.turnout.android.data.local.UserPreferences
import com.turnout.android.domain.usecase.ChangeEventStatusParams
import com.turnout.android.domain.usecase.ChangeEventStatusUseCase
import com.turnout.android.domain.usecase.CreateEventParams
import com.turnout.android.domain.usecase.CreateEventUseCase
import com.turnout.android.domain.usecase.GenerateEventDescriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateEventUiState(
    val currentStep: Int = 1,
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val eventDate: String = "", // ISO-8601, e.g. "2026-08-15T18:00:00"
    val capacity: Int = 50,
    val isLoading: Boolean = false,
    val aiLoading: Boolean = false,
    val draftSaved: Boolean = false,
    val draftRestored: Boolean = false,
    val error: String? = null,
    val stepError: String? = null
)

// Serialized to/from DataStore as JSON — mirrors the editable fields of CreateEventUiState,
// not the transient UI-only ones (isLoading, aiLoading, errors) which have no business
// surviving a process restart.
private data class DraftEventData(
    val title: String,
    val description: String,
    val location: String,
    val eventDate: String,
    val capacity: Int
)

private const val MINIMUM_CAPACITY = 10

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val createEventUseCase: CreateEventUseCase,
    private val changeEventStatusUseCase: ChangeEventStatusUseCase,
    private val generateEventDescriptionUseCase: GenerateEventDescriptionUseCase,
    private val userPreferences: UserPreferences,
    private val authStateManager: AuthStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState = _uiState.asStateFlow()

    private val gson = Gson()
    private var cachedUserId: Long? = null

    init {
        viewModelScope.launch {
            val userId = resolveUserId()
            val draftJson = userPreferences.getDraftEvent(userId).first()
            if (draftJson != null) {
                runCatching { gson.fromJson(draftJson, DraftEventData::class.java) }.getOrNull()?.let { draft ->
                    _uiState.value = _uiState.value.copy(
                        title = draft.title,
                        description = draft.description,
                        location = draft.location,
                        eventDate = draft.eventDate,
                        capacity = draft.capacity,
                        draftRestored = true
                    )
                }
            }
        }
    }

    private suspend fun resolveUserId(): Long {
        cachedUserId?.let { return it }
        val user = authStateManager.currentUser.value ?: authStateManager.currentUser.filterNotNull().first()
        return user.id.also { cachedUserId = it }
    }

    private fun autoSaveDraft() = viewModelScope.launch {
        val userId = resolveUserId()
        val state = _uiState.value
        val draft = DraftEventData(state.title, state.description, state.location, state.eventDate, state.capacity)
        userPreferences.saveDraftEvent(userId, gson.toJson(draft))
    }

    fun updateTitle(value: String) {
        _uiState.value = _uiState.value.copy(title = value, stepError = null)
        autoSaveDraft()
    }

    fun updateDescription(value: String) {
        _uiState.value = _uiState.value.copy(description = value, stepError = null)
        autoSaveDraft()
    }

    fun updateLocation(value: String) {
        _uiState.value = _uiState.value.copy(location = value, stepError = null)
        autoSaveDraft()
    }

    fun updateEventDate(value: String) {
        _uiState.value = _uiState.value.copy(eventDate = value, stepError = null)
        autoSaveDraft()
    }

    fun updateCapacity(value: Int) {
        _uiState.value = _uiState.value.copy(capacity = value.coerceAtLeast(1))
        autoSaveDraft()
    }

    fun generateDescription(notes: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(aiLoading = true)
        when (val result = generateEventDescriptionUseCase(notes)) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(aiLoading = false, description = result.data)
                autoSaveDraft()
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(aiLoading = false, error = result.message)
            }
        }
    }

    fun goToNextStep() {
        val state = _uiState.value
        val validationError = when (state.currentStep) {
            1 -> if (state.title.isBlank()) "Event title is required" else null
            2 -> when {
                state.eventDate.isBlank() -> "Please select a date and time"
                !isDateInFuture(state.eventDate) -> "Event date must be in the future"
                state.location.isBlank() -> "Location is required"
                else -> null
            }
            3 -> if (state.capacity < MINIMUM_CAPACITY) "Minimum capacity is $MINIMUM_CAPACITY" else null
            else -> null
        }

        if (validationError != null) {
            _uiState.value = state.copy(stepError = validationError)
        } else {
            _uiState.value = state.copy(currentStep = (state.currentStep + 1).coerceAtMost(4), stepError = null)
        }
    }

    fun goToPreviousStep() {
        _uiState.value = _uiState.value.copy(currentStep = (_uiState.value.currentStep - 1).coerceAtLeast(1), stepError = null)
    }

    private fun isDateInFuture(isoDate: String): Boolean =
        runCatching { java.time.LocalDateTime.parse(isoDate).isAfter(java.time.LocalDateTime.now()) }.getOrDefault(false)

    fun submit(activate: Boolean) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val state = _uiState.value

        val createResult = createEventUseCase(
            CreateEventParams(state.title, state.description, state.location, state.eventDate, state.capacity)
        )

        when (createResult) {
            is Result.Success -> {
                if (activate) {
                    changeEventStatusUseCase(ChangeEventStatusParams(createResult.data.id, "ACTIVE"))
                }
                val userId = resolveUserId()
                userPreferences.clearDraftEvent(userId)
                _uiState.value = _uiState.value.copy(isLoading = false, draftSaved = true)
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(isLoading = false, error = createResult.message)
            }
        }
    }

    fun discardDraft() = viewModelScope.launch {
        val userId = resolveUserId()
        userPreferences.clearDraftEvent(userId)
    }

    fun hasUnsavedContent(): Boolean {
        val state = _uiState.value
        return state.title.isNotBlank() || state.description.isNotBlank() || state.location.isNotBlank()
    }
}
