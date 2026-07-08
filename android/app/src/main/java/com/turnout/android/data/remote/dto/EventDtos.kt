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

data class PlatformStatsDto(
    val totalEvents: Int,
    val totalOrganizers: Int,
    val totalGuestsInvited: Int,
    val totalConfirmedRsvps: Int,
    val activeEventsCount: Int
)

data class RecentRsvpDto(
    val guestName: String,
    val eventTitle: String,
    val rsvpStatus: String,
    val timestamp: String,
    val eventId: Long
)

data class ChangeStatusRequest(val status: String)

data class EventStatsDto(
    val eventId: Long,
    val confirmedCount: Int,
    val pendingCount: Int,
    val declinedCount: Int,
    val waitlistedCount: Int,
    val capacity: Int
)
