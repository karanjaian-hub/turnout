package com.turnout.android.domain.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Subscription
import com.turnout.android.domain.model.SubscriptionPlan
import com.turnout.android.domain.model.Transaction

data class PagedTransactions(
    val transactions: List<Transaction>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)

interface PaymentRepository {
    suspend fun getPlans(): Result<List<SubscriptionPlan>>
    suspend fun getCurrentSubscription(): Result<Subscription>
    suspend fun getTransactions(page: Int = 0): Result<PagedTransactions>
    suspend fun initiateMpesa(phoneNumber: String, planId: String): Result<String>
    suspend fun initiateStripe(planId: String): Result<String>
    suspend fun requestEnterprise(companyName: String, contactEmail: String, notes: String): Result<String>
}
