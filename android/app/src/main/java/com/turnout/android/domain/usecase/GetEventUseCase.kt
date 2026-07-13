package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.repository.EventRepository
import javax.inject.Inject

class GetEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) : UseCase<Long, Event>() {
    override suspend fun invoke(params: Long): Result<Event> = eventRepository.getEventById(params)
}
