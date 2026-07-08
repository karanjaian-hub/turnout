package com.turnout.android.core.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks unread live-activity count for the Dashboard nav badge. Always 0 today — this
 * is deliberately a stub until Phase 4+ wires it to real WebSocket/Kafka events from
 * notificationservice. The badge display logic in NavGraph is fully real and correct now;
 * only the data source feeding it is a placeholder.
 */
object LiveActivityState {
    private val _wsNewActivityCount = MutableStateFlow(0)
    val wsNewActivityCount: StateFlow<Int> = _wsNewActivityCount.asStateFlow()

    fun clear() {
        _wsNewActivityCount.value = 0
    }
}
