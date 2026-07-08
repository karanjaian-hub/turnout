package com.turnout.android.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.usecase.RegisterParams
import com.turnout.android.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RegisterEvent {
    data class NavigateToOtp(val email: String) : RegisterEvent()
    data class ShowError(val message: String) : RegisterEvent()
}

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

private const val MINIMUM_STATE_DURATION_MS = 600L

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RegisterEvent>(replay = 0)
    val events = _events.asSharedFlow()

    fun register(fullName: String, email: String, username: String, password: String) =
        viewModelScope.launch {
            _uiState.value = RegisterUiState(isLoading = true)
            val startTime = System.currentTimeMillis()

            // Routed through RegisterUseCase (not authRepository directly) so the
            // blank-field and password-length validation written in 3.2 actually runs,
            // rather than being dead code sitting unused.
            val result = registerUseCase(RegisterParams(fullName, email, username, password))
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < MINIMUM_STATE_DURATION_MS) delay(MINIMUM_STATE_DURATION_MS - elapsed)

            when (result) {
                is Result.Success -> {
                    _uiState.value = RegisterUiState()
                    _events.emit(RegisterEvent.NavigateToOtp(email))
                }
                is Result.Error -> {
                    _uiState.value = RegisterUiState(errorMessage = result.message)
                    _events.emit(RegisterEvent.ShowError(result.message))
                }
            }
        }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
