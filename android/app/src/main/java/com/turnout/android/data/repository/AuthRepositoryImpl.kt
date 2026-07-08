package com.turnout.android.data.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.safeApiCall
import com.turnout.android.data.local.TokenManager
import com.turnout.android.data.remote.api.AuthApi
import com.turnout.android.data.remote.dto.*
import com.turnout.android.domain.model.AuthTokens
import com.turnout.android.domain.model.User
import com.turnout.android.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<AuthTokens> {
        return safeApiCall {
            val response = authApi.login(LoginRequest(username, password))
            tokenManager.saveTokens(response.accessToken, response.refreshToken)
            AuthTokens(response.accessToken, response.refreshToken)
        }
    }

    override suspend fun register(fullName: String, email: String, username: String, password: String): Result<Unit> {
        return safeApiCall {
            authApi.register(RegisterRequest(fullName, email, username, password))
            Unit
        }
    }

    override suspend fun verifyOtp(email: String, otp: String): Result<AuthTokens> {
        return safeApiCall {
            val response = authApi.verifyOtp(OtpRequest(email, otp))
            tokenManager.saveTokens(response.accessToken, response.refreshToken)
            AuthTokens(response.accessToken, response.refreshToken)
        }
    }

    override suspend fun resendOtp(email: String): Result<Unit> =
        safeApiCall { authApi.resendOtp(EmailRequest(email)); Unit }

    override suspend fun forgotPassword(email: String): Result<Unit> =
        safeApiCall { authApi.forgotPassword(EmailRequest(email)); Unit }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> =
        safeApiCall { authApi.resetPassword(ResetPasswordRequest(token, newPassword)); Unit }

    override suspend fun logout(): Result<Unit> {
        return safeApiCall {
            authApi.logout()
            tokenManager.clearTokens()
            Unit
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return safeApiCall {
            val dto = authApi.getCurrentUser()
            User(dto.id, dto.username, dto.email, dto.fullName, dto.role, dto.tier)
        }
    }

    override suspend fun updateFcmToken(fcmToken: String): Result<Unit> =
        safeApiCall { authApi.updateFcmToken(FcmTokenRequest(fcmToken)); Unit }

    override suspend fun refresh(refreshToken: String): Result<AuthTokens> {
        return safeApiCall {
            val response = authApi.refresh("Bearer $refreshToken")
            // TokenResponse only carries an access token per the API (see 3.1's
            // TokenRefreshInterceptor, which reads the same endpoint) — refreshToken
            // is passed through unchanged since this endpoint doesn't rotate it.
            tokenManager.updateAccessToken(response.accessToken)
            AuthTokens(response.accessToken, refreshToken)
        }
    }
}
