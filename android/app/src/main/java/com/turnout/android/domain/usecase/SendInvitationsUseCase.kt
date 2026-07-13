package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.EventRepository
import javax.inject.Inject

class SendInvitationsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) : UseCase<Long, Unit>() {
    override suspend fun invoke(params: Long): Result<Unit> = eventRepository.sendInvitations(params)
}
