package com.turnout.android.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.turnout.android.core.theme.AccentBlue
import com.turnout.android.core.theme.BorderColor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A one-shot trigger for a temporary speed-up "flash." Exposed as a Channel rather than a
 * plain callback so multiple flash requests queue up naturally instead of the newest one
 * silently overwriting an in-flight one — e.g. if two events fire close together, both
 * flashes play in sequence rather than the second one canceling the first.
 */
class PulseLineFlashController {
    private val channel = Channel<Unit>(Channel.BUFFERED)
    val flashes = channel.receiveAsFlow()
    suspend fun flash() = channel.send(Unit)
}

@Composable
fun PulseLine(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    speedMultiplier: Float = 1f,
    flashController: PulseLineFlashController? = null
) {
    var currentSpeedMultiplier by remember { mutableFloatStateOf(speedMultiplier) }

    // Listen for one-shot flash requests: bump speed way up for 600ms, then restore
    // whatever the caller's normal speedMultiplier was.
    if (flashController != null) {
        LaunchedEffect(flashController) {
            flashController.flashes.collect {
                currentSpeedMultiplier = speedMultiplier * 4f
                delay(600)
                currentSpeedMultiplier = speedMultiplier
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_line")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1400 / currentSpeedMultiplier).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_line_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        val width = size.width
        val baseLineY = size.height / 2f

        // Base line — always visible, dim, shows the track the bright segment travels along
        drawLine(
            color = BorderColor,
            start = Offset(0f, baseLineY),
            end = Offset(width, baseLineY),
            strokeWidth = size.height
        )

        if (isActive) {
            val segmentWidth = width * 0.2f
            val travel = width + segmentWidth
            val segmentStart = (progress * travel) - segmentWidth

            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        AccentBlue,
                        Color.Transparent
                    ),
                    startX = segmentStart,
                    endX = segmentStart + segmentWidth
                ),
                start = Offset(segmentStart, baseLineY),
                end = Offset(segmentStart + segmentWidth, baseLineY),
                strokeWidth = size.height
            )
        }
    }
}
