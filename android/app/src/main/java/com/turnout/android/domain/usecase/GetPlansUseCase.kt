package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.SubscriptionPlan
import com.turnout.android.domain.repository.PaymentRepository
import javax.inject.Inject

class GetPlansUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) : NoParamUseCase<List<SubscriptionPlan>>() {
    override suspend fun invoke(): Result<List<SubscriptionPlan>> = paymentRepository.getPlans()
}
