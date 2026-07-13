package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.repository.EventRepository
import javax.inject.Inject

data class ChangeEventStatusParams(val eventId: Long, val status: String)

class ChangeEventStatusUseCase @Inject constructor(
    private val eventRepository: EventRepository
) : UseCase<ChangeEventStatusParams, Event>() {
    override suspend fun invoke(params: ChangeEventStatusParams): Result<Event> =
        eventRepository.changeStatus(params.eventId, params.status)
}
