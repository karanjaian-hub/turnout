package com.turnout.android.domain.repository

import com.turnout.android.core.utils.Result

interface AiRepository {
    suspend fun generateEventDescription(notes: String): Result<String>
}
