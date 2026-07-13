package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.RsvpValidation
import com.turnout.android.domain.repository.RsvpRepository
import javax.inject.Inject

class ValidateTokenUseCase @Inject constructor(
    private val rsvpRepository: RsvpRepository
) : UseCase<String, RsvpValidation>() {
    override suspend fun invoke(params: String): Result<RsvpValidation> = rsvpRepository.validateToken(params)
}
