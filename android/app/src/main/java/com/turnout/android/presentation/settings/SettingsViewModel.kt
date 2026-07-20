package com.turnout.android.presentation.settings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.AppEvent
import com.turnout.android.core.utils.AppEventBus
import com.turnout.android.core.utils.AuthStateManager
import com.turnout.android.core.utils.Result
import com.turnout.android.core.utils.TurnoutBiometricManager
import com.turnout.android.data.local.UserPreferences
import com.turnout.android.domain.model.User
import com.turnout.android.domain.usecase.GetCurrentSubscriptionUseCase
import com.turnout.android.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsEvent {
    data object NavigateToLogin : SettingsEvent()
    data class ShowError(val message: String) : SettingsEvent()
}

data class SettingsUiState(
    val currentUser: User? = null,
    val biometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val currentPlan: String = "FREE"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authStateManager: AuthStateManager,
    private val biometricManager: TurnoutBiometricManager,
    private val userPreferences: UserPreferences,
    private val getCurrentSubscriptionUseCase: GetCurrentSubscriptionUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(isBiometricAvailable = biometricManager.isBiometricAvailable())
    )
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(replay = 0)
    val events = _events.asSharedFlow()

    init {
        authStateManager.currentUser
            .onEach { user -> _uiState.value = _uiState.value.copy(currentUser = user) }
            .launchIn(viewModelScope)

        biometricManager.isBiometricEnabled()
            .combine(userPreferences.notificationsEnabled) { biometric, notifications -> biometric to notifications }
            .onEach { (biometric, notifications) ->
                _uiState.value = _uiState.value.copy(biometricEnabled = biometric, notificationsEnabled = notifications)
            }
            .launchIn(viewModelScope)

        loadSubscription()
    }

    private fun loadSubscription() = viewModelScope.launch {
        when (val result = getCurrentSubscriptionUseCase()) {
            is Result.Success -> _uiState.value = _uiState.value.copy(currentPlan = result.data.plan)
            is Result.Error -> Unit // plan badge just stays at the default "FREE" — non-fatal
        }
    }

    fun toggleBiometric(activity: FragmentActivity, enable: Boolean) {
        if (!enable) {
            // Disabling never needs confirmation — only enabling does, per spec, since
            // turning OFF a security feature isn't itself something you need to prove
            // your identity for.
            viewModelScope.launch { userPreferences.setBiometricEnabled(false) }
            return
        }

        biometricManager.authenticate(
            activity = activity,
            onSuccess = {
                viewModelScope.launch { userPreferences.setBiometricEnabled(true) }
            },
            onError = { message ->
                viewModelScope.launch { _events.emit(SettingsEvent.ShowError(message)) }
            }
        )
    }

    fun toggleNotifications(enabled: Boolean) = viewModelScope.launch {
        userPreferences.setNotificationsEnabled(enabled)
        // Actually requesting the runtime permission (if enabling on API 33+) is handled
        // by RequestNotificationPermission() at the MainActivity level, which already
        // re-runs on next authenticated composition — no separate request wired here to
        // avoid duplicating that permission-request logic in a second place.
    }

    fun logout() = viewModelScope.launch {
        when (logoutUseCase()) {
            is Result.Success, is Result.Error -> {
                // Treated the same either way — even if the server-side logout call itself
                // fails (e.g. offline), tokens are already cleared locally by LogoutUseCase,
                // so the user should still be taken back to Login rather than stuck.
                AppEventBus.emit(AppEvent.Logout)
                _events.emit(SettingsEvent.NavigateToLogin)
            }
        }
    }
}
