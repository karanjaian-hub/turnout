package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : NoParamUseCase<Unit>() {
    override suspend fun invoke(): Result<Unit> = authRepository.logout()
}
