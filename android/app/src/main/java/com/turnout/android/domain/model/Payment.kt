package com.turnout.android.domain.model

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val monthlyPriceKes: Double,
    val monthlyPriceUsd: Double,
    val maxEvents: Int,
    val maxGuestsPerEvent: Int,
    val features: List<String>,
    val isRecommended: Boolean
)

data class Subscription(
    val plan: String,
    val expiresAt: String?,
    val eventLimit: Int,
    val guestLimit: Int
)

data class Transaction(
    val id: Long,
    val provider: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val createdAt: String
)
