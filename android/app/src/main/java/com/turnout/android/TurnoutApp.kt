package com.turnout.android

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.turnout.android.core.theme.TurnoutTheme
import com.turnout.android.core.utils.AdaptiveLayoutConfig
import com.turnout.android.core.utils.toAdaptiveConfig

/**
 * Root composable — the one real place in the app that measures the Activity's actual
 * window size and feeds it into TurnoutTheme. Everything below this point (NavGraph,
 * every screen) reads window size only via LocalAdaptiveConfig, never by measuring
 * an Activity directly — keeping that concern in exactly one place.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun TurnoutApp(content: @Composable (AdaptiveLayoutConfig) -> Unit) {
    val activity = LocalContext.current as Activity
    val windowSizeClass = calculateWindowSizeClass(activity)
    val adaptiveConfig = windowSizeClass.toAdaptiveConfig()

    TurnoutTheme(windowSize = adaptiveConfig.windowSize) {
        content(adaptiveConfig)
    }
}
