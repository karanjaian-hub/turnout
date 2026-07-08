package com.turnout.android.presentation.payments

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import com.turnout.android.core.components.EmptyState

// TODO(Phase 9 - Payments): replace with real plan upgrade / billing UI.
@Composable
fun UpgradeScreen() {
    EmptyState(
        icon = Icons.Default.Star,
        title = "Upgrade",
        subtitle = "Plan upgrade options coming soon"
    )
}
