package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.RsvpResult
import com.turnout.android.domain.repository.RsvpRepository
import javax.inject.Inject

data class SubmitRsvpParams(val token: String, val status: String)

class SubmitRsvpUseCase @Inject constructor(
    private val rsvpRepository: RsvpRepository
) : UseCase<SubmitRsvpParams, RsvpResult>() {
    override suspend fun invoke(params: SubmitRsvpParams): Result<RsvpResult> =
        rsvpRepository.submitRsvp(params.token, params.status)
}
