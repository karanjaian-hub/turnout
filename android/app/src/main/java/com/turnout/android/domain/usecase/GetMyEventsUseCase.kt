package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.DashboardRepository
import com.turnout.android.domain.repository.PagedEvents
import javax.inject.Inject

data class GetMyEventsParams(val page: Int = 0, val size: Int = 20)

class GetMyEventsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : UseCase<GetMyEventsParams, PagedEvents>() {
    override suspend fun invoke(params: GetMyEventsParams): Result<PagedEvents> =
        dashboardRepository.getMyEvents(params.page, params.size)
}
