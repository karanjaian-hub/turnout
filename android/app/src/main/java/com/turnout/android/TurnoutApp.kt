package com.turnout.android

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.theme.DangerRed
import com.turnout.android.core.theme.TurnoutTheme
import com.turnout.android.core.utils.AdaptiveLayoutConfig
import com.turnout.android.core.utils.toAdaptiveConfig
import com.turnout.android.presentation.ConnectivityViewModel
import kotlinx.coroutines.delay

/**
 * Root composable — the one real place in the app that measures the Activity's actual
 * window size and feeds it into TurnoutTheme. Everything below this point (NavGraph,
 * every screen) reads window size only via LocalAdaptiveConfig, never by measuring
 * an Activity directly — keeping that concern in exactly one place.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun TurnoutApp(
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
    content: @Composable (AdaptiveLayoutConfig) -> Unit
) {
    val activity = LocalContext.current as Activity
    val windowSizeClass = calculateWindowSizeClass(activity)
    val adaptiveConfig = windowSizeClass.toAdaptiveConfig()
    val isConnected by connectivityViewModel.isConnected.collectAsStateWithLifecycle(initialValue = true)

    // Content starts invisible and fades in ~100ms after first composition — gives the
    // native splash screen's own exit animation (300ms fade, set up in MainActivity)
    // a moment to actually finish before Compose content appears underneath it, rather
    // than both animating at once and looking like a flicker.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    TurnoutTheme(windowSize = adaptiveConfig.windowSize) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(300))) {
                content(adaptiveConfig)
            }

            // Persistent banner, not a Snackbar — per spec this needs to stay visible
            // the entire time connectivity is down, not auto-dismiss after a few seconds
            // the way a Snackbar would.
            AnimatedVisibility(
                visible = !isConnected,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DangerRed)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("No internet connection", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }
            }
        }
    }
}
