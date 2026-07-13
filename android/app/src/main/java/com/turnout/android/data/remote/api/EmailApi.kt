package com.turnout.android.data.remote.api

import com.turnout.android.data.remote.dto.EmailLogDto
import com.turnout.android.data.remote.dto.MessageResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EmailApi {
    @GET("api/events/{id}/email-logs")
    suspend fun getEmailLogs(@Path("id") eventId: Long): List<EmailLogDto>

    @POST("api/events/{eventId}/email-logs/{logId}/retry")
    suspend fun retryEmail(@Path("eventId") eventId: Long, @Path("logId") logId: Long): MessageResponse
}
