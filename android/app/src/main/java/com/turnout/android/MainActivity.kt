package com.turnout.android

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
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
import com.turnout.android.core.navigation.NavGraph
import com.turnout.android.core.navigation.Screen
import com.turnout.android.TurnoutApp
import com.turnout.android.presentation.AuthState
import com.turnout.android.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() must be called BEFORE super.onCreate() —
        // it needs to intercept the window before any content is drawn.
        val splashScreen = installSplashScreen()
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
                Box(
                    modifier = Modifier.fillMaxSize().imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    when (authState) {
                        // Show spinner while checking stored tokens — avoids login flash
                        is AuthState.Loading -> CircularProgressIndicator()
                        is AuthState.Authenticated -> NavGraph(
                            startDestination = Screen.Dashboard.route,
                            adaptiveConfig = adaptiveConfig
                        )
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
