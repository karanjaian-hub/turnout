package com.turnout.android.core.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.turnout.android.core.theme.JetBrainsMonoFontFamily
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize

// Compose doesn't ship a named "easeOutCubic" constant — this is the standard cubic-bezier
// curve most design systems mean by that name (fast start, gentle settle at the end).
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

/**
 * Animates between old and new integer values by actually counting through intermediate
 * numbers, not just cross-fading text — appropriate for stat cards (guest counts, RSVP
 * totals) where seeing the number tick up/down reinforces that something changed.
 */
@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier
) {
    val windowSize = LocalAdaptiveConfig.current.windowSize

    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 600, easing = EaseOutCubic),
        label = "animated_counter"
    )

    // displayMedium doesn't exist as a style in TurnoutTypography (only displayLarge is
    // defined) — using displayLarge for Expanded here, one clear step up from titleLarge,
    // rather than adding an unused style just to match a name that isn't otherwise needed.
    val textStyle = if (windowSize == TurnoutWindowSize.Expanded) {
        MaterialTheme.typography.displayLarge
    } else {
        MaterialTheme.typography.titleLarge
    }

    Text(
        text = animatedValue.toString(),
        style = textStyle.copy(fontFamily = JetBrainsMonoFontFamily),
        modifier = modifier
    )
}
