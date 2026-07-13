package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.GuestRepository
import javax.inject.Inject

class ExportGuestsUseCase @Inject constructor(
    private val guestRepository: GuestRepository
) : UseCase<Long, String>() {
    override suspend fun invoke(params: Long): Result<String> = guestRepository.exportGuests(params)
}
