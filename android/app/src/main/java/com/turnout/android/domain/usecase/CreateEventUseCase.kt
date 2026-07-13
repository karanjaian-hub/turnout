package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.repository.EventRepository
import javax.inject.Inject

data class CreateEventParams(
    val title: String,
    val description: String,
    val location: String,
    val eventDate: String,
    val capacity: Int
)

class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) : UseCase<CreateEventParams, Event>() {
    override suspend fun invoke(params: CreateEventParams): Result<Event> {
        if (params.title.isBlank()) return Result.Error("Event title is required")
        if (params.location.isBlank()) return Result.Error("Location is required")
        if (params.capacity <= 0) return Result.Error("Capacity must be greater than zero")
        return eventRepository.createEvent(
            params.title, params.description, params.location, params.eventDate, params.capacity
        )
    }
}
