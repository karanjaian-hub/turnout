package com.turnout.android.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.turnout.android.core.utils.AdaptiveLayoutConfig
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize
import com.turnout.android.core.utils.toAdaptiveConfig

private val LightColors = lightColorScheme(
    primary          = Blue,
    onPrimary        = SurfaceWhite,
    primaryContainer = BlueLight,
    secondary        = Navy,
    onSecondary      = SurfaceWhite,
    background       = BackgroundLight,
    onBackground     = TextPrimary,
    surface          = SurfaceWhite,
    onSurface        = TextPrimary,
    error            = ErrorRed,
    onError          = SurfaceWhite,
    outline          = BorderColor
)

private val DarkColors = darkColorScheme(
    primary          = Blue,
    onPrimary        = SurfaceWhite,
    primaryContainer = Navy,
    secondary        = BlueDark,
    onSecondary      = SurfaceWhite,
    background       = DarkBackground,
    onBackground     = SurfaceWhite,
    surface          = DarkSurface,
    onSurface        = SurfaceWhite,
    error            = ErrorRed,
    onError          = SurfaceWhite,
    outline          = DarkBorder
)

/**
 * windowSize drives LocalAdaptiveConfig here, so any composable under TurnoutTheme can read
 * screen-size-aware values (via LocalAdaptiveConfig.current or AdaptiveText) without needing
 * its own Activity reference — only the root call site (TurnoutApp.kt) needs a real Activity
 * to compute windowSize in the first place, via rememberAdaptiveConfig().
 *
 * dynamicColor is deliberately always false and not wired to Android 12+ dynamic theming —
 * Turnout's brand colors (navy/blue) are a deliberate design choice, not something that
 * should shift based on the user's wallpaper. The parameter exists so this intent is explicit
 * in the function signature rather than silently absent.
 */
@Composable
fun TurnoutTheme(
    windowSize: TurnoutWindowSize = TurnoutWindowSize.Compact,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val adaptiveConfig: AdaptiveLayoutConfig = windowSize.toAdaptiveConfig()

    CompositionLocalProvider(LocalAdaptiveConfig provides adaptiveConfig) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography  = TurnoutTypography,
            shapes      = TurnoutShapes,
            content     = content
        )
    }
}
