package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.AiRepository
import javax.inject.Inject

class GenerateEventDescriptionUseCase @Inject constructor(
    private val aiRepository: AiRepository
) : UseCase<String, String>() {
    override suspend fun invoke(params: String): Result<String> {
        if (params.isBlank()) return Result.Error("Enter some notes about the event first")
        return aiRepository.generateEventDescription(params)
    }
}
