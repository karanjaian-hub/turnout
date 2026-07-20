package com.turnout.android.core.utils

import com.turnout.android.data.local.TokenManager
import com.turnout.android.domain.model.User
import com.turnout.android.domain.usecase.GetCurrentUserUseCase
import com.turnout.android.domain.usecase.RefreshTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for "is the user logged in, and who are they" — MainViewModel
 * (which drives MainActivity's UI) reads from this rather than maintaining its own
 * separate check, so there's exactly one place that decides auth state, not two
 * potentially disagreeing ones.
 */
@Singleton
class AuthStateManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(tokenManager.isLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    init {
        // A stored refresh token only proves the user was logged in at some point —
        // it doesn't confirm the token is still valid server-side. Attempting a silent
        // refresh on app start catches an expired/revoked token immediately instead of
        // waiting for the first API call to fail with a 401.
        attemptSilentRefresh()

        scope.launch {
            AppEventBus.events.collect { event ->
                if (event is AppEvent.Logout) {
                    _currentUser.value = null
                    _isLoggedIn.value = false
                }
            }
        }
    }

    private fun attemptSilentRefresh() {
        val refreshToken = tokenManager.getRefreshToken() ?: run {
            _isLoggedIn.value = false
            _isInitializing.value = false
            return
        }

        scope.launch {
            // 10s timeout — if the backend is slow/unreachable at app start, we don't
            // want the user stuck on an indefinite spinner; treat a timeout the same
            // as a failed refresh and fall through to the Login screen.
            val result = withTimeoutOrNull(10_000) { refreshTokenUseCase(refreshToken) }
            when (result) {
                is Result.Success -> {
                    _isLoggedIn.value = true
                    loadCurrentUser()
                }
                is Result.Error, null -> {
                    // Refresh token is invalid/expired server-side — clear local state
                    // rather than leaving the app thinking it's logged in when it isn't.
                    tokenManager.clearTokens()
                    _isLoggedIn.value = false
                }
            }
            _isInitializing.value = false
        }
    }

    private suspend fun loadCurrentUser() {
        when (val result = getCurrentUserUseCase()) {
            is Result.Success -> _currentUser.value = result.data
            is Result.Error -> { /* Non-fatal — user stays logged in, just without a
                                     populated profile until the next successful fetch. */ }
        }
    }
}
