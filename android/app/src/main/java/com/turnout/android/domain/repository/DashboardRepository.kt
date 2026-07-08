package com.turnout.android.domain.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.model.RsvpActivity

data class PlatformStats(
    val totalEvents: Int,
    val totalOrganizers: Int,
    val totalGuestsInvited: Int,
    val totalConfirmedRsvps: Int,
    val activeEventsCount: Int
)

data class PagedEvents(
    val events: List<Event>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)

interface DashboardRepository {
    suspend fun getMyEvents(page: Int, size: Int): Result<PagedEvents>
    suspend fun getPlatformStats(): Result<PlatformStats>
    suspend fun getRecentRsvps(): Result<List<RsvpActivity>>
}
