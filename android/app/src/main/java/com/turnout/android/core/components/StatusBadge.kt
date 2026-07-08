package com.turnout.android.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val windowSize = LocalAdaptiveConfig.current.windowSize

    val dotColor = when (status.uppercase()) {
        "CONFIRMED", "ACTIVE" -> SignalGreen
        "PENDING"             -> WarningAmber
        "DECLINED", "CANCELLED" -> DangerRed
        "WAITLISTED"          -> InfoPurple
        "MAYBE"               -> AccentBlue
        "COMPLETED"           -> Navy
        "DRAFT"               -> Color.Gray
        else                  -> Color.Gray
    }

    // labelMedium doesn't exist as a distinct style in TurnoutTypography (Material3's
    // Typography only has labelLarge/labelSmall) — using labelLarge for Expanded here,
    // one visible step up from labelSmall on Compact, matching the guide's intent even
    // though the exact "labelMedium" name isn't a real Material3 typography slot.
    val textStyle = if (windowSize == TurnoutWindowSize.Expanded) {
        MaterialTheme.typography.labelLarge
    } else {
        MaterialTheme.typography.labelSmall
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(dotColor.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = status.lowercase().replaceFirstChar { it.uppercase() },
            style = textStyle,
            color = dotColor,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
