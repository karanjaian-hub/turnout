package com.turnout.android.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateFormatter {

    private val displayFormatter = DateTimeFormatter
        .ofPattern("MMM d, yyyy · h:mm a")
        .withZone(ZoneId.systemDefault())

    private val shortFormatter = DateTimeFormatter
        .ofPattern("MMM d, yyyy")
        .withZone(ZoneId.systemDefault())

    fun formatDisplay(isoString: String): String = runCatching {
        displayFormatter.format(Instant.parse(isoString))
    }.getOrDefault(isoString)

    fun formatShort(isoString: String): String = runCatching {
        shortFormatter.format(Instant.parse(isoString))
    }.getOrDefault(isoString)

    /** Returns "2m ago", "3h ago", "Yesterday", etc. */
    fun timeAgo(isoString: String): String = runCatching {
        val now = Instant.now()
        val then = Instant.parse(isoString)
        val minutes = ChronoUnit.MINUTES.between(then, now)
        when {
            minutes < 1    -> "Just now"
            minutes < 60   -> "${minutes}m ago"
            minutes < 1440 -> "${minutes / 60}h ago"
            minutes < 2880 -> "Yesterday"
            else           -> formatShort(isoString)
        }
    }.getOrDefault("")
}
