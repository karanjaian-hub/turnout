package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.RsvpActivity
import com.turnout.android.domain.repository.DashboardRepository
import javax.inject.Inject

class GetRecentRsvpsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : NoParamUseCase<List<RsvpActivity>>() {
    override suspend fun invoke(): Result<List<RsvpActivity>> = dashboardRepository.getRecentRsvps()
}
