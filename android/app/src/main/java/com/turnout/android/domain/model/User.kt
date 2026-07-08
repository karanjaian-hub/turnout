package com.turnout.android.domain.model

data class User(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String,
    val tier: String
)
