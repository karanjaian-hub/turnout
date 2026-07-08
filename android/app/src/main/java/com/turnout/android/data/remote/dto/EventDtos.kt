package com.turnout.android.data.remote.dto

data class EventDto(
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

data class CreateEventRequest(
    val title: String,
    val description: String,
    val location: String,
    val eventDate: String,
    val capacity: Int
)

data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int
)
