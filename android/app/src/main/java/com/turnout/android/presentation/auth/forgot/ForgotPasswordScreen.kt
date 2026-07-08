package com.turnout.android.presentation.auth.forgot

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.DarkTextField
import com.turnout.android.core.components.TurnoutButton
import com.turnout.android.core.components.TurnoutTopBar
import com.turnout.android.core.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ForgotPasswordEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Canvas)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TurnoutTopBar(title = "Forgot Password", onNavigateBack = onNavigateBack)

            AnimatedContent(targetState = uiState.emailSent, label = "forgot_state") { sent ->
                if (sent) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp)
                    ) {
                        PulsingEmailIcon()
                        Text("Check your inbox", style = MaterialTheme.typography.headlineMedium, color = TextOnCanvas)
                        Text(
                            "We sent a password reset link to $email",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextOnCanvasSecondary,
                            textAlign = TextAlign.Center
                        )
                        TurnoutButton(text = "Back to Login", onClick = onNavigateBack, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Enter your email and we'll send you a reset link.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextOnCanvasSecondary
                        )
                        DarkTextField(value = email, onValueChange = { email = it }, label = "Email", leadingIcon = Icons.Default.Email)
                        TurnoutButton(
                            text = "Send Reset Link",
                            onClick = { viewModel.sendResetLink(email) },
                            isLoading = uiState.isLoading,
                            enabled = email.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PulsingEmailIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "email_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "email_scale"
    )

    Icon(
        Icons.Default.Email,
        contentDescription = null,
        tint = AccentBlue,
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
    )
}
