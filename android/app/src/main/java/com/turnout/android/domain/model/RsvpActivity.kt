package com.turnout.android.domain.model

/** Represents a single live RSVP event streamed from the WebSocket feed. */
data class RsvpActivity(
    val guestName: String,
    val eventName: String,
    val status: String,      // CONFIRMED | DECLINED | MAYBE | WAITLISTED
    val timestamp: String
)
