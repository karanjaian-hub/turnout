package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Subscription
import com.turnout.android.domain.repository.PaymentRepository
import javax.inject.Inject

class GetCurrentSubscriptionUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) : NoParamUseCase<Subscription>() {
    override suspend fun invoke(): Result<Subscription> = paymentRepository.getCurrentSubscription()
}
