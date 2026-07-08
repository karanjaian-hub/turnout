package com.turnout.android.presentation.auth.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.usecase.ForgotPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ForgotPasswordEvent {
    data class ShowError(val message: String) : ForgotPasswordEvent()
}

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val emailSent: Boolean = false
)

private const val MINIMUM_STATE_DURATION_MS = 600L

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ForgotPasswordEvent>(replay = 0)
    val events = _events.asSharedFlow()

    fun sendResetLink(email: String) = viewModelScope.launch {
        _uiState.value = ForgotPasswordUiState(isLoading = true)
        val startTime = System.currentTimeMillis()

        val result = forgotPasswordUseCase(email)
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < MINIMUM_STATE_DURATION_MS) delay(MINIMUM_STATE_DURATION_MS - elapsed)

        when (result) {
            is Result.Success -> _uiState.value = ForgotPasswordUiState(emailSent = true)
            is Result.Error -> {
                _uiState.value = ForgotPasswordUiState()
                _events.emit(ForgotPasswordEvent.ShowError(result.message))
            }
        }
    }
}
