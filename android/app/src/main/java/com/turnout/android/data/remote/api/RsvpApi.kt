package com.turnout.android.data.remote.api

import com.turnout.android.data.remote.dto.*
import retrofit2.http.*

interface RsvpApi {

    @GET("api/rsvp/validate")
    suspend fun validateToken(@Query("token") token: String): RsvpValidationDto

    @POST("api/rsvp/submit")
    suspend fun submitRsvp(@Body request: RsvpSubmitRequest): RsvpResultDto
}
