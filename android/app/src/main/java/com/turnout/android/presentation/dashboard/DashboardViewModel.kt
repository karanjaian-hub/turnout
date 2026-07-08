package com.turnout.android.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.AuthStateManager
import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.WebSocketManager
import com.turnout.android.core.utils.WsState
import com.turnout.android.data.local.TokenManager
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.usecase.GetMyEventsUseCase
import com.turnout.android.domain.usecase.GetPlatformStatsUseCase
import com.turnout.android.domain.usecase.GetRecentRsvpsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
    val totalEvents: Int = 0,
    val totalConfirmedRsvps: Int = 0,
    val totalGuestsInvited: Int = 0,
    val pendingRsvps: Int = 0,
    val liveActivity: List<LiveActivityItem> = emptyList(),
    val greeting: String = "",
    val contextualNote: String? = null,
    val capacityWarning: String? = null
)

private const val MAX_LIVE_ACTIVITY_ITEMS = 20
private const val CAPACITY_WARNING_THRESHOLD = 0.8

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getMyEventsUseCase: GetMyEventsUseCase,
    private val getPlatformStatsUseCase: GetPlatformStatsUseCase,
    private val getRecentRsvpsUseCase: GetRecentRsvpsUseCase,
    private val webSocketManager: WebSocketManager,
    private val tokenManager: TokenManager,
    private val authStateManager: AuthStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    val wsState = webSocketManager.state

    // Emitted once per incoming WS message — DashboardScreen collects this to trigger
    // a brief PulseLine speed-up "flash," distinct from the line's constant idle animation.
    private val _wsFlash = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val wsFlash = _wsFlash.asSharedFlow()

    init {
        loadDashboardData()
        connectWebSocket()
        // Dashboard is the destination that "clears" unread activity — arriving here
        // is treated as the user having seen whatever was pending.
        webSocketManager.clearActivityCount()
    }

    private fun loadDashboardData() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // async{} for genuine concurrency — these three calls don't depend on each
        // other, so running them sequentially would triple the wait for no reason.
        // Each Deferred keeps its own real type (not flattened into a shared list),
        // so .await() below returns properly typed Result<T> for each, no casting needed.
        val eventsDeferred = async { getMyEventsUseCase(com.turnout.android.domain.usecase.GetMyEventsParams()) }
        val statsDeferred = async { getPlatformStatsUseCase() }
        val rsvpsDeferred = async { getRecentRsvpsUseCase() }

        val eventsResult = eventsDeferred.await()
        val statsResult = statsDeferred.await()
        val rsvpsResult = rsvpsDeferred.await()

        val events = (eventsResult as? Result.Success)?.data?.events ?: emptyList()
        val stats = (statsResult as? Result.Success)?.data
        val recentRsvps = (rsvpsResult as? Result.Success)?.data

        val fullName = authStateManager.currentUser.value?.fullName
        val firstName = fullName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "there"

        _uiState.value = DashboardUiState(
            isLoading = false,
            events = events,
            totalEvents = stats?.totalEvents ?: events.size,
            totalConfirmedRsvps = stats?.totalConfirmedRsvps ?: 0,
            totalGuestsInvited = stats?.totalGuestsInvited ?: 0,
            pendingRsvps = events.sumOf { it.pendingCount },
            liveActivity = recentRsvps.orEmpty().map { it.toLiveActivityItem() }.take(MAX_LIVE_ACTIVITY_ITEMS),
            greeting = buildGreeting(firstName),
            contextualNote = findTodaysEventNote(events),
            capacityWarning = findCapacityWarning(events)
        )
    }

    private fun connectWebSocket() {
        // No eventId — Dashboard only needs the general admin-alerts stream, not a
        // specific event's rsvp-updates/email-progress subscriptions.
        webSocketManager.connect(eventId = null)

        webSocketManager.adminAlerts
            .onEach { alert ->
                _wsFlash.emit(Unit)
                val newItem = alert.toLiveActivityItem()
                _uiState.value = _uiState.value.copy(
                    liveActivity = (listOf(newItem) + _uiState.value.liveActivity).take(MAX_LIVE_ACTIVITY_ITEMS)
                )
            }
            .launchIn(viewModelScope)
    }

    private fun buildGreeting(firstName: String): String {
        val hour = LocalDate.now().let { java.time.LocalTime.now().hour }
        val timeOfDay = when {
            hour < 12 -> "morning"
            hour < 17 -> "afternoon"
            else -> "evening"
        }
        return "Good $timeOfDay, $firstName"
    }

    private fun findTodaysEventNote(events: List<Event>): String? {
        val today = LocalDate.now()
        val todaysEvent = events.firstOrNull { event ->
            runCatching { LocalDate.parse(event.eventDate.take(10)) == today }.getOrDefault(false)
        }
        return todaysEvent?.let { "— ${it.title} is today." }
    }

    private fun findCapacityWarning(events: List<Event>): String? {
        val nearCapacityEvent = events.firstOrNull { event ->
            event.status == "ACTIVE" && event.capacity > 0 &&
                (event.confirmedCount.toDouble() / event.capacity) > CAPACITY_WARNING_THRESHOLD
        } ?: return null

        val pct = ((nearCapacityEvent.confirmedCount.toDouble() / nearCapacityEvent.capacity) * 100).toInt()
        return "${nearCapacityEvent.title} is almost full ($pct%)"
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
