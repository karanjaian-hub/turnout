package com.turnout.android.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** App-wide events that cross layer boundaries (e.g. network -> navigation, websocket -> UI). */
sealed class AppEvent {
    // Fired by TokenRefreshInterceptor when refresh fails outright, or the user explicitly
    // logs out — both cases mean "go to Login and clear the back stack."
    data object Logout : AppEvent()

    // Fired the moment a 401 is first observed, before the refresh attempt even starts —
    // distinct from Logout, which only fires if refresh itself fails. Currently unconsumed
    // by any collector; reserved for a future in-flight-request-cancellation use case.
    data object TokenExpired : AppEvent()

    // Drives the Dashboard nav badge (LiveActivityState, wired in Phase 2's NavGraph).
    // count is cumulative unread count, not a delta — the collector should treat each
    // emission as "here is the current total," not "add this many."
    data class NewRsvpActivity(val count: Int) : AppEvent()
}

object AppEventBus {
    // extraBufferCapacity=8 per spec: a handful of events emitted before any collector is
    // actively listening (e.g. during a brief Activity recreation) aren't silently dropped.
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    suspend fun emit(event: AppEvent) = _events.emit(event)
}
