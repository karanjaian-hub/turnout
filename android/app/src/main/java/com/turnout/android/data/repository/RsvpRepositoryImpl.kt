package com.turnout.android.data.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.safeApiCall
import com.turnout.android.data.remote.api.RsvpApi
import com.turnout.android.data.remote.dto.RsvpSubmitRequest
import com.turnout.android.domain.model.RsvpResult
import com.turnout.android.domain.model.RsvpValidation
import com.turnout.android.domain.repository.RsvpRepository
import javax.inject.Inject

class RsvpRepositoryImpl @Inject constructor(
    private val rsvpApi: RsvpApi
) : RsvpRepository {

    override suspend fun validateToken(token: String): Result<RsvpValidation> =
        safeApiCall {
            val dto = rsvpApi.validateToken(token)
            RsvpValidation(
                valid = dto.valid,
                guestName = dto.guestName,
                eventTitle = dto.eventTitle,
                eventDate = dto.eventDate,
                eventLocation = dto.eventLocation,
                eventDescription = dto.eventDescription,
                alreadyResponded = dto.alreadyResponded,
                previousStatus = dto.previousStatus,
                eventFull = dto.eventFull
            )
        }

    override suspend fun submitRsvp(token: String, status: String): Result<RsvpResult> =
        safeApiCall {
            val dto = rsvpApi.submitRsvp(RsvpSubmitRequest(token, status))
            RsvpResult(dto.status, dto.waitlisted)
        }
}
