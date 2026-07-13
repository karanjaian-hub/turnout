package com.turnout.android.data.repository

import android.content.Context
import android.net.Uri
import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.safeApiCall
import com.turnout.android.data.remote.api.GuestApi
import com.turnout.android.domain.model.Guest
import com.turnout.android.domain.repository.GuestRepository
import com.turnout.android.domain.repository.GuestStats
import com.turnout.android.domain.repository.PagedGuests
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

private fun com.turnout.android.data.remote.dto.GuestDto.toDomain() =
    Guest(id, fullName, email, phone, rsvpStatus, eventId)

class GuestRepositoryImpl @Inject constructor(
    private val guestApi: GuestApi,
    @ApplicationContext private val context: Context
) : GuestRepository {

    override suspend fun getGuests(eventId: Long, page: Int, search: String?, status: String?): Result<PagedGuests> =
        safeApiCall {
            val response = guestApi.getGuests(eventId = eventId, page = page, search = search, status = status)
            PagedGuests(
                guests = response.content.map { it.toDomain() },
                totalElements = response.totalElements,
                totalPages = response.totalPages,
                currentPage = response.number
            )
        }

    override suspend fun getGuest(id: Long): Result<Guest> =
        safeApiCall { guestApi.getGuest(id).toDomain() }

    override suspend fun importCsv(eventId: Long, fileUri: Uri): Result<Map<String, Int>> =
        safeApiCall {
            // Copy the content:// URI into a real temp file first — MultipartBody.Part
            // needs an actual File/RequestBody, and content resolvers don't reliably
            // expose a direct file path, especially for files picked from cloud storage.
            val tempFile = File.createTempFile("guest_import", ".csv", context.cacheDir)
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("Could not read the selected file")

            val requestBody = tempFile.asRequestBody("text/csv".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

            val result = guestApi.importCsv(eventId, part)
            tempFile.delete()

            mapOf(
                "successCount" to result.successCount,
                "failureCount" to result.failureCount
            )
        }

    override suspend fun getSampleTemplate(): Result<String> =
        safeApiCall { guestApi.getSampleTemplate().string() }

    override suspend fun exportGuests(eventId: Long): Result<String> =
        safeApiCall { guestApi.exportGuests(eventId).string() }

    override suspend fun getGuestStats(eventId: Long): Result<GuestStats> =
        safeApiCall {
            val dto = guestApi.getGuestStats(eventId)
            GuestStats(dto.totalCount, dto.confirmedCount, dto.pendingCount, dto.declinedCount, dto.waitlistedCount)
        }

    override suspend fun resendInvitation(guestId: Long): Result<Unit> =
        safeApiCall { guestApi.resendInvitation(guestId); Unit }

    override suspend fun deleteGuest(guestId: Long): Result<Unit> =
        safeApiCall { guestApi.deleteGuest(guestId); Unit }
}
