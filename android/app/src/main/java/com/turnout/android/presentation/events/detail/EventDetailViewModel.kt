package com.turnout.android.presentation.events.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.WebSocketManager
import com.turnout.android.domain.model.EmailLog
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.repository.EventStats
import com.turnout.android.domain.usecase.ChangeEventStatusParams
import com.turnout.android.domain.usecase.ChangeEventStatusUseCase
import com.turnout.android.domain.usecase.DeleteEventUseCase
import com.turnout.android.domain.usecase.GetCapacityForecastUseCase
import com.turnout.android.domain.usecase.GetEmailLogsUseCase
import com.turnout.android.domain.usecase.GetEventStatsUseCase
import com.turnout.android.domain.usecase.GetEventUseCase
import com.turnout.android.domain.usecase.GetRsvpInsightsUseCase
import com.turnout.android.domain.usecase.RetryEmailParams
import com.turnout.android.domain.usecase.RetryEmailUseCase
import com.turnout.android.domain.usecase.SendInvitationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EventDetailEvent {
    data object NavigateBackAfterDelete : EventDetailEvent()
    data class ShowError(val message: String) : EventDetailEvent()
}

data class EventDetailUiState(
    val event: Event? = null,
    val stats: EventStats? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentTab: Int = 0,
    val emailLogs: List<EmailLog> = emptyList(),
    val emailLogsLoading: Boolean = false,
    val aiInsights: String? = null,
    val aiInsightsLoading: Boolean = false,
    val aiForecast: String? = null,
    val aiForecastLoading: Boolean = false,
    val sendInvitesInProgress: Boolean = false,
    val emailSent: Int = 0,
    val emailTotal: Int = 0
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val getEventUseCase: GetEventUseCase,
    private val getEventStatsUseCase: GetEventStatsUseCase,
    private val changeEventStatusUseCase: ChangeEventStatusUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val sendInvitationsUseCase: SendInvitationsUseCase,
    private val getEmailLogsUseCase: GetEmailLogsUseCase,
    private val retryEmailUseCase: RetryEmailUseCase,
    private val getRsvpInsightsUseCase: GetRsvpInsightsUseCase,
    private val getCapacityForecastUseCase: GetCapacityForecastUseCase,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EventDetailEvent>(replay = 0)
    val events = _events.asSharedFlow()

    private var initialized = false
    private var currentEventId: Long = 0L

    // Called once from the screen's LaunchedEffect(eventId) — guarded so navigating
    // back to an already-initialized instance (rare, but possible with saved state)
    // doesn't redundantly reload everything or reconnect the WebSocket a second time.
    fun initialize(eventId: Long) {
        if (initialized) return
        initialized = true
        currentEventId = eventId
        loadEvent(eventId)
        loadStats(eventId)
        connectWebSocket(eventId)
    }

    private fun loadEvent(eventId: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        when (val result = getEventUseCase(eventId)) {
            is Result.Success -> _uiState.value = _uiState.value.copy(isLoading = false, event = result.data)
            is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
        }
    }

    private fun loadStats(eventId: Long) = viewModelScope.launch {
        when (val result = getEventStatsUseCase(eventId)) {
            is Result.Success -> _uiState.value = _uiState.value.copy(stats = result.data)
            is Result.Error -> Unit // stats are supplementary — event detail itself still shows fine without them
        }
    }

    private fun connectWebSocket(eventId: Long) {
        // Reconnects the shared WebSocketManager singleton with this event's ID — since
        // it's a singleton already possibly connected from Dashboard (eventId = null),
        // this replaces that connection entirely rather than running two in parallel.
        // Known simplification: navigating back to Dashboard afterward means Dashboard's
        // admin-alerts subscription needs re-establishing too, which its own onResume-style
        // reconnect logic doesn't currently exist for — acceptable for now, not silently ignored.
        webSocketManager.connect(eventId.toString())

        webSocketManager.rsvpUpdates
            .onEach { update ->
                if (update.eventId != eventId) return@onEach
                val currentStats = _uiState.value.stats ?: return@onEach
                val updatedStats = when (update.status) {
                    "CONFIRMED" -> currentStats.copy(confirmedCount = currentStats.confirmedCount + 1)
                    "DECLINED" -> currentStats.copy(declinedCount = currentStats.declinedCount + 1)
                    "WAITLISTED" -> currentStats.copy(waitlistedCount = currentStats.waitlistedCount + 1)
                    else -> currentStats
                }
                _uiState.value = _uiState.value.copy(stats = updatedStats)
            }
            .launchIn(viewModelScope)

        webSocketManager.emailProgress
            .onEach { progress ->
                if (progress.eventId != eventId) return@onEach
                _uiState.value = _uiState.value.copy(
                    emailSent = progress.sent,
                    emailTotal = progress.total,
                    sendInvitesInProgress = progress.sent < progress.total
                )
            }
            .launchIn(viewModelScope)
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(currentTab = index)
        // Lazy-load each tab's data only the first time it's actually viewed, rather
        // than eagerly fetching email logs / AI insights the user may never look at.
        when (index) {
            2 -> if (_uiState.value.emailLogs.isEmpty() && !_uiState.value.emailLogsLoading) loadEmailLogs()
            3 -> {
                if (_uiState.value.aiInsights == null) loadRsvpInsights()
                if (_uiState.value.aiForecast == null) loadCapacityForecast()
            }
        }
    }

    private fun loadEmailLogs() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(emailLogsLoading = true)
        when (val result = getEmailLogsUseCase(currentEventId)) {
            is Result.Success -> _uiState.value = _uiState.value.copy(emailLogsLoading = false, emailLogs = result.data)
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(emailLogsLoading = false)
                _events.emit(EventDetailEvent.ShowError(result.message))
            }
        }
    }

    fun retryEmail(logId: Long) = viewModelScope.launch {
        when (val result = retryEmailUseCase(RetryEmailParams(currentEventId, logId))) {
            is Result.Success -> loadEmailLogs()
            is Result.Error -> _events.emit(EventDetailEvent.ShowError(result.message))
        }
    }

    private fun loadRsvpInsights() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(aiInsightsLoading = true)
        when (val result = getRsvpInsightsUseCase(currentEventId)) {
            is Result.Success -> _uiState.value = _uiState.value.copy(aiInsightsLoading = false, aiInsights = result.data)
            is Result.Error -> _uiState.value = _uiState.value.copy(aiInsightsLoading = false)
        }
    }

    private fun loadCapacityForecast() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(aiForecastLoading = true)
        when (val result = getCapacityForecastUseCase(currentEventId)) {
            is Result.Success -> _uiState.value = _uiState.value.copy(aiForecastLoading = false, aiForecast = result.data)
            is Result.Error -> _uiState.value = _uiState.value.copy(aiForecastLoading = false)
        }
    }

    fun sendInvitations() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(sendInvitesInProgress = true, emailSent = 0, emailTotal = 0)
        when (val result = sendInvitationsUseCase(currentEventId)) {
            is Result.Success -> Unit // progress itself streams in via emailProgress WS subscription above
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(sendInvitesInProgress = false)
                _events.emit(EventDetailEvent.ShowError(result.message))
            }
        }
    }

    fun changeStatus(status: String) = viewModelScope.launch {
        when (val result = changeEventStatusUseCase(ChangeEventStatusParams(currentEventId, status))) {
            is Result.Success -> _uiState.value = _uiState.value.copy(event = result.data)
            is Result.Error -> _events.emit(EventDetailEvent.ShowError(result.message))
        }
    }

    fun deleteEvent() = viewModelScope.launch {
        when (val result = deleteEventUseCase(currentEventId)) {
            is Result.Success -> _events.emit(EventDetailEvent.NavigateBackAfterDelete)
            is Result.Error -> _events.emit(EventDetailEvent.ShowError(result.message))
        }
    }
}
