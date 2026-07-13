package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.PaymentRepository
import javax.inject.Inject

class CreateStripeSessionUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) : UseCase<String, String>() {
    override suspend fun invoke(params: String): Result<String> = paymentRepository.initiateStripe(params)
}
