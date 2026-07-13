package com.turnout.android.domain.repository

import android.net.Uri
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Guest

data class PagedGuests(
    val guests: List<Guest>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)

data class GuestStats(
    val totalCount: Int,
    val confirmedCount: Int,
    val pendingCount: Int,
    val declinedCount: Int,
    val waitlistedCount: Int
)

interface GuestRepository {
    // Was Result<List<Guest>> — silently discarded pagination metadata, which infinite
    // scroll in 6.2 genuinely needs (to know whether a next page exists at all).
    suspend fun getGuests(eventId: Long, page: Int = 0, search: String? = null, status: String? = null): Result<PagedGuests>
    suspend fun getGuest(id: Long): Result<Guest>
    suspend fun importCsv(eventId: Long, fileUri: Uri): Result<Map<String, Int>>
    suspend fun getSampleTemplate(): Result<String>
    suspend fun exportGuests(eventId: Long): Result<String>
    suspend fun getGuestStats(eventId: Long): Result<GuestStats>
    suspend fun resendInvitation(guestId: Long): Result<Unit>
    suspend fun deleteGuest(guestId: Long): Result<Unit>
}
