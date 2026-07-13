package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.EmailLog
import com.turnout.android.domain.repository.EmailRepository
import javax.inject.Inject

class GetEmailLogsUseCase @Inject constructor(
    private val emailRepository: EmailRepository
) : UseCase<Long, List<EmailLog>>() {
    override suspend fun invoke(params: Long): Result<List<EmailLog>> = emailRepository.getEmailLogs(params)
}
