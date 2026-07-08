package com.turnout.android.data.remote.api

import com.turnout.android.data.remote.dto.*
import retrofit2.http.*

interface PaymentApi {

    @GET("api/payments/transactions")
    suspend fun getTransactions(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PagedResponse<TransactionDto>

    @POST("api/payments/mpesa/initiate")
    suspend fun initiateMpesa(@Body request: MpesaRequest): MpesaResponse

    @POST("api/payments/stripe/checkout")
    suspend fun initiateStripe(@Body request: StripeRequest): StripeResponse

    @GET("api/payments/subscription")
    suspend fun getCurrentSubscription(): SubscriptionDto
}
