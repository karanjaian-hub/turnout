package com.turnout.android.presentation.auth.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.usecase.ResetPasswordParams
import com.turnout.android.domain.usecase.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ResetPasswordEvent {
    data object NavigateToLoginWithSuccess : ResetPasswordEvent()
    data class ShowError(val message: String) : ResetPasswordEvent()
}

data class ResetPasswordUiState(val isLoading: Boolean = false)

private const val MINIMUM_STATE_DURATION_MS = 600L

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ResetPasswordEvent>(replay = 0)
    val events = _events.asSharedFlow()

    fun resetPassword(token: String, newPassword: String) = viewModelScope.launch {
        _uiState.value = ResetPasswordUiState(isLoading = true)
        val startTime = System.currentTimeMillis()

        val result = resetPasswordUseCase(ResetPasswordParams(token, newPassword))
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < MINIMUM_STATE_DURATION_MS) delay(MINIMUM_STATE_DURATION_MS - elapsed)

        when (result) {
            is Result.Success -> {
                _uiState.value = ResetPasswordUiState()
                _events.emit(ResetPasswordEvent.NavigateToLoginWithSuccess)
            }
            is Result.Error -> {
                _uiState.value = ResetPasswordUiState()
                _events.emit(ResetPasswordEvent.ShowError(result.message))
            }
        }
    }
}
