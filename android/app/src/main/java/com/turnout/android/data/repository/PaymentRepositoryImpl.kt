package com.turnout.android.data.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.safeApiCall
import com.turnout.android.data.remote.api.PaymentApi
import com.turnout.android.data.remote.dto.EnterpriseRequestDto
import com.turnout.android.data.remote.dto.MpesaRequest
import com.turnout.android.data.remote.dto.StripeRequest
import com.turnout.android.domain.model.Subscription
import com.turnout.android.domain.model.SubscriptionPlan
import com.turnout.android.domain.model.Transaction
import com.turnout.android.domain.repository.PagedTransactions
import com.turnout.android.domain.repository.PaymentRepository
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val paymentApi: PaymentApi
) : PaymentRepository {

    override suspend fun getPlans(): Result<List<SubscriptionPlan>> =
        safeApiCall {
            paymentApi.getPlans().map { dto ->
                SubscriptionPlan(
                    id = dto.id,
                    name = dto.name,
                    monthlyPriceKes = dto.monthlyPriceKes,
                    monthlyPriceUsd = dto.monthlyPriceUsd,
                    maxEvents = dto.maxEvents,
                    maxGuestsPerEvent = dto.maxGuestsPerEvent,
                    features = dto.features,
                    isRecommended = dto.isRecommended
                )
            }
        }

    override suspend fun getCurrentSubscription(): Result<Subscription> =
        safeApiCall {
            val dto = paymentApi.getCurrentSubscription()
            Subscription(dto.plan, dto.expiresAt, dto.eventLimit, dto.guestLimit)
        }

    override suspend fun getTransactions(page: Int): Result<PagedTransactions> =
        safeApiCall {
            val response = paymentApi.getTransactions(page = page)
            PagedTransactions(
                transactions = response.content.map { dto ->
                    Transaction(dto.id, dto.provider, dto.amount, dto.currency, dto.status, dto.createdAt)
                },
                totalElements = response.totalElements,
                totalPages = response.totalPages,
                currentPage = response.number
            )
        }

    override suspend fun initiateMpesa(phoneNumber: String, planId: String): Result<String> =
        safeApiCall { paymentApi.initiateMpesa(MpesaRequest(phoneNumber, planId)).checkoutRequestId }

    override suspend fun initiateStripe(planId: String): Result<String> =
        safeApiCall { paymentApi.initiateStripe(StripeRequest(planId)).checkoutUrl }

    override suspend fun requestEnterprise(companyName: String, contactEmail: String, notes: String): Result<String> =
        safeApiCall { paymentApi.requestEnterprise(EnterpriseRequestDto(companyName, contactEmail, notes)).message }
}
