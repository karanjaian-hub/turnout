package com.turnout.android.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.StatusBadge
import com.turnout.android.core.components.TurnoutButton
import com.turnout.android.core.components.TurnoutCard
import com.turnout.android.core.components.TurnoutTextField
import com.turnout.android.core.components.TurnoutTopBar
import com.turnout.android.core.components.ButtonVariant
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize
import com.turnout.android.core.utils.deterministicColor
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToPlanBilling: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    com.turnout.android.core.components.ExitAppBackHandler()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSize = LocalAdaptiveConfig.current.windowSize
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showEditProfileSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.NavigateToLogin -> onLogout()
                is SettingsEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = { TurnoutTopBar(title = "Settings") },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            val contentModifier = if (windowSize == TurnoutWindowSize.Compact) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.widthIn(max = 600.dp)
            }

            LazyColumn(modifier = contentModifier, contentPadding = PaddingValues(16.dp)) {
                item {
                    ProfileCard(
                        fullName = uiState.currentUser?.fullName ?: "",
                        email = uiState.currentUser?.email ?: "",
                        plan = uiState.currentPlan,
                        onEditProfile = { showEditProfileSheet = true }
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item {
                    SectionHeader("ACCOUNT")
                    SettingsItem(label = "Change Password", icon = Icons.Default.Lock, onClick = onNavigateToChangePassword)
                    SettingsItem(label = "Plan & Billing", icon = Icons.Default.CreditCard, onClick = onNavigateToPlanBilling)
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    SectionHeader("PREFERENCES")
                    SettingsSwitchItem(
                        label = "Biometric Login",
                        icon = Icons.Default.Fingerprint,
                        checked = uiState.biometricEnabled,
                        available = uiState.isBiometricAvailable,
                        onCheckedChange = { enabled ->
                            activity?.let { viewModel.toggleBiometric(it, enabled) }
                        }
                    )
                    SettingsSwitchItem(
                        label = "Push Notifications",
                        icon = Icons.Default.Notifications,
                        checked = uiState.notificationsEnabled,
                        available = true,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    SectionHeader("ABOUT")
                    SettingsItem(label = "App Version", icon = Icons.Default.Info, trailingText = "v1.0.0 (build 1)")
                    SettingsItem(label = "Privacy Policy", icon = Icons.Default.OpenInNew, onClick = {
                        openUri(context, "https://turnout.app/privacy")
                    })
                    SettingsItem(label = "Terms of Service", icon = Icons.Default.Description, onClick = {
                        openUri(context, "https://turnout.app/terms")
                    })
                }

                item {
                    Spacer(Modifier.height(32.dp))
                    TurnoutButton(
                        text = "Sign Out",
                        onClick = { showSignOutDialog = true },
                        variant = ButtonVariant.DANGER,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again to access Turnout.") },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; viewModel.logout() }) {
                    Text("Sign Out", color = DangerRed)
                }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEditProfileSheet) {
        EditProfileSheet(
            currentName = uiState.currentUser?.fullName ?: "",
            onDismiss = { showEditProfileSheet = false }
        )
    }
}

@Composable
private fun ProfileCard(fullName: String, email: String, plan: String, onEditProfile: () -> Unit) {
    TurnoutCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val avatarColor = deterministicColor(fullName.ifBlank { "?" })
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    fullName.take(1).ifBlank { "?" }.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                StatusBadge(status = plan)
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = BorderColor)
        TextButton(onClick = onEditProfile) {
            Text("Edit Profile", color = AccentBlue)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    label: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailingText: String? = null
) {
    TurnoutCard(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            when {
                trailingText != null -> Text(trailingText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                onClick != null -> Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    available: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    TurnoutCard(modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            if (available) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentBlue,
                        checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                    )
                )
            } else {
                Text("Not available", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(currentName: String, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Edit Profile", style = MaterialTheme.typography.titleLarge)
            TurnoutTextField(value = name, onValueChange = { name = it }, label = "Full name")
            // Note: no actual save wiring here — there's no UpdateProfileUseCase/endpoint
            // in this codebase yet, so this would need real backend support before a
            // "Save" button here could do anything beyond updating local UI state.
            TurnoutButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun openUri(context: android.content.Context, url: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
    runCatching { context.startActivity(intent) }
}
