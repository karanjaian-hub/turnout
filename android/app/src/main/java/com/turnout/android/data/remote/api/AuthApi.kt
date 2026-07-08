package com.turnout.android.data.remote.api

import com.turnout.android.data.remote.dto.*
import retrofit2.http.*

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): MessageResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: OtpRequest): AuthResponse

    @POST("api/auth/resend-otp")
    suspend fun resendOtp(@Body request: EmailRequest): MessageResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Header("Authorization") refreshToken: String): TokenResponse

    @POST("api/auth/logout")
    suspend fun logout(): MessageResponse

    @GET("api/auth/me")
    suspend fun getCurrentUser(): UserResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: EmailRequest): MessageResponse

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): MessageResponse

    @POST("api/auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): MessageResponse
}
