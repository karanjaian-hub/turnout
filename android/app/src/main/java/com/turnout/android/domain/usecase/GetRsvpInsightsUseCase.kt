package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.AiRepository
import javax.inject.Inject

class GetRsvpInsightsUseCase @Inject constructor(
    private val aiRepository: AiRepository
) : UseCase<Long, String>() {
    override suspend fun invoke(params: Long): Result<String> = aiRepository.getRsvpInsights(params)
}
