package com.turnout.android.presentation.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.TurnoutBiometricManager
import com.turnout.android.data.local.TokenManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// One-shot events the UI reacts to once then discards (navigation, toasts, haptics)
sealed class AuthEvent {
    data class NavigateToOtp(val email: String) : AuthEvent()
    data object NavigateToDashboard : AuthEvent()
    data object NavigateToLogin : AuthEvent()
    data class ShowError(val message: String) : AuthEvent()
    data class NavigateToReset(val email: String) : AuthEvent()
    // Distinct from ShowError — the screen reacts to this by triggering the success
    // haptic before actually navigating, ShowError never should.
    data object LoginSucceeded : AuthEvent()
    // Distinct from NavigateToDashboard — OTP screen shows a checkmark animation first,
    // then navigates itself once that finishes, rather than the ViewModel navigating
    // out from under an animation that's still playing.
    data object OtpVerified : AuthEvent()
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

private const val MINIMUM_STATE_DURATION_MS = 600L

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val resendOtpUseCase: ResendOtpUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val biometricManager: TurnoutBiometricManager,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    // SharedFlow with replay=0: each event is consumed once — no duplicate navigations
    private val _events = MutableSharedFlow<AuthEvent>(replay = 0)
    val events = _events.asSharedFlow()

    // Only meaningful if biometric is both enabled by the user AND there's actually a
    // stored refresh token to unlock — enabling biometric with no session to fall back
    // on would show a button that leads nowhere.
    val showBiometricButton = biometricManager.isBiometricEnabled()
        .combine(tokenManager.accessToken) { enabled, _ ->
            enabled && tokenManager.getRefreshToken() != null
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun login(username: String, password: String) = viewModelScope.launch {
        _uiState.value = AuthUiState(isLoading = true)
        val startTime = System.currentTimeMillis()

        val loginResult = loginUseCase(LoginParams(username, password))

        when (loginResult) {
            is Result.Success -> {
                // Login itself succeeded server-side, but we still need to confirm this
                // account's role before treating it as a successful admin-panel sign-in.
                // getCurrentUserUseCase failing here is treated as "proceed anyway" —
                // blocking a legitimate login over a flaky profile-fetch would be worse
                // UX than occasionally letting a role-check slip through once.
                val roleCheck = getCurrentUserUseCase()
                val isEventOrganizer = (roleCheck as? Result.Success)?.data?.role == "EVENT_ORGANIZER"

                enforceMinimumDuration(startTime)

                if (isEventOrganizer) {
                    // This role has no business in the admin panel — log the session
                    // back out rather than leaving valid tokens sitting unused locally.
                    logoutUseCase()
                    val message = "This panel is for admins. Use the Turnout mobile app to manage your events."
                    _uiState.value = AuthUiState(errorMessage = message)
                    _events.emit(AuthEvent.ShowError(message))
                } else {
                    _uiState.value = AuthUiState()
                    _events.emit(AuthEvent.LoginSucceeded)
                    _events.emit(AuthEvent.NavigateToDashboard)
                }
            }
            is Result.Error -> {
                enforceMinimumDuration(startTime)
                _uiState.value = AuthUiState(errorMessage = loginResult.message)
                _events.emit(AuthEvent.ShowError(loginResult.message))
            }
        }
    }

    fun verifyOtp(email: String, otp: String) = viewModelScope.launch {
        _uiState.value = AuthUiState(isLoading = true)
        val startTime = System.currentTimeMillis()

        when (val result = verifyOtpUseCase(OtpParams(email, otp))) {
            is Result.Success -> {
                enforceMinimumDuration(startTime)
                _uiState.value = AuthUiState()
                // OTP screen owns the checkmark-then-navigate sequencing itself.
                _events.emit(AuthEvent.OtpVerified)
            }
            is Result.Error -> {
                enforceMinimumDuration(startTime)
                _uiState.value = AuthUiState(errorMessage = result.message)
                _events.emit(AuthEvent.ShowError(result.message))
            }
        }
    }

    fun resendOtp(email: String) = viewModelScope.launch {
        when (val result = resendOtpUseCase(email)) {
            is Result.Success -> Unit // resend succeeded silently — countdown reset is UI-local
            is Result.Error -> _events.emit(AuthEvent.ShowError(result.message))
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Biometric never calls the login API — it just unlocks the refresh token already
    // stored from a previous real login, then uses that to get a fresh access token.
    fun biometricLogin(activity: FragmentActivity) {
        biometricManager.authenticate(
            activity = activity,
            onSuccess = {
                viewModelScope.launch {
                    val refreshToken = tokenManager.getRefreshToken()
                    if (refreshToken == null) {
                        _events.emit(AuthEvent.ShowError("Session expired — please sign in again"))
                        return@launch
                    }
                    when (refreshTokenUseCase(refreshToken)) {
                        is Result.Success -> _events.emit(AuthEvent.NavigateToDashboard)
                        is Result.Error -> _events.emit(AuthEvent.ShowError("Could not restore your session — please sign in again"))
                    }
                }
            },
            onError = { errorMessage ->
                viewModelScope.launch { _events.emit(AuthEvent.ShowError(errorMessage)) }
            }
        )
    }

    // Ensures at least 600ms passes between starting an operation and reacting to its
    // result — prevents a fast network response from flashing a loading spinner for
    // an imperceptible instant, which reads as janky rather than deliberate.
    private suspend fun enforceMinimumDuration(startTime: Long) {
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < MINIMUM_STATE_DURATION_MS) {
            delay(MINIMUM_STATE_DURATION_MS - elapsed)
        }
    }
}
