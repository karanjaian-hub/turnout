package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.EmailRepository
import javax.inject.Inject

data class RetryEmailParams(val eventId: Long, val logId: Long)

class RetryEmailUseCase @Inject constructor(
    private val emailRepository: EmailRepository
) : UseCase<RetryEmailParams, Unit>() {
    override suspend fun invoke(params: RetryEmailParams): Result<Unit> =
        emailRepository.retryEmail(params.eventId, params.logId)
}
