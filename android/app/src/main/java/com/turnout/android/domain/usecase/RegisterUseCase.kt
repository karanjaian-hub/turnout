package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.AuthRepository
import javax.inject.Inject

data class RegisterParams(
    val fullName: String,
    val email: String,
    val username: String,
    val password: String
)

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<RegisterParams, Unit>() {
    override suspend fun invoke(params: RegisterParams): Result<Unit> {
        if (params.fullName.isBlank()) return Result.Error("Full name is required")
        if (params.email.isBlank()) return Result.Error("Email is required")
        if (params.username.isBlank()) return Result.Error("Username is required")
        if (params.password.length < 8) return Result.Error("Password must be at least 8 characters")
        return authRepository.register(params.fullName, params.email, params.username, params.password)
    }
}
