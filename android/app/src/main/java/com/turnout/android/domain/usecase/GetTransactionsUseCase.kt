package com.turnout.android.domain.usecase

import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.PagedTransactions
import com.turnout.android.domain.repository.PaymentRepository
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) : UseCase<Int, PagedTransactions>() {
    override suspend fun invoke(params: Int): Result<PagedTransactions> = paymentRepository.getTransactions(params)
}
