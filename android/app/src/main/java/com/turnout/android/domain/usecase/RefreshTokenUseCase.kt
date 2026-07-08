package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.AuthTokens
import com.turnout.android.domain.repository.AuthRepository
import javax.inject.Inject

class RefreshTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<String, AuthTokens>() {
    override suspend fun invoke(params: String): Result<AuthTokens> {
        if (params.isBlank()) return Result.Error("No refresh token available")
        return authRepository.refresh(params)
    }
}
