package com.turnout.android.data.remote.dto

data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(val fullName: String, val email: String, val username: String, val password: String)
data class OtpRequest(val email: String, val otp: String)
data class EmailRequest(val email: String)
data class ResetPasswordRequest(val token: String, val newPassword: String)
data class FcmTokenRequest(val fcmToken: String)

data class AuthResponse(val accessToken: String, val refreshToken: String, val user: UserResponse)
data class TokenResponse(val accessToken: String)
data class UserResponse(val id: Long, val username: String, val email: String, val fullName: String, val role: String, val tier: String)
data class MessageResponse(val message: String)
