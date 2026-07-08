package com.turnout.android.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.turnout.android.MainActivity
import com.turnout.android.R
import com.turnout.android.TurnoutApplication

/**
 * Receives push notifications from the backend's notificationservice (Kafka -> FCM bridge).
 * This is deliberately a thin class — it only turns an incoming RemoteMessage into a visible
 * system notification. Anything more complex (deep-linking to a specific event, updating local
 * cache) is out of scope until Phase 10 wires up full push-notification handling end-to-end.
 */
class TurnoutFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Turnout"
        val body = message.notification?.body ?: message.data["body"] ?: return
        val channelId = message.data["channel"] ?: TurnoutApplication.CHANNEL_EVENTS

        showNotification(title, body, channelId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO(Phase 10): send this token to authservice so the backend can target
        // this specific device. Not wired yet — token is generated but currently unused.
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        val notificationManager = getSystemService<NotificationManager>() ?: return

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            android.content.Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        // Using System.currentTimeMillis() as the notification ID means each push gets
        // its own slot in the notification tray instead of overwriting the previous one.
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
