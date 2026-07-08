package com.turnout.android.data.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.safeApiCall
import com.turnout.android.data.remote.api.EventApi
import com.turnout.android.data.remote.dto.ChangeStatusRequest
import com.turnout.android.data.remote.dto.CreateEventRequest
import com.turnout.android.data.remote.dto.EventDto
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.repository.EventRepository
import com.turnout.android.domain.repository.EventStats
import javax.inject.Inject

private fun EventDto.toDomain() = Event(
    id = id,
    title = title,
    description = description,
    location = location,
    eventDate = eventDate,
    capacity = capacity,
    status = status,
    confirmedCount = confirmedCount,
    pendingCount = pendingCount,
    declinedCount = declinedCount,
    waitlistedCount = waitlistedCount
)

class EventRepositoryImpl @Inject constructor(
    private val eventApi: EventApi
) : EventRepository {

    override suspend fun getEvents(status: String?, page: Int): Result<List<Event>> =
        safeApiCall {
            eventApi.getEvents(status = status, page = page).content.map { it.toDomain() }
        }

    override suspend fun getEventById(id: Long): Result<Event> =
        safeApiCall { eventApi.getEventById(id).toDomain() }

    override suspend fun createEvent(
        title: String, description: String, location: String, eventDate: String, capacity: Int
    ): Result<Event> =
        safeApiCall {
            eventApi.createEvent(CreateEventRequest(title, description, location, eventDate, capacity)).toDomain()
        }

    override suspend fun updateEvent(
        id: Long, title: String, description: String, location: String, eventDate: String, capacity: Int
    ): Result<Event> =
        safeApiCall {
            eventApi.updateEvent(id, CreateEventRequest(title, description, location, eventDate, capacity)).toDomain()
        }

    override suspend fun deleteEvent(id: Long): Result<Unit> =
        safeApiCall { eventApi.deleteEvent(id); Unit }

    override suspend fun sendInvitations(eventId: Long): Result<Unit> =
        safeApiCall { eventApi.sendInvitations(eventId); Unit }

    override suspend fun changeStatus(eventId: Long, status: String): Result<Event> =
        safeApiCall { eventApi.changeStatus(eventId, ChangeStatusRequest(status)).toDomain() }

    override suspend fun getEventStats(eventId: Long): Result<EventStats> =
        safeApiCall {
            val dto = eventApi.getEventStats(eventId)
            EventStats(
                eventId = dto.eventId,
                confirmedCount = dto.confirmedCount,
                pendingCount = dto.pendingCount,
                declinedCount = dto.declinedCount,
                waitlistedCount = dto.waitlistedCount,
                capacity = dto.capacity
            )
        }
}
