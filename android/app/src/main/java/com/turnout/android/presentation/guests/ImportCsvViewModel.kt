package com.turnout.android.presentation.guests

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.usecase.GetSampleTemplateUseCase
import com.turnout.android.domain.usecase.ImportGuestsParams
import com.turnout.android.domain.usecase.ImportGuestsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ImportResult(val successCount: Int, val failureCount: Int)

sealed class ImportCsvEvent {
    data class ShowSnackbar(val message: String) : ImportCsvEvent()
}

data class ImportCsvUiState(
    val selectedFileUri: Uri? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val isUploading: Boolean = false,
    val importResult: ImportResult? = null,
    val error: String? = null
)

@HiltViewModel
class ImportCsvViewModel @Inject constructor(
    private val importGuestsUseCase: ImportGuestsUseCase,
    private val getSampleTemplateUseCase: GetSampleTemplateUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportCsvUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ImportCsvEvent>(replay = 0)
    val events = _events.asSharedFlow()

    fun selectFile(uri: Uri) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        var name: String? = null
        var size: Long? = null
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (it.moveToFirst()) {
                if (nameIndex >= 0) name = it.getString(nameIndex)
                if (sizeIndex >= 0) size = it.getLong(sizeIndex)
            }
        }
        _uiState.value = _uiState.value.copy(
            selectedFileUri = uri,
            fileName = name ?: "selected_file.csv",
            fileSize = size,
            importResult = null,
            error = null
        )
    }

    fun importGuests(eventId: Long) = viewModelScope.launch {
        val uri = _uiState.value.selectedFileUri ?: return@launch
        _uiState.value = _uiState.value.copy(isUploading = true, error = null)

        when (val result = importGuestsUseCase(ImportGuestsParams(eventId, uri))) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    importResult = ImportResult(
                        successCount = result.data["successCount"] ?: 0,
                        failureCount = result.data["failureCount"] ?: 0
                    )
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(isUploading = false, error = result.message)
            }
        }
    }

    fun downloadSampleTemplate() = viewModelScope.launch {
        when (val result = getSampleTemplateUseCase()) {
            is Result.Success -> {
                val saved = saveTemplateToDownloads(result.data)
                if (saved) {
                    _events.emit(ImportCsvEvent.ShowSnackbar("Template saved to Downloads"))
                } else {
                    _events.emit(ImportCsvEvent.ShowSnackbar("Couldn't save template"))
                }
            }
            is Result.Error -> _events.emit(ImportCsvEvent.ShowSnackbar(result.message))
        }
    }

    // Two genuinely different code paths, not a style choice — MediaStore's Downloads
    // collection API (the modern, scoped-storage-correct way) only exists from API 29.
    // Our minSdk is 28, so the legacy FileOutputStream path below is a real requirement,
    // not defensive over-engineering.
    private fun saveTemplateToDownloads(content: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "turnout_guest_template.csv")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) } ?: return false
            true
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "turnout_guest_template.csv")
            runCatching { file.writeText(content) }.isSuccess
        }
    }
}
