package com.turnout.android.core.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize

enum class ButtonVariant { PRIMARY, SECONDARY, OUTLINE, DANGER }

@Composable
fun TurnoutButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val windowSize = LocalAdaptiveConfig.current.windowSize

    // Spring, not tween — a linear scale-down reads as sluggish for a press animation;
    // spring gives it the slight overshoot/settle that actually feels tactile.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    // Compact phones get a slightly shorter button; Expanded (tablet) gets a taller target
    // since touch targets on a bigger screen benefit from a bit more presence.
    val height = if (windowSize == TurnoutWindowSize.Expanded) 52.dp else 48.dp

    val shape = MaterialTheme.shapes.medium
    val isEnabled = enabled && !isLoading

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(shape)
            .then(
                when (variant) {
                    ButtonVariant.PRIMARY -> Modifier.background(
                        Brush.horizontalGradient(listOf(Navy, Blue))
                    )
                    ButtonVariant.DANGER -> Modifier.background(ErrorRed)
                    ButtonVariant.SECONDARY -> Modifier.background(BlueLight)
                    ButtonVariant.OUTLINE -> Modifier.background(Color.Transparent)
                }
            )
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                )
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = if (variant == ButtonVariant.OUTLINE) Blue else Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = when (variant) {
                    ButtonVariant.PRIMARY, ButtonVariant.DANGER -> Color.White
                    ButtonVariant.SECONDARY -> Blue
                    ButtonVariant.OUTLINE   -> Blue
                }
            )
        }
    }
}
