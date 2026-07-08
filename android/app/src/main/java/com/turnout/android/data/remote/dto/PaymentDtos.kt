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
