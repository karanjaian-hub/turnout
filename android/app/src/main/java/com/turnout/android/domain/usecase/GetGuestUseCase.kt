package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Guest
import com.turnout.android.domain.repository.GuestRepository
import javax.inject.Inject

class GetGuestUseCase @Inject constructor(
    private val guestRepository: GuestRepository
) : UseCase<Long, Guest>() {
    override suspend fun invoke(params: Long): Result<Guest> = guestRepository.getGuest(params)
}
