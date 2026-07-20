package com.turnout.android.core.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.turnout.android.core.theme.DangerRed

/**
 * Shared across the four bottom-nav root screens (Dashboard, Events, AI, Settings) —
 * pressing back on any of them shows an exit-confirmation dialog rather than the usual
 * pop-back-stack behavior, since these are the app's "top level" destinations with
 * nowhere further back to go within the app itself.
 */
@Composable
fun ExitAppBackHandler() {
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler(enabled = true) { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Turnout?") },
            confirmButton = {
                TextButton(onClick = {
                    (context as? android.app.Activity)?.finish()
                }) { Text("Exit", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
            }
        )
    }
}
