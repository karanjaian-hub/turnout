package com.turnout.android.core.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

/**
 * Reveals text character by character rather than all at once — used for AI-generated
 * content, where the reveal itself communicates "this was just generated," not just a
 * decorative flourish. A blinking cursor follows the reveal, then continues blinking
 * briefly after completion before disappearing, rather than vanishing abruptly.
 */
@Composable
fun TypewriterText(
    fullText: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    speedMs: Long = 15L
) {
    var visibleChars by remember(fullText) { mutableStateOf(0) }
    var revealComplete by remember(fullText) { mutableStateOf(false) }

    LaunchedEffect(fullText) {
        visibleChars = 0
        revealComplete = false
        fullText.indices.forEach { index ->
            delay(speedMs)
            visibleChars = index + 1
        }
        revealComplete = true
        // Let the cursor blink a bit after the text finishes, then stop showing it —
        // an indefinitely-blinking cursor on static, already-read text reads as a bug.
        delay(1200)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "typewriter_cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
        label = "cursor_alpha"
    )

    val showCursor = visibleChars < fullText.length || !revealComplete

    Row(modifier = modifier) {
        Text(text = fullText.take(visibleChars), style = style)
        if (showCursor) {
            Text(text = "▌", style = style, modifier = Modifier.graphicsLayer { alpha = cursorAlpha })
        }
    }
}
