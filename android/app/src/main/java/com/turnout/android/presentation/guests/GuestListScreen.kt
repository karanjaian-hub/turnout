package com.turnout.android.presentation.guests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.runtime.Composable
import com.turnout.android.core.components.EmptyState

// TODO(Phase 5/6 - Guests): replace with real guest list, search, filters, RSVP status.
@Composable
fun GuestListScreen(eventId: Long) {
    EmptyState(
        icon = Icons.Default.People,
        title = "Guest List",
        subtitle = "Guest management for this event coming soon"
    )
}
