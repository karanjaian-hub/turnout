package com.turnout.android.core.utils

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Three-tier bucket for screen width, matching Google's own breakpoints.
 * Every adaptive component in this app branches on this type instead of raw dp values,
 * so a breakpoint change only needs to happen once, here.
 */
sealed class TurnoutWindowSize {
    data object Compact : TurnoutWindowSize()   // phone portrait, <600dp
    data object Medium : TurnoutWindowSize()    // large phone landscape / small tablet, 600-840dp
    data object Expanded : TurnoutWindowSize()  // tablet, open foldable, desktop, >840dp
}

fun WindowSizeClass.toTurnoutWindowSize(): TurnoutWindowSize =
    when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> TurnoutWindowSize.Compact
        WindowWidthSizeClass.Medium -> TurnoutWindowSize.Medium
        else -> TurnoutWindowSize.Expanded
    }

/**
 * Every screen-size-dependent value a composable might need, computed once and passed down
 * via CompositionLocal — individual screens read this instead of each re-deriving their own
 * breakpoint logic, which is how visual inconsistency between screens creeps in over time.
 */
data class AdaptiveLayoutConfig(
    val windowSize: TurnoutWindowSize,
    val showBottomBar: Boolean,
    val showNavRail: Boolean,
    val contentPadding: Dp,
    val cardWidth: Dp,
    val columnCount: Int,
    val showTwoPaneLayout: Boolean
)

// The actual size -> config mapping lives here, keyed purely on TurnoutWindowSize —
// so both WindowSizeClass.toAdaptiveConfig() (used at the Activity root, where a real
// WindowSizeClass is available) and Theme.kt (which only receives a TurnoutWindowSize,
// per the guide's TurnoutTheme(windowSize, darkTheme, content) signature) share one
// source of truth instead of duplicating this branch in two places.
fun TurnoutWindowSize.toAdaptiveConfig(): AdaptiveLayoutConfig =
    when (this) {
        TurnoutWindowSize.Compact -> AdaptiveLayoutConfig(
            windowSize = this,
            showBottomBar = true,
            showNavRail = false,
            contentPadding = 16.dp,
            cardWidth = Dp.Unspecified, // fills available width on compact
            columnCount = 2,
            showTwoPaneLayout = false
        )
        TurnoutWindowSize.Medium -> AdaptiveLayoutConfig(
            windowSize = this,
            showBottomBar = false,
            showNavRail = true,
            contentPadding = 24.dp,
            cardWidth = 360.dp,
            columnCount = 4,
            showTwoPaneLayout = false
        )
        TurnoutWindowSize.Expanded -> AdaptiveLayoutConfig(
            windowSize = this,
            showBottomBar = false,
            showNavRail = true,
            contentPadding = 32.dp,
            cardWidth = 400.dp,
            columnCount = 4,
            showTwoPaneLayout = true
        )
    }

fun WindowSizeClass.toAdaptiveConfig(): AdaptiveLayoutConfig =
    toTurnoutWindowSize().toAdaptiveConfig()

// Sensible compact-mode fallback for any composable previewing outside a real Activity context
// (e.g. Compose Preview), where rememberAdaptiveConfig() below has no Activity to measure.
val LocalAdaptiveConfig = compositionLocalOf {
    AdaptiveLayoutConfig(
        windowSize = TurnoutWindowSize.Compact,
        showBottomBar = true,
        showNavRail = false,
        contentPadding = 16.dp,
        cardWidth = Dp.Unspecified,
        columnCount = 2,
        showTwoPaneLayout = false
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberAdaptiveConfig(): AdaptiveLayoutConfig {
    val activity = LocalContext.current as Activity
    val windowSizeClass = calculateWindowSizeClass(activity)
    return windowSizeClass.toAdaptiveConfig()
}
