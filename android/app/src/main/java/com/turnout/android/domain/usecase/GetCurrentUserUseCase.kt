package com.turnout.android.domain.usecase

import com.turnout.android.domain.model.User
import com.turnout.android.domain.repository.AuthRepository
import com.turnout.android.core.utils.Result
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : NoParamUseCase<User>() {

    override suspend fun invoke(): Result<User> = authRepository.getCurrentUser()
}
