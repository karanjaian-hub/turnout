package com.turnout.android.domain.repository

import com.turnout.android.domain.model.AuthTokens
import com.turnout.android.domain.model.User
import com.turnout.android.core.utils.Result

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<AuthTokens>
    suspend fun register(fullName: String, email: String, username: String, password: String): Result<Unit>
    suspend fun verifyOtp(email: String, otp: String): Result<AuthTokens>
    suspend fun resendOtp(email: String): Result<Unit>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun resetPassword(token: String, newPassword: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Result<User>
    suspend fun updateFcmToken(fcmToken: String): Result<Unit>
    suspend fun refresh(refreshToken: String): Result<AuthTokens>
}
