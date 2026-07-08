package com.turnout.android.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks real-time internet connectivity via ConnectivityManager's NetworkCallback API,
 * rather than a one-shot check — isConnected reflects the current state continuously,
 * so UI (e.g. an offline banner) can react the moment connectivity actually changes.
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isConnected = MutableStateFlow(true)
    val isConnected = _isConnected.asStateFlow()

    // A private scope tied to this singleton's lifetime — since ConnectivityObserver lives
    // as long as the app process does, there's no natural "owner" scope to hook cleanup into
    // otherwise (it isn't a ViewModel, doesn't have onCleared()).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.value = true
        }

        override fun onLost(network: Network) {
            _isConnected.value = false
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isConnected.value = hasInternet && isValidated
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    // No natural single call site for "app is shutting down" on a @Singleton — this exists
    // so a caller with an appropriate lifecycle (e.g. Application.onTerminate, though that's
    // unreliable on real devices) CAN unregister explicitly if ever needed, rather than
    // leaking the callback forever with no way to clean it up at all.
    fun cleanup() {
        scope.launch {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}
