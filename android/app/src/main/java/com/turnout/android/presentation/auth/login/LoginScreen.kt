package com.turnout.android.presentation.auth.login

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.*
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize
import com.turnout.android.presentation.auth.AuthEvent
import com.turnout.android.presentation.auth.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSize = LocalAdaptiveConfig.current.windowSize
    val haptic = LocalHapticFeedback.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // TODO(Phase 10 - Biometric/Settings): wire to a real biometricEnabled preference
    // once biometric enrollment exists. Always false for now — button correctly hidden
    // until that infrastructure is built, rather than shown with no way to actually use it.
    val biometricEnabled = false
    val hasRefreshToken = false

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            repeat(4) { i ->
                shakeOffset.animateTo(if (i % 2 == 0) 12f else -12f, tween(80))
            }
            shakeOffset.animateTo(0f, tween(80))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AuthEvent.LoginSucceeded ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                is AuthEvent.NavigateToDashboard -> onNavigateToDashboard()
                else -> Unit
            }
        }
    }

    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardVisible = true }

    val cardWidthModifier = if (windowSize == TurnoutWindowSize.Compact) {
        Modifier.fillMaxWidth(0.88f)
    } else {
        Modifier.widthIn(max = 400.dp)
    }
    val cardPadding = if (windowSize == TurnoutWindowSize.Expanded) 48.dp else 32.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = cardVisible,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 24 },
                animationSpec = tween(400)
            )
        ) {
            Column(
                modifier = cardWidthModifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Color(0x0AFFFFFF))
                    .border(1.dp, Color(0x14FFFFFF), MaterialTheme.shapes.extraLarge)
                    .padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PulseLine(isActive = true, speedMultiplier = 0.4f)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(AccentBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("T", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "TURNOUT",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceGroteskFontFamily),
                        color = TextOnCanvas
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Admin Sign In", style = MaterialTheme.typography.headlineMedium, color = TextOnCanvas)
                    Text(
                        "Sign in to manage your platform",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnCanvasSecondary
                    )
                }

                DarkTextField(
                    value = username,
                    onValueChange = { username = it; viewModel.clearError() },
                    label = "Username",
                    leadingIcon = Icons.Default.Person
                )

                DarkTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearError() },
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password",
                                tint = TextOnCanvasSecondary
                            )
                        }
                    }
                )

                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.offset(x = shakeOffset.value.dp)
                    )
                }

                TurnoutButton(
                    text = "Sign In",
                    onClick = { viewModel.login(username, password) },
                    isLoading = uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(onClick = onNavigateToForgotPassword) {
                    Text("Forgot password?", color = AccentBlue)
                }

                if (biometricEnabled && hasRefreshToken) {
                    TextButton(onClick = { /* TODO(Phase 10): trigger biometric prompt */ }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = TextOnCanvasSecondary)
                            Text("Sign in with Fingerprint", color = TextOnCanvasSecondary)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account?", color = TextOnCanvasSecondary, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onNavigateToRegister) {
                Text("Register", color = AccentBlue, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

