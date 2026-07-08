package com.turnout.android.data.remote.dto

data class AiPromptRequest(val prompt: String)
data class AiEventRequest(val eventId: Long)
data class AiFollowupRequest(val eventId: Long, val tone: String)
data class AiResponse(val result: String, val cached: Boolean)
