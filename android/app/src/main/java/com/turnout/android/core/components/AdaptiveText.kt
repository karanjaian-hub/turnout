package com.turnout.android.core.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize

/**
 * Renders text with a different TextStyle per window size — e.g. a stat card's number can be
 * genuinely larger on a tablet without the caller needing to branch on window size itself.
 * Reads the current size from LocalAdaptiveConfig rather than taking it as a parameter, so
 * callers don't need access to an Activity or thread rememberAdaptiveConfig() through manually.
 */
@Composable
fun AdaptiveText(
    text: String,
    compactStyle: TextStyle,
    mediumStyle: TextStyle,
    expandedStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val windowSize = LocalAdaptiveConfig.current.windowSize

    val style = when (windowSize) {
        TurnoutWindowSize.Compact -> compactStyle
        TurnoutWindowSize.Medium -> mediumStyle
        TurnoutWindowSize.Expanded -> expandedStyle
    }

    Text(text = text, style = style, modifier = modifier)
}
