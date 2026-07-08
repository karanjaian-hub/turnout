package com.turnout.android.domain.repository

import android.net.Uri
import com.turnout.android.domain.model.Guest
import com.turnout.android.core.utils.Result

interface GuestRepository {
    suspend fun getGuests(eventId: Long, page: Int = 0, search: String? = null): Result<List<Guest>>
    suspend fun importCsv(eventId: Long, fileUri: Uri): Result<Map<String, Int>>
    suspend fun resendInvitation(guestId: Long): Result<Unit>
    suspend fun deleteGuest(guestId: Long): Result<Unit>
}
