package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.GuestRepository
import com.turnout.android.domain.repository.GuestStats
import javax.inject.Inject

class GetGuestStatsUseCase @Inject constructor(
    private val guestRepository: GuestRepository
) : UseCase<Long, GuestStats>() {
    override suspend fun invoke(params: Long): Result<GuestStats> = guestRepository.getGuestStats(params)
}
