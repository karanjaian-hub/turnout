package com.turnout.android.domain.model

data class Guest(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String?,
    val rsvpStatus: String,
    val eventId: Long
)
