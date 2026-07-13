package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.AiRepository
import javax.inject.Inject

data class GetFollowupSuggestionsParams(val eventId: Long, val tone: String)

class GetFollowupSuggestionsUseCase @Inject constructor(
    private val aiRepository: AiRepository
) : UseCase<GetFollowupSuggestionsParams, String>() {
    override suspend fun invoke(params: GetFollowupSuggestionsParams): Result<String> =
        aiRepository.getFollowupSuggestions(params.eventId, params.tone)
}
