package com.turnout.android.domain.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.RsvpResult
import com.turnout.android.domain.model.RsvpValidation

interface RsvpRepository {
    suspend fun validateToken(token: String): Result<RsvpValidation>
    suspend fun submitRsvp(token: String, status: String): Result<RsvpResult>
}
