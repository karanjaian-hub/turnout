package com.turnout.android.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.turnout.android.core.theme.TextPrimary

// Placeholder — full implementation in Phase 14.6
@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Settings — Phase 14.6", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            TextButton(onClick = onLogout) { Text("Logout") }
        }
    }
}
