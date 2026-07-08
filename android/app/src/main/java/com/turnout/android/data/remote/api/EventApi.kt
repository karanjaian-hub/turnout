package com.turnout.android.data.remote.api

import com.turnout.android.data.remote.dto.*
import retrofit2.http.*

interface EventApi {

    @GET("api/events")
    suspend fun getEvents(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PagedResponse<EventDto>

    @GET("api/events/{id}")
    suspend fun getEventById(@Path("id") id: Long): EventDto

    @POST("api/events")
    suspend fun createEvent(@Body request: CreateEventRequest): EventDto

    @PUT("api/events/{id}")
    suspend fun updateEvent(@Path("id") id: Long, @Body request: CreateEventRequest): EventDto

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Long): MessageResponse

    @POST("api/events/{id}/send-invitations")
    suspend fun sendInvitations(@Path("id") id: Long): MessageResponse
}
