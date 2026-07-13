package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.PaymentRepository
import javax.inject.Inject

data class InitiateMpesaParams(val phoneNumber: String, val planId: String)

class InitiateMpesaUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) : UseCase<InitiateMpesaParams, String>() {
    override suspend fun invoke(params: InitiateMpesaParams): Result<String> {
        if (params.phoneNumber.isBlank()) return Result.Error("Phone number is required")
        return paymentRepository.initiateMpesa(params.phoneNumber, params.planId)
    }
}
