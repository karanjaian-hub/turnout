package com.turnout.android.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.runtime.Composable
import com.turnout.android.core.components.EmptyState

// TODO(Phase 11+): replace with the real first-launch onboarding walkthrough.
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    EmptyState(
        icon = Icons.Default.WavingHand,
        title = "Welcome to Turnout",
        subtitle = "Onboarding walkthrough coming soon",
        actionLabel = "Get Started",
        onAction = onComplete
    )
}
