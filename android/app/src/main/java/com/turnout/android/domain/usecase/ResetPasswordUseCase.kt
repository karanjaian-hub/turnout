package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.AuthRepository
import javax.inject.Inject

data class ResetPasswordParams(val token: String, val newPassword: String)

class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<ResetPasswordParams, Unit>() {
    override suspend fun invoke(params: ResetPasswordParams): Result<Unit> {
        if (params.token.isBlank()) return Result.Error("Reset token is missing")
        if (params.newPassword.length < 8) return Result.Error("Password must be at least 8 characters")
        return authRepository.resetPassword(params.token, params.newPassword)
    }
}
