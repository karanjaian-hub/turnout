package com.turnout.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

// @HiltAndroidApp triggers Hilt's compile-time code generation.
// This annotation is required — without it nothing gets injected anywhere.
@HiltAndroidApp
class TurnoutApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannels()
    }

    // Notification channels must exist BEFORE any notification is shown, and
    // Application.onCreate() runs before any Activity or Service — so this is
    // the only safe place to guarantee they're ready in time.
    private fun createNotificationChannels() {
        // Channels are an Android 8+ (API 26) concept — this app's minSdk is 28,
        // so the version check below is technically redundant, but kept as a
        // defensive habit in case minSdk ever drops in the future.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = getSystemService(NotificationManager::class.java)

        val rsvpChannel = NotificationChannel(
            CHANNEL_RSVP,
            "RSVP Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Guest RSVP confirmations and changes"
        }

        val eventsChannel = NotificationChannel(
            CHANNEL_EVENTS,
            "Event Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General event announcements and reminders"
        }

        val paymentsChannel = NotificationChannel(
            CHANNEL_PAYMENTS,
            "Payment Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "M-Pesa and Stripe payment confirmations"
        }

        notificationManager.createNotificationChannels(
            listOf(rsvpChannel, eventsChannel, paymentsChannel)
        )
    }

    companion object {
        // Referenced here AND in TurnoutFirebaseService when it's built next —
        // constants live on the Application class since this is where channels
        // are first created, avoiding a magic-string mismatch between the two files.
        const val CHANNEL_RSVP = "turnout_rsvp"
        const val CHANNEL_EVENTS = "turnout_events"
        const val CHANNEL_PAYMENTS = "turnout_payments"
    }
}
