package com.turnout.android.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.turnout.android.MainActivity
import com.turnout.android.R
import com.turnout.android.TurnoutApplication
import com.turnout.android.core.navigation.Screen
import com.turnout.android.data.local.UserPreferences
import com.turnout.android.domain.usecase.SaveFcmTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives push notifications from the backend's notificationservice (Kafka -> FCM bridge).
 * @AndroidEntryPoint + field injection, not constructor injection — FirebaseMessagingService
 * is instantiated by the Android framework itself, so constructor injection (how every other
 * Hilt class in this app works) isn't available here.
 */
@AndroidEntryPoint
class TurnoutFirebaseService : FirebaseMessagingService() {

    @Inject lateinit var saveFcmTokenUseCase: SaveFcmTokenUseCase
    @Inject lateinit var userPreferences: UserPreferences

    // FirebaseMessagingService isn't a LifecycleOwner, so there's no built-in coroutine
    // scope tied to it the way a ViewModel or Activity has — this mirrors the same pattern
    // used in ConnectivityObserver/WebSocketManager for framework classes without one.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"] ?: "GENERIC"
        val title = message.notification?.title ?: message.data["title"] ?: "Turnout"
        val body = message.notification?.body ?: message.data["body"] ?: return

        val channelId = when (type) {
            "NEW_RSVP" -> TurnoutApplication.CHANNEL_RSVP
            "PAYMENT_SUCCESS", "ENTERPRISE_APPROVED" -> TurnoutApplication.CHANNEL_PAYMENTS
            else -> TurnoutApplication.CHANNEL_EVENTS // EVENT_FULL, EMAIL_COMPLETE, GENERIC
        }

        val deepLinkUri = buildDeepLinkUri(type, message.data)
        showNotification(title, body, channelId, deepLinkUri, type, message.data)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            userPreferences.saveFcmToken(token)
            // Best-effort — if this fails (e.g. offline at the moment the token rotates),
            // there's no retry queue here. Acceptable for now: the next successful app
            // launch's token refresh (or a future one) will sync it, not a silent data-loss risk.
            saveFcmTokenUseCase(token)
        }
    }

    private fun buildDeepLinkUri(type: String, data: Map<String, String>): Uri? {
        val eventId = data["eventId"]?.toLongOrNull()
        return when (type) {
            "NEW_RSVP", "EVENT_FULL", "EMAIL_COMPLETE" ->
                eventId?.let { Uri.parse(Screen.EventDetail.deepLinkUri(it)) }
            "PAYMENT_SUCCESS", "ENTERPRISE_APPROVED" ->
                Uri.parse(Screen.Payments.deepLinkUri)
            else -> null
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        deepLinkUri: Uri?,
        type: String,
        data: Map<String, String>
    ) {
        val notificationManager = getSystemService<NotificationManager>() ?: return

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (deepLinkUri != null) data = deepLinkUri
        }

        val notificationId = System.currentTimeMillis().toInt()
        val contentPendingIntent = PendingIntent.getActivity(
            this, notificationId, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        // Only NEW_RSVP gets an inline action button — the others don't have an obviously
        // useful one-tap follow-up action, so adding buttons there would just be UI noise.
        if (type == "NEW_RSVP" && deepLinkUri != null) {
            val viewGuestIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                this.data = deepLinkUri
            }
            val viewGuestPendingIntent = PendingIntent.getActivity(
                this, notificationId + 1, viewGuestIntent, PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "View", viewGuestPendingIntent)
        }

        notificationManager.notify(notificationId, builder.build())
    }
}
