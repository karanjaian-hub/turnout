package com.turnout.android.data.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.safeApiCall
import com.turnout.android.data.remote.api.EventApi
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.model.RsvpActivity
import com.turnout.android.domain.repository.DashboardRepository
import com.turnout.android.domain.repository.PagedEvents
import com.turnout.android.domain.repository.PlatformStats
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val eventApi: EventApi
) : DashboardRepository {

    override suspend fun getMyEvents(page: Int, size: Int): Result<PagedEvents> =
        safeApiCall {
            val response = eventApi.getEvents(page = page, size = size)
            PagedEvents(
                // EventDto and Event's fields match 1:1 — no per-field mapping needed,
                // just constructing the domain type from the same shape.
                events = response.content.map { dto ->
                    Event(
                        id = dto.id,
                        title = dto.title,
                        description = dto.description,
                        location = dto.location,
                        eventDate = dto.eventDate,
                        capacity = dto.capacity,
                        status = dto.status,
                        confirmedCount = dto.confirmedCount,
                        pendingCount = dto.pendingCount,
                        declinedCount = dto.declinedCount,
                        waitlistedCount = dto.waitlistedCount
                    )
                },
                totalElements = response.totalElements,
                totalPages = response.totalPages,
                currentPage = response.number
            )
        }

    override suspend fun getPlatformStats(): Result<PlatformStats> =
        safeApiCall {
            val dto = eventApi.getPlatformStats()
            PlatformStats(
                totalEvents = dto.totalEvents,
                totalOrganizers = dto.totalOrganizers,
                totalGuestsInvited = dto.totalGuestsInvited,
                totalConfirmedRsvps = dto.totalConfirmedRsvps,
                activeEventsCount = dto.activeEventsCount
            )
        }

    override suspend fun getRecentRsvps(): Result<List<RsvpActivity>> =
        safeApiCall {
            eventApi.getRecentRsvps().map { dto ->
                RsvpActivity(
                    guestName = dto.guestName,
                    eventName = dto.eventTitle,
                    status = dto.rsvpStatus,
                    timestamp = dto.timestamp,
                    eventId = dto.eventId
                )
            }
        }
}
