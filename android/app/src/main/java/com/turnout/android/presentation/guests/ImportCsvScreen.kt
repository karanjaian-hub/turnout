package com.turnout.android.presentation.guests

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.turnout.android.core.theme.TextPrimary

// Placeholder — full implementation in Phase 14.3
@Composable
fun ImportCsvScreen(eventId: Long, onNavigateBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Import CSV — Phase 14.3", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
    }
}
