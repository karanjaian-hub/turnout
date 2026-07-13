package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.GuestRepository
import javax.inject.Inject

class DeleteGuestUseCase @Inject constructor(
    private val guestRepository: GuestRepository
) : UseCase<Long, Unit>() {
    override suspend fun invoke(params: Long): Result<Unit> = guestRepository.deleteGuest(params)
}
