package com.turnout.android.data.remote.api

import com.turnout.android.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface GuestApi {
    @GET("api/guests/event/{eventId}")
    suspend fun getGuests(
        @Path("eventId") eventId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): PagedResponse<GuestDto>

    @GET("api/guests/{id}")
    suspend fun getGuest(@Path("id") id: Long): GuestDto

    @Multipart
    @POST("api/guests/bulk-import/{eventId}")
    suspend fun importCsv(
        @Path("eventId") eventId: Long,
        @Part file: MultipartBody.Part
    ): ImportResultDto

    @GET("api/guests/sample-template")
    suspend fun getSampleTemplate(): ResponseBody

    @GET("api/guests/event/{eventId}/export")
    suspend fun exportGuests(@Path("eventId") eventId: Long): ResponseBody

    @GET("api/guests/event/{eventId}/stats")
    suspend fun getGuestStats(@Path("eventId") eventId: Long): GuestStatsDto

    @POST("api/guests/{guestId}/resend-invitation")
    suspend fun resendInvitation(@Path("guestId") guestId: Long): MessageResponse

    @DELETE("api/guests/{guestId}")
    suspend fun deleteGuest(@Path("guestId") guestId: Long): MessageResponse
}
