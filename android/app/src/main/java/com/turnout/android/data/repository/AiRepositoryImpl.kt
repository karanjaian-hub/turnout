package com.turnout.android.data.repository

import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.safeApiCall
import com.turnout.android.data.remote.api.AiApi
import com.turnout.android.data.remote.dto.AiPromptRequest
import com.turnout.android.domain.repository.AiRepository
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val aiApi: AiApi
) : AiRepository {

    // Returns the single "result" string as-is — the real AiApi/AiResponse shape only
    // has one text field, not the guide spec's three-way description/tagline/invitationCopy
    // split. If the backend later returns structured fields, this expands then, rather
    // than fabricating a fake three-field split now from a single string.
    override suspend fun generateEventDescription(notes: String): Result<String> =
        safeApiCall { aiApi.generateDescription(AiPromptRequest(notes)).result }
}
