package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.AuthRepository
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<String, Unit>() {
    override suspend fun invoke(params: String): Result<Unit> {
        if (params.isBlank()) return Result.Error("Email is required")
        return authRepository.forgotPassword(params)
    }
}
