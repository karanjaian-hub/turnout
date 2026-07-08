package com.turnout.android.data.remote.dto

data class RsvpValidationDto(
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

data class RsvpSubmitRequest(val token: String, val status: String)

data class RsvpResultDto(val status: String, val waitlisted: Boolean)
