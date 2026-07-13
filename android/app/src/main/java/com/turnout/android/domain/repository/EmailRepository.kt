package com.turnout.android.domain.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.EmailLog

interface EmailRepository {
    suspend fun getEmailLogs(eventId: Long): Result<List<EmailLog>>
    suspend fun retryEmail(eventId: Long, logId: Long): Result<Unit>
}
