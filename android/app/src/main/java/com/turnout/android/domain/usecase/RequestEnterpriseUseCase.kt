package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.PaymentRepository
import javax.inject.Inject

data class RequestEnterpriseParams(val companyName: String, val contactEmail: String, val notes: String)

class RequestEnterpriseUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) : UseCase<RequestEnterpriseParams, String>() {
    override suspend fun invoke(params: RequestEnterpriseParams): Result<String> {
        if (params.companyName.isBlank()) return Result.Error("Company name is required")
        if (params.contactEmail.isBlank()) return Result.Error("Contact email is required")
        return paymentRepository.requestEnterprise(params.companyName, params.contactEmail, params.notes)
    }
}
