package com.turnout.android.domain.repository

import com.turnout.android.domain.model.Event
import com.turnout.android.core.utils.Result

interface EventRepository {
    suspend fun getEvents(status: String? = null, page: Int = 0): Result<List<Event>>
    suspend fun getEventById(id: Long): Result<Event>
    suspend fun createEvent(title: String, description: String, location: String, eventDate: String, capacity: Int): Result<Event>
    suspend fun updateEvent(id: Long, title: String, description: String, location: String, eventDate: String, capacity: Int): Result<Event>
    suspend fun deleteEvent(id: Long): Result<Unit>
    suspend fun sendInvitations(eventId: Long): Result<Unit>
}
