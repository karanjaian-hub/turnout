package com.turnout.android.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.turnout.android.core.theme.AccentBlue
import com.turnout.android.core.theme.BorderColor
import com.turnout.android.core.theme.DangerRed
import com.turnout.android.core.theme.SignalGreen
import com.turnout.android.core.theme.TextOnCanvasSecondary
import com.turnout.android.core.theme.WarningAmber

/**
 * Shared between Register and Reset Password — both need identical strength feedback,
 * so this lives here once rather than as two screens' private copies that could drift
 * out of sync if one gets tweaked later and the other doesn't.
 */
fun calculatePasswordStrength(password: String): Int {
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score
}

@Composable
fun PasswordStrengthBar(password: String) {
    val strength = calculatePasswordStrength(password)
    // Exact spec mapping: 1=DangerRed, 2=WarningAmber, 3=AccentBlue, 4=SignalGreen
    val colors = listOf(DangerRed, WarningAmber, AccentBlue, SignalGreen)
    val labels = listOf("Weak", "Fair", "Good", "Strong")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { index ->
                val filled = index < strength
                LinearProgressIndicator(
                    progress = { if (filled) 1f else 0f },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = if (filled) colors[strength - 1] else BorderColor,
                    trackColor = BorderColor
                )
            }
        }
        if (password.isNotEmpty()) {
            Text(
                text = labels.getOrElse(strength - 1) { "" },
                style = MaterialTheme.typography.bodySmall,
                color = if (strength > 0) colors[strength - 1] else TextOnCanvasSecondary
            )
        }
    }
}
