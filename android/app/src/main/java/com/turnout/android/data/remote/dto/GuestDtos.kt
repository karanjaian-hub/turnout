package com.turnout.android.data.remote.dto

data class GuestDto(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String?,
    val rsvpStatus: String,
    val eventId: Long
)

data class ImportResultDto(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<ImportError>
)

data class ImportError(val row: Int, val reason: String)
