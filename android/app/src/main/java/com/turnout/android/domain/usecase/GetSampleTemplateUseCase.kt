package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.GuestRepository
import javax.inject.Inject

class GetSampleTemplateUseCase @Inject constructor(
    private val guestRepository: GuestRepository
) : NoParamUseCase<String>() {
    override suspend fun invoke(): Result<String> = guestRepository.getSampleTemplate()
}
