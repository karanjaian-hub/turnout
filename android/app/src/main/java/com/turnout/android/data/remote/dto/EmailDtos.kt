package com.turnout.android.data.remote.dto

data class EmailLogDto(
    val id: Long,
    val guestName: String,
    val status: String, // SENT | FAILED | QUEUED | PENDING
    val timestamp: String,
    val errorMessage: String? = null
)
