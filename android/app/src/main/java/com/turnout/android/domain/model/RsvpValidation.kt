package com.turnout.android.domain.model

data class RsvpValidation(
    val valid: Boolean,
    val guestName: String?,
    val eventTitle: String?,
    val eventDate: String?,
    val eventLocation: String?,
    val eventDescription: String?,
    val alreadyResponded: Boolean,
    val previousStatus: String?,
    val eventFull: Boolean
)

data class RsvpResult(val status: String, val waitlisted: Boolean)
