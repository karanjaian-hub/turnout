package com.turnout.android.data.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.safeApiCall
import com.turnout.android.data.remote.api.AiApi
import com.turnout.android.data.remote.dto.AiEventRequest
import com.turnout.android.data.remote.dto.AiFollowupRequest
import com.turnout.android.data.remote.dto.AiPromptRequest
import com.turnout.android.domain.repository.AiRepository
import javax.inject.Inject

// Every method returns the single "result" string as-is — AiResponse only has one
// text field, not per-feature structured DTOs. If the backend later returns structured
// fields for a given feature, that one method expands then, rather than fabricating
// fake structure across all six now.
class AiRepositoryImpl @Inject constructor(
    private val aiApi: AiApi
) : AiRepository {

    override suspend fun generateEventDescription(notes: String): Result<String> =
        safeApiCall { aiApi.generateDescription(AiPromptRequest(notes)).result }

    override suspend fun generateInvitationCopy(notes: String): Result<String> =
        safeApiCall { aiApi.generateInvitation(AiPromptRequest(notes)).result }

    override suspend fun getRsvpInsights(eventId: Long): Result<String> =
        safeApiCall { aiApi.getRsvpInsights(AiEventRequest(eventId)).result }

    override suspend fun getSendTimeOptimization(eventId: Long): Result<String> =
        safeApiCall { aiApi.getSendTimeOptimization(AiEventRequest(eventId)).result }

    override suspend fun getCapacityForecast(eventId: Long): Result<String> =
        safeApiCall { aiApi.getCapacityForecast(AiEventRequest(eventId)).result }

    override suspend fun getFollowupSuggestions(eventId: Long, tone: String): Result<String> =
        safeApiCall { aiApi.getFollowupSuggestions(AiFollowupRequest(eventId, tone)).result }
}
