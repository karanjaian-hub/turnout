package com.turnout.android.domain.model

data class RsvpUpdateMessage(
    val eventId: Long,
    val guestId: Long,
    val guestName: String,
    val status: String,
    val timestamp: String
)

data class EmailProgressMessage(
    val eventId: Long,
    val sent: Int,
    val total: Int,
    val failed: Int
)

data class AdminAlertMessage(
    val title: String,
    val message: String,
    val severity: String
)
