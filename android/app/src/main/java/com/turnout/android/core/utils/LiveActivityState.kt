package com.turnout.android.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tracks unread live-activity count for the Dashboard nav badge (built in Phase 2 as a
 * stub always reading 0). Now wired to WebSocketManager's activity via AppEventBus —
 * NewRsvpActivity carries the current cumulative count, which this just mirrors.
 */
object LiveActivityState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _wsNewActivityCount = MutableStateFlow(0)
    val wsNewActivityCount: StateFlow<Int> = _wsNewActivityCount.asStateFlow()

    init {
        scope.launch {
            AppEventBus.events.collect { event ->
                if (event is AppEvent.NewRsvpActivity) {
                    _wsNewActivityCount.value = event.count
                }
            }
        }
    }

    fun clear() {
        _wsNewActivityCount.value = 0
    }
}
