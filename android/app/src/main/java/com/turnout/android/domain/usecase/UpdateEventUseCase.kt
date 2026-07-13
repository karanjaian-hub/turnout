package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.repository.EventRepository
import javax.inject.Inject

data class UpdateEventParams(
    val id: Long,
    val title: String,
    val description: String,
    val location: String,
    val eventDate: String,
    val capacity: Int
)

class UpdateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) : UseCase<UpdateEventParams, Event>() {
    override suspend fun invoke(params: UpdateEventParams): Result<Event> {
        if (params.title.isBlank()) return Result.Error("Event title is required")
        return eventRepository.updateEvent(
            params.id, params.title, params.description, params.location, params.eventDate, params.capacity
        )
    }
}
