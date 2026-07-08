package com.turnout.android.presentation.payments

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.runtime.Composable
import com.turnout.android.core.components.EmptyState

// TODO(Phase 9 - Payments): replace with real M-Pesa/Stripe payment history UI.
// Placeholder only exists so the Payments route has something to render in 2.5.
@Composable
fun PaymentsScreen() {
    EmptyState(
        icon = Icons.Default.Payment,
        title = "Payments",
        subtitle = "Payment history and management coming soon"
    )
}
