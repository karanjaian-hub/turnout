package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.EventRepository
import com.turnout.android.domain.repository.EventStats
import javax.inject.Inject

class GetEventStatsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) : UseCase<Long, EventStats>() {
    override suspend fun invoke(params: Long): Result<EventStats> = eventRepository.getEventStats(params)
}
