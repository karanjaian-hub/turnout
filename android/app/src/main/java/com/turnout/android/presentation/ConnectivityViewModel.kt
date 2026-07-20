package com.turnout.android.presentation

import androidx.lifecycle.ViewModel
import com.turnout.android.core.utils.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ConnectivityObserver itself is a plain @Singleton, not a ViewModel, so it can't be
// obtained via hiltViewModel() directly — this thin wrapper exists solely so TurnoutApp
// (a plain composable, not backed by its own real ViewModel) can read its isConnected
// flow using the same hiltViewModel() pattern every other screen already uses.
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver
) : ViewModel() {
    val isConnected = connectivityObserver.isConnected
}
