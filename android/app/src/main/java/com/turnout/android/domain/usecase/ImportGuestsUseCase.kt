package com.turnout.android.domain.usecase

import android.net.Uri
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.repository.GuestRepository
import javax.inject.Inject

data class ImportGuestsParams(val eventId: Long, val fileUri: Uri)

class ImportGuestsUseCase @Inject constructor(
    private val guestRepository: GuestRepository
) : UseCase<ImportGuestsParams, Map<String, Int>>() {
    override suspend fun invoke(params: ImportGuestsParams): Result<Map<String, Int>> =
        guestRepository.importCsv(params.eventId, params.fileUri)
}
