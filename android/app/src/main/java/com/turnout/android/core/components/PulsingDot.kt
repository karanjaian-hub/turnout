package com.turnout.android.core.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.turnout.android.core.theme.SuccessGreen

/** Animated green dot shown next to "Live Activity" when WebSocket is connected. */
@Composable
fun PulsingDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_dot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue  = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Box(
        modifier = modifier
            .size(10.dp)
            .scale(scale)
            .background(SuccessGreen, CircleShape)
    )
}
