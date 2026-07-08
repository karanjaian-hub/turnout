package com.turnout.android.presentation.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.turnout.android.core.theme.TextPrimary

// Placeholder — full implementation in Phase 14.4
@Composable
fun AiScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("AI — Phase 14.4", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
    }
}
