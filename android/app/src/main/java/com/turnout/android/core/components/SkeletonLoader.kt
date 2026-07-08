package com.turnout.android.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.turnout.android.core.theme.DarkSurface
import com.turnout.android.core.theme.BorderColor

/**
 * Placeholder block shown while real content loads — a moving light band sweeps across
 * a flat base color, standard "shimmer" pattern. Colors swap between light/dark theme
 * so the shimmer stays visible (a light shimmer on a light base would be invisible, and
 * vice versa in dark mode).
 */
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) DarkSurface else BorderColor
    val highlightColor = if (isDark) BorderColor else androidx.compose.ui.graphics.Color.White

    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(baseColor, highlightColor, baseColor),
                    start = Offset(shimmerProgress * 300f, 0f),
                    end = Offset(shimmerProgress * 300f + 300f, 300f)
                )
            )
    )
}
