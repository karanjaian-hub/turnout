package com.turnout.android.core.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Requests POST_NOTIFICATIONS at runtime on Android 13+ (API 33) — the manifest
 * declaration alone (added in Phase 1.5) isn't enough; this is a dangerous permission
 * requiring an explicit runtime prompt, same as camera/location. Below API 33,
 * notification permission is granted automatically at install time, so this is a no-op.
 */
@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    var permissionRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — either way, nothing further to do here; a denial just
           means push notifications silently won't show, no broken flow either way. */ }

    LaunchedEffect(Unit) {
        if (permissionRequested) return@LaunchedEffect
        permissionRequested = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!alreadyGranted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
