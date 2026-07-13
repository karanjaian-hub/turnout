package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.repository.EventRepository
import javax.inject.Inject

data class GetEventsParams(val status: String? = null, val page: Int = 0)

class GetEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) : UseCase<GetEventsParams, List<Event>>() {
    override suspend fun invoke(params: GetEventsParams): Result<List<Event>> =
        eventRepository.getEvents(params.status, params.page)
}
