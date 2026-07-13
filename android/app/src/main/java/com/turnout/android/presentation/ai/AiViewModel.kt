package com.turnout.android.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.turnout.android.core.utils.AuthStateManager
import com.turnout.android.core.utils.Result
import com.turnout.android.data.local.UserPreferences
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.usecase.GenerateEventDescriptionUseCase
import com.turnout.android.domain.usecase.GenerateInvitationUseCase
import com.turnout.android.domain.usecase.GetCapacityForecastUseCase
import com.turnout.android.domain.usecase.GetFollowupSuggestionsParams
import com.turnout.android.domain.usecase.GetFollowupSuggestionsUseCase
import com.turnout.android.domain.usecase.GetMyEventsParams
import com.turnout.android.domain.usecase.GetMyEventsUseCase
import com.turnout.android.domain.usecase.GetRsvpInsightsUseCase
import com.turnout.android.domain.usecase.GetSendTimeOptimizationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// Every feature's result is treated as ONE opaque string — this mirrors the real backend
// contract (AiResponse.result), not the guide's imagined structured per-feature fields
// (separate tagline, 4 discrete insights, numeric capacity bands, etc.), none of which
// this API actually returns.
sealed class AiFeatureState {
    data object Idle : AiFeatureState()
    data object Loading : AiFeatureState()
    data class Success(val result: String, val timestamp: Long) : AiFeatureState()
    data class Error(val message: String) : AiFeatureState()
}

private data class CachedAiResult(val result: String, val timestamp: Long)

enum class AiFeature(val key: String, val needsEventContext: Boolean) {
    DESCRIPTION("description", needsEventContext = false),
    INVITATION("invitation", needsEventContext = false),
    RSVP_INSIGHTS("rsvp_insights", needsEventContext = true),
    SEND_TIME("send_time", needsEventContext = true),
    CAPACITY_FORECAST("capacity_forecast", needsEventContext = true),
    FOLLOWUP("followup", needsEventContext = true)
}

data class AiUiState(
    val events: List<Event> = emptyList(),
    val selectedEventId: Long? = null,
    val featureStates: Map<AiFeature, AiFeatureState> = AiFeature.entries.associateWith { AiFeatureState.Idle }
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val generateEventDescriptionUseCase: GenerateEventDescriptionUseCase,
    private val generateInvitationUseCase: GenerateInvitationUseCase,
    private val getRsvpInsightsUseCase: GetRsvpInsightsUseCase,
    private val getSendTimeOptimizationUseCase: GetSendTimeOptimizationUseCase,
    private val getCapacityForecastUseCase: GetCapacityForecastUseCase,
    private val getFollowupSuggestionsUseCase: GetFollowupSuggestionsUseCase,
    private val getMyEventsUseCase: GetMyEventsUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        loadEvents()
        loadCachedResults()
    }

    private fun loadEvents() = viewModelScope.launch {
        when (val result = getMyEventsUseCase(GetMyEventsParams())) {
            is Result.Success -> {
                val events = result.data.events
                _uiState.value = _uiState.value.copy(
                    events = events,
                    selectedEventId = events.firstOrNull()?.id
                )
            }
            is Result.Error -> Unit // event picker just stays empty — non-fatal for the two event-independent features
        }
    }

    private fun loadCachedResults() = viewModelScope.launch {
        AiFeature.entries.forEach { feature ->
            val json = userPreferences.getAiResult(feature.key).first() ?: return@forEach
            val cached = runCatching { gson.fromJson(json, CachedAiResult::class.java) }.getOrNull() ?: return@forEach
            updateFeatureState(feature, AiFeatureState.Success(cached.result, cached.timestamp))
        }
    }

    fun selectEvent(eventId: Long) {
        _uiState.value = _uiState.value.copy(selectedEventId = eventId)
    }

    private fun updateFeatureState(feature: AiFeature, state: AiFeatureState) {
        _uiState.value = _uiState.value.copy(
            featureStates = _uiState.value.featureStates + (feature to state)
        )
    }

    private fun runGeneration(feature: AiFeature, block: suspend () -> Result<String>) = viewModelScope.launch {
        updateFeatureState(feature, AiFeatureState.Loading)
        when (val result = block()) {
            is Result.Success -> {
                val timestamp = System.currentTimeMillis()
                updateFeatureState(feature, AiFeatureState.Success(result.data, timestamp))
                val cached = CachedAiResult(result.data, timestamp)
                userPreferences.saveAiResult(feature.key, gson.toJson(cached))
            }
            is Result.Error -> updateFeatureState(feature, AiFeatureState.Error(result.message))
        }
    }

    fun generateDescription(notes: String) =
        runGeneration(AiFeature.DESCRIPTION) { generateEventDescriptionUseCase(notes) }

    fun generateInvitation(notes: String) =
        runGeneration(AiFeature.INVITATION) { generateInvitationUseCase(notes) }

    fun generateRsvpInsights() {
        val eventId = _uiState.value.selectedEventId ?: return
        runGeneration(AiFeature.RSVP_INSIGHTS) { getRsvpInsightsUseCase(eventId) }
    }

    fun generateSendTimeOptimization() {
        val eventId = _uiState.value.selectedEventId ?: return
        runGeneration(AiFeature.SEND_TIME) { getSendTimeOptimizationUseCase(eventId) }
    }

    fun generateCapacityForecast() {
        val eventId = _uiState.value.selectedEventId ?: return
        runGeneration(AiFeature.CAPACITY_FORECAST) { getCapacityForecastUseCase(eventId) }
    }

    fun generateFollowupSuggestions(tone: String) {
        val eventId = _uiState.value.selectedEventId ?: return
        runGeneration(AiFeature.FOLLOWUP) { getFollowupSuggestionsUseCase(GetFollowupSuggestionsParams(eventId, tone)) }
    }
}
