package com.turnout.android.domain.model

/** Represents a single live RSVP event streamed from the WebSocket feed. */
data class RsvpActivity(
    val guestName: String,
    val eventName: String,
    val status: String,      // CONFIRMED | DECLINED | MAYBE | WAITLISTED
    val timestamp: String,
    // Added for the REST "recent RSVPs" dashboard endpoint — the live WebSocket feed
    // doesn't currently need this (it's usually already viewing a specific event), but
    // the dashboard's recent-activity list needs it to route a tap through to that event.
    val eventId: Long = 0L
)
