package com.turnout.android.data.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.safeApiCall
import com.turnout.android.data.remote.api.EmailApi
import com.turnout.android.domain.model.EmailLog
import com.turnout.android.domain.repository.EmailRepository
import javax.inject.Inject

class EmailRepositoryImpl @Inject constructor(
    private val emailApi: EmailApi
) : EmailRepository {

    override suspend fun getEmailLogs(eventId: Long): Result<List<EmailLog>> =
        safeApiCall {
            emailApi.getEmailLogs(eventId).map { dto ->
                EmailLog(dto.id, dto.guestName, dto.status, dto.timestamp, dto.errorMessage)
            }
        }

    override suspend fun retryEmail(eventId: Long, logId: Long): Result<Unit> =
        safeApiCall { emailApi.retryEmail(eventId, logId); Unit }
}
