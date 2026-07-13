package com.turnout.android.domain.repository

import com.turnout.android.core.utils.Result

interface AiRepository {
    suspend fun generateEventDescription(notes: String): Result<String>
    suspend fun generateInvitationCopy(notes: String): Result<String>
    suspend fun getRsvpInsights(eventId: Long): Result<String>
    suspend fun getSendTimeOptimization(eventId: Long): Result<String>
    suspend fun getCapacityForecast(eventId: Long): Result<String>
    suspend fun getFollowupSuggestions(eventId: Long, tone: String): Result<String>
}
