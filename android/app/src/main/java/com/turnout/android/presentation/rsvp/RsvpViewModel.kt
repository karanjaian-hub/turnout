package com.turnout.android.presentation.rsvp

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.usecase.SubmitRsvpParams
import com.turnout.android.domain.usecase.SubmitRsvpUseCase
import com.turnout.android.domain.usecase.ValidateTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RsvpUiState {
    data object Loading : RsvpUiState()
    data class Valid(
        val guestName: String,
        val eventTitle: String,
        val eventDate: String,
        val eventLocation: String
    ) : RsvpUiState()
    data class Submitting(val validState: Valid) : RsvpUiState()
    data class Success(
        val status: String, // CONFIRMED | DECLINED | WAITLISTED
        val guestName: String,
        val eventTitle: String,
        val eventDate: String,
        val eventLocation: String
    ) : RsvpUiState()
    data class AlreadyResponded(val status: String, val validState: Valid) : RsvpUiState()
    data class InvalidToken(val reason: String) : RsvpUiState()
}

private const val MINIMUM_STATE_DURATION_MS = 600L

@HiltViewModel
class RsvpViewModel @Inject constructor(
    private val validateTokenUseCase: ValidateTokenUseCase,
    private val submitRsvpUseCase: SubmitRsvpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RsvpUiState>(RsvpUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val selectedStatus = mutableStateOf<String?>(null)
    val showCalendarSheet = mutableStateOf(false)

    private var currentToken: String = ""

    fun initialize(token: String) {
        currentToken = token
        validateToken(token)
    }

    private fun validateToken(token: String) = viewModelScope.launch {
        _uiState.value = RsvpUiState.Loading
        val startTime = System.currentTimeMillis()

        val result = validateTokenUseCase(token)
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < MINIMUM_STATE_DURATION_MS) delay(MINIMUM_STATE_DURATION_MS - elapsed)

        when (result) {
            is Result.Success -> {
                val validation = result.data
                if (!validation.valid) {
                    // The API doesn't return a specific "reason" string — only a flat
                    // valid/eventFull pair. This is the most honest message we can give
                    // without inventing backend detail that doesn't exist.
                    val reason = if (validation.eventFull) {
                        "This event has reached full capacity."
                    } else {
                        "This invitation link has expired or is no longer valid."
                    }
                    _uiState.value = RsvpUiState.InvalidToken(reason)
                    return@launch
                }

                val validState = RsvpUiState.Valid(
                    guestName = validation.guestName ?: "Guest",
                    eventTitle = validation.eventTitle ?: "",
                    eventDate = validation.eventDate ?: "",
                    eventLocation = validation.eventLocation ?: ""
                )

                _uiState.value = if (validation.alreadyResponded && validation.previousStatus != null) {
                    RsvpUiState.AlreadyResponded(validation.previousStatus, validState)
                } else {
                    validState
                }
            }
            is Result.Error -> _uiState.value = RsvpUiState.InvalidToken(result.message)
        }
    }

    fun selectStatus(status: String) {
        selectedStatus.value = status
    }

    fun changeResponse() {
        val current = _uiState.value
        if (current is RsvpUiState.AlreadyResponded) {
            _uiState.value = current.validState
        }
    }

    fun submitRsvp() {
        val status = selectedStatus.value ?: return
        val current = _uiState.value
        val validState = when (current) {
            is RsvpUiState.Valid -> current
            is RsvpUiState.AlreadyResponded -> current.validState
            else -> return
        }

        viewModelScope.launch {
            _uiState.value = RsvpUiState.Submitting(validState)
            val startTime = System.currentTimeMillis()

            val result = submitRsvpUseCase(SubmitRsvpParams(currentToken, status))
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < MINIMUM_STATE_DURATION_MS) delay(MINIMUM_STATE_DURATION_MS - elapsed)

            when (result) {
                is Result.Success -> {
                    val finalStatus = if (result.data.waitlisted) "WAITLISTED" else result.data.status
                    _uiState.value = RsvpUiState.Success(
                        status = finalStatus,
                        guestName = validState.guestName,
                        eventTitle = validState.eventTitle,
                        eventDate = validState.eventDate,
                        eventLocation = validState.eventLocation
                    )
                }
                is Result.Error -> {
                    // Fall back to the selection screen with the error — resubmission
                    // should be possible rather than getting stuck on a dead end.
                    _uiState.value = validState
                }
            }
        }
    }
}
