package com.turnout.android.presentation.dashboard

import com.turnout.android.domain.model.AdminAlertMessage
import com.turnout.android.domain.model.RsvpActivity

/**
 * Unifies two mismatched sources into one displayable shape for the Live Activity list:
 * the initial REST snapshot (RsvpActivity: guest/event/status/timestamp) and live WS
 * admin alerts (AdminAlertMessage: title/message/severity — a different shape entirely).
 * The guide's spec asks for adminAlerts to feed the same RsvpActivityItem-style list as
 * the REST recentRsvps, but doesn't reconcile the shape mismatch — this is the pragmatic
 * bridge between them, not a literal translation of one into the other.
 */
data class LiveActivityItem(
    val id: String,
    val guestName: String,
    val eventName: String,
    val status: String,
    val timestamp: String
)

fun RsvpActivity.toLiveActivityItem(): LiveActivityItem =
    LiveActivityItem(
        id = "$eventName-$guestName-$timestamp",
        guestName = guestName,
        eventName = eventName,
        status = status,
        timestamp = timestamp
    )

fun AdminAlertMessage.toLiveActivityItem(): LiveActivityItem =
    LiveActivityItem(
        id = "$title-$message-${System.currentTimeMillis()}",
        guestName = title,
        eventName = message,
        status = severity,
        timestamp = "now"
    )
