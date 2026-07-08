package com.turnout.android.presentation.auth.reset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.*
import com.turnout.android.core.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ResetPasswordScreen(
    token: String,
    onNavigateToLogin: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ResetPasswordEvent.NavigateToLoginWithSuccess -> {
                    // Show the confirmation on THIS screen, hold briefly so it's actually
                    // readable, then navigate — a Snackbar host on the destination we're
                    // leaving wouldn't survive the navigation to show anything at all.
                    snackbarHostState.showSnackbar("Password reset successfully")
                    kotlinx.coroutines.delay(900)
                    onNavigateToLogin()
                }
                is ResetPasswordEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Canvas)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TurnoutTopBar(title = "Reset Password", onNavigateBack = onNavigateToLogin)

            Text("Enter your new password below.", style = MaterialTheme.typography.bodyLarge, color = TextOnCanvasSecondary)

            DarkTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New Password",
                leadingIcon = Icons.Default.Lock,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle password",
                            tint = TextOnCanvasSecondary
                        )
                    }
                }
            )

            PasswordStrengthBar(password = newPassword)

            DarkTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm New Password",
                leadingIcon = Icons.Default.Lock,
                isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                errorMessage = "Passwords do not match",
                visualTransformation = PasswordVisualTransformation()
            )

            TurnoutButton(
                text = "Reset Password",
                onClick = { viewModel.resetPassword(token, newPassword) },
                isLoading = uiState.isLoading,
                enabled = newPassword.isNotBlank() && newPassword == confirmPassword,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
