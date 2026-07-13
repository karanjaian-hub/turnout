package com.turnout.android.domain.model

data class EmailLog(
    val id: Long,
    val guestName: String,
    val status: String,
    val timestamp: String,
    val errorMessage: String? = null
)
