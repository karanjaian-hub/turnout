package com.turnout.android.data.remote.api

import com.turnout.android.data.remote.dto.*
import retrofit2.http.*

interface AiApi {

    @POST("api/ai/generate/description")
    suspend fun generateDescription(@Body request: AiPromptRequest): AiResponse

    @POST("api/ai/generate/rsvp-insights")
    suspend fun getRsvpInsights(@Body request: AiEventRequest): AiResponse

    @POST("api/ai/generate/send-time")
    suspend fun getSendTimeOptimization(@Body request: AiEventRequest): AiResponse

    @POST("api/ai/generate/capacity-forecast")
    suspend fun getCapacityForecast(@Body request: AiEventRequest): AiResponse

    @POST("api/ai/generate/followup-suggestions")
    suspend fun getFollowupSuggestions(@Body request: AiFollowupRequest): AiResponse
}
