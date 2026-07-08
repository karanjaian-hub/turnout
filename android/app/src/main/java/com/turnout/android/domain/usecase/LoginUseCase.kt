package com.turnout.android.domain.usecase

import com.turnout.android.domain.model.AuthTokens
import com.turnout.android.domain.repository.AuthRepository
import com.turnout.android.core.utils.Result
import javax.inject.Inject

data class LoginParams(val username: String, val password: String)

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<LoginParams, AuthTokens>() {

    override suspend fun invoke(params: LoginParams): Result<AuthTokens> {
        // Validate before hitting the network — fail fast with a clear message
        if (params.username.isBlank()) return Result.Error("Username is required")
        if (params.password.isBlank()) return Result.Error("Password is required")
        return authRepository.login(params.username, params.password)
    }
}
