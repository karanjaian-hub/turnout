package com.turnout.android.data.remote.dto

data class TransactionDto(
    val id: Long,
    val provider: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val createdAt: String
)

data class MpesaRequest(val phoneNumber: String, val planId: String)
data class MpesaResponse(val checkoutRequestId: String, val message: String)
data class StripeRequest(val planId: String)
data class StripeResponse(val checkoutUrl: String)
data class SubscriptionDto(val plan: String, val expiresAt: String?, val eventLimit: Int, val guestLimit: Int)

// Neither of these two endpoints existed anywhere in the codebase before now — no
// prior working shape to match, so this follows the same field-naming convention
// already established across the other real DTOs in this file.
data class SubscriptionPlanDto(
    val id: String,
    val name: String,
    val monthlyPriceKes: Double,
    val monthlyPriceUsd: Double,
    val maxEvents: Int,
    val maxGuestsPerEvent: Int,
    val features: List<String>,
    val isRecommended: Boolean
)

data class EnterpriseRequestDto(val companyName: String, val contactEmail: String, val notes: String)
data class EnterpriseResponseDto(val message: String)
