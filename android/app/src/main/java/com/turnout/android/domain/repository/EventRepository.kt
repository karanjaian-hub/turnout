package com.turnout.android.domain.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Event

data class EventStats(
    val eventId: Long,
    val confirmedCount: Int,
    val pendingCount: Int,
    val declinedCount: Int,
    val waitlistedCount: Int,
    val capacity: Int
)

interface EventRepository {
    suspend fun getEvents(status: String? = null, page: Int = 0): Result<List<Event>>
    suspend fun getEventById(id: Long): Result<Event>
    suspend fun createEvent(title: String, description: String, location: String, eventDate: String, capacity: Int): Result<Event>
    suspend fun updateEvent(id: Long, title: String, description: String, location: String, eventDate: String, capacity: Int): Result<Event>
    suspend fun deleteEvent(id: Long): Result<Unit>
    suspend fun sendInvitations(eventId: Long): Result<Unit>
    suspend fun changeStatus(eventId: Long, status: String): Result<Event>
    suspend fun getEventStats(eventId: Long): Result<EventStats>
}
