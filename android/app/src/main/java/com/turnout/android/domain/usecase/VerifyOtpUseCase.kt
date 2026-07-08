package com.turnout.android.domain.usecase

import com.turnout.android.domain.model.AuthTokens
import com.turnout.android.domain.repository.AuthRepository
import com.turnout.android.core.utils.Result
import javax.inject.Inject

data class OtpParams(val email: String, val otp: String)

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<OtpParams, AuthTokens>() {

    override suspend fun invoke(params: OtpParams): Result<AuthTokens> {
        if (params.otp.length != 6) return Result.Error("OTP must be 6 digits")
        return authRepository.verifyOtp(params.email, params.otp)
    }
}
