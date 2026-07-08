package com.turnout.android.core.utils

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Wraps Modifier.clickable with a haptic tick fired before onClick runs — every tappable
 * component in the app should use this instead of plain .clickable, so haptic behavior
 * stays consistent everywhere rather than being copy-pasted (and inevitably drifting)
 * into each component individually.
 */
fun Modifier.hapticClick(
    haptic: HapticFeedback,
    type: HapticFeedbackType = HapticFeedbackType.LongPress,
    onClick: () -> Unit
): Modifier = this.clickable {
    haptic.performHapticFeedback(type)
    onClick()
}
