package com.turnout.android.core.utils

import com.google.gson.Gson
import com.turnout.android.BuildConfig
import com.turnout.android.data.local.TokenManager
import com.turnout.android.domain.model.AdminAlertMessage
import com.turnout.android.domain.model.EmailProgressMessage
import com.turnout.android.domain.model.RsvpUpdateMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

sealed class WsState {
    data object Connecting : WsState()
    data object Connected : WsState()
    data object Disconnected : WsState()
    data class Error(val message: String) : WsState()
}

private val RECONNECT_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L)

@Singleton
class WebSocketManager @Inject constructor(
    @Named("webSocketClient") private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private var session: StompSession? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var lastEventId: String? = null

    private val _state = MutableStateFlow<WsState>(WsState.Disconnected)
    val state = _state.asStateFlow()

    private val _rsvpUpdates = MutableSharedFlow<RsvpUpdateMessage>(extraBufferCapacity = 8)
    val rsvpUpdates = _rsvpUpdates.asSharedFlow()

    private val _emailProgress = MutableSharedFlow<EmailProgressMessage>(extraBufferCapacity = 8)
    val emailProgress = _emailProgress.asSharedFlow()

    private val _adminAlerts = MutableSharedFlow<AdminAlertMessage>(extraBufferCapacity = 8)
    val adminAlerts = _adminAlerts.asSharedFlow()

    private val _newActivityCount = MutableStateFlow(0)
    val newActivityCount = _newActivityCount.asStateFlow()

    fun connect(eventId: String? = null) {
        lastEventId = eventId
        reconnectJob?.cancel()
        _state.value = WsState.Connecting

        scope.launch {
            try {
                val client = StompClient(OkHttpWebSocketClient(okHttpClient))
                val accessToken = tokenManager.getAccessToken()

                val newSession = client.connect(
                    url = BuildConfig.WS_BASE_URL,
                    customStompConnectHeaders = mapOf("Authorization" to "Bearer $accessToken")
                )
                session = newSession
                _state.value = WsState.Connected
                reconnectAttempts = 0

                newSession.subscribeText("/topic/admin-alerts")
                    .onEach { json -> handleAdminAlert(json) }
                    .launchIn(scope)

                if (eventId != null) {
                    newSession.subscribeText("/topic/rsvp-updates/$eventId")
                        .onEach { json -> handleRsvpUpdate(json) }
                        .launchIn(scope)

                    newSession.subscribeText("/topic/email-progress/$eventId")
                        .onEach { json -> handleEmailProgress(json) }
                        .launchIn(scope)
                }
            } catch (e: Exception) {
                _state.value = WsState.Error(e.message ?: "Connection failed")
                scheduleReconnect()
            }
        }
    }

    private suspend fun handleAdminAlert(json: String) {
        val message = runCatching { gson.fromJson(json, AdminAlertMessage::class.java) }.getOrNull() ?: return
        _adminAlerts.emit(message)
        bumpActivityCount()
    }

    private suspend fun handleRsvpUpdate(json: String) {
        val message = runCatching { gson.fromJson(json, RsvpUpdateMessage::class.java) }.getOrNull() ?: return
        _rsvpUpdates.emit(message)
        bumpActivityCount()
    }

    private suspend fun handleEmailProgress(json: String) {
        // Email progress does NOT count toward the nav badge — it's a passive progress
        // stream for the currently-open event, not a "something needs your attention" alert.
        val message = runCatching { gson.fromJson(json, EmailProgressMessage::class.java) }.getOrNull() ?: return
        _emailProgress.emit(message)
    }

    private suspend fun bumpActivityCount() {
        _newActivityCount.value += 1
        AppEventBus.emit(AppEvent.NewRsvpActivity(_newActivityCount.value))
    }

    fun disconnect() {
        reconnectJob?.cancel()
        scope.launch {
            runCatching { session?.disconnect() }
            session = null
            _state.value = WsState.Disconnected
        }
    }

    fun clearActivityCount() {
        _newActivityCount.value = 0
    }

    private fun scheduleReconnect() {
        reconnectJob = scope.launch {
            if (reconnectAttempts >= RECONNECT_DELAYS_MS.size) {
                _state.value = WsState.Error("Unable to reconnect")
                return@launch
            }
            val delayMs = RECONNECT_DELAYS_MS[reconnectAttempts]
            reconnectAttempts++
            delay(delayMs)
            connect(lastEventId)
        }
    }
}
