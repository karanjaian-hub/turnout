package com.turnout.android.core.navigation

/**
 * Single source of truth for every route in the app.
 * Using a sealed class means the compiler catches typos —
 * you can't accidentally navigate to "Dashbord" vs "Dashboard".
 */
sealed class Screen(val route: String) {

    // ── Auth stack ────────────────────────────────────────────────────────────
    data object Login          : Screen("login")
    data object Register       : Screen("register")
    data object OtpVerification: Screen("otp/{email}") {
        fun createRoute(email: String) = "otp/$email"
    }
    data object ForgotPassword : Screen("forgot_password")
    data object ResetPassword  : Screen("reset_password/{token}") {
        fun createRoute(token: String) = "reset_password/$token"
    }

    // ── Main (bottom nav) stack ───────────────────────────────────────────────
    data object Dashboard      : Screen("dashboard")
    data object Events         : Screen("events")
    data object Ai             : Screen("ai")
    data object Settings       : Screen("settings")

    // ── Event sub-stack ───────────────────────────────────────────────────────
    data object CreateEvent    : Screen("events/create")
    data object EventDetail    : Screen("events/{eventId}") {
        fun createRoute(eventId: Long) = "events/$eventId"
    }
    data object ImportCsv      : Screen("events/{eventId}/import") {
        fun createRoute(eventId: Long) = "events/$eventId/import"
    }
    data object GuestList      : Screen("events/{eventId}/guests") {
        fun createRoute(eventId: Long) = "events/$eventId/guests"
    }

    data object Payments       : Screen("payments")
    data object Upgrade        : Screen("upgrade")
    data object Onboarding     : Screen("onboarding")

    // ── Guest RSVP deep link ──────────────────────────────────────────────────
    data object Rsvp           : Screen("rsvp?token={token}") {
        fun createRoute(token: String) = "rsvp?token=$token"
        // Was hardcoded to "app.turnout.app" — silently different from the manifest's
        // ${rsvpHost} placeholder (wired in 1.4 from local.properties). Reading
        // BuildConfig.RSVP_BASE_URL here keeps both in sync with one source of truth.
        val deepLinkUri: String
            get() = com.turnout.android.BuildConfig.RSVP_BASE_URL.trimEnd('/') + "/rsvp"
    }
}
