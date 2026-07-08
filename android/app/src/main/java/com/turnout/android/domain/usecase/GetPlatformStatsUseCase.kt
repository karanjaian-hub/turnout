package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.DashboardRepository
import com.turnout.android.domain.repository.PlatformStats
import javax.inject.Inject

class GetPlatformStatsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : NoParamUseCase<PlatformStats>() {
    override suspend fun invoke(): Result<PlatformStats> = dashboardRepository.getPlatformStats()
}
