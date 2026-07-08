package com.turnout.android.domain.model

data class Event(
    val id: Long,
    val title: String,
    val description: String,
    val location: String,
    val eventDate: String,
    val capacity: Int,
    val status: String,
    val confirmedCount: Int,
    val pendingCount: Int,
    val declinedCount: Int,
    val waitlistedCount: Int
)
