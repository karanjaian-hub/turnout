package com.turnout.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.AuthStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class AuthState {
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
}

/**
 * Thin Compose-facing adapter over AuthStateManager — MainActivity only needs a simple
 * three-state AuthState, but AuthStateManager (the real source of truth) exposes richer
 * state (isLoggedIn + isInitializing + currentUser) that other consumers might need too.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    authStateManager: AuthStateManager
) : ViewModel() {

    val authState = combine(
        authStateManager.isInitializing,
        authStateManager.isLoggedIn
    ) { isInitializing, isLoggedIn ->
        when {
            isInitializing -> AuthState.Loading
            isLoggedIn -> AuthState.Authenticated
            else -> AuthState.Unauthenticated
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthState.Loading
    )
}
