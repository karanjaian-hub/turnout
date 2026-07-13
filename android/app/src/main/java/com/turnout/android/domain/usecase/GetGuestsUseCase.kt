package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.GuestRepository
import com.turnout.android.domain.repository.PagedGuests
import javax.inject.Inject

data class GetGuestsParams(
    val eventId: Long,
    val page: Int = 0,
    val search: String? = null,
    val status: String? = null
)

class GetGuestsUseCase @Inject constructor(
    private val guestRepository: GuestRepository
) : UseCase<GetGuestsParams, PagedGuests>() {
    override suspend fun invoke(params: GetGuestsParams): Result<PagedGuests> =
        guestRepository.getGuests(params.eventId, params.page, params.search, params.status)
}
