package com.turnout.android

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import android.os.SystemClock
import kotlinx.coroutines.delay
import com.turnout.android.core.navigation.NavGraph
import com.turnout.android.core.navigation.Screen
import com.turnout.android.core.notifications.RequestNotificationPermission
import com.turnout.android.TurnoutApp
import com.turnout.android.presentation.AuthState
import com.turnout.android.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    // FragmentActivity, not ComponentActivity — androidx.biometric.BiometricPrompt
    // specifically requires a FragmentActivity to attach its internal dialog fragment.
    // FragmentActivity extends ComponentActivity, so this is a safe superset change —
    // splash screen, Hilt, and setContent all still work identically.

    private val mainViewModel: MainViewModel by viewModels()

    // Without this, Android's SplashScreen API dismisses the splash based on its own
    // default heuristic (roughly "first frame drawn"), which with Compose can happen
    // almost instantly — often before the real auth check (silent token refresh) even
    // finishes. Holding it on this flag until AuthState actually resolves means the
    // splash stays visible for as long as the real work takes, not just a guess.
    private var keepSplashOnScreen = true

    // Captured at process start — on a fresh install, AuthStateManager's silent-refresh
    // check has no stored token to check at all, so it resolves in a few milliseconds
    // with no real network wait. Without a minimum, the splash would then dismiss almost
    // instantly on first launch, which reads as "no splash at all."
    private val launchStartTime = SystemClock.elapsedRealtime()

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() must be called BEFORE super.onCreate() —
        // it needs to intercept the window before any content is drawn.
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Fade the splash icon out over 300ms instead of a hard cut when it exits.
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            ObjectAnimator.ofFloat(splashScreenView.iconView, "alpha", 1f, 0f).apply {
                duration = 300L
                interpolator = AccelerateInterpolator()
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }
        setContent {
                        TurnoutApp { adaptiveConfig ->
                val authState by mainViewModel.authState.collectAsStateWithLifecycle()
                LaunchedEffect(authState) {
                    if (authState !is AuthState.Loading) {
                        val elapsed = SystemClock.elapsedRealtime() - launchStartTime
                        val minimumSplashMs = 800L
                        if (elapsed < minimumSplashMs) {
                            delay(minimumSplashMs - elapsed)
                        }
                        keepSplashOnScreen = false
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize().imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    when (authState) {
                        // Show spinner while checking stored tokens — avoids login flash
                        is AuthState.Loading -> CircularProgressIndicator()
                        is AuthState.Authenticated -> {
                            // Requested once per authenticated session start — the natural
                            // point where push notifications actually become meaningful
                            // (there's no reason to prompt before the user is even logged in).
                            RequestNotificationPermission()
                            NavGraph(
                                startDestination = Screen.Dashboard.route,
                                adaptiveConfig = adaptiveConfig
                            )
                        }
                        is AuthState.Unauthenticated -> NavGraph(
                            startDestination = Screen.Login.route,
                            adaptiveConfig = adaptiveConfig
                        )
                    }
                }
            }
        }
    }
}
