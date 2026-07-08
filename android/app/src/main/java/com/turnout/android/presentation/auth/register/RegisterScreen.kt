package com.turnout.android.presentation.auth.register

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.*
import com.turnout.android.core.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen(
    onNavigateToOtp: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(1) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is RegisterEvent.NavigateToOtp -> onNavigateToOtp(event.email)
                // Previously silently dropped — every registration failure showed
                // nothing to the user beyond the spinner stopping. Now surfaced.
                is RegisterEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TurnoutTopBar(title = "Create Account", onNavigateBack = onNavigateBack)

            StepIndicator(currentStep = step, totalSteps = 2)

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "register_step"
            ) { currentStep ->
                when (currentStep) {
                    1 -> StepOne(
                        fullName = fullName,
                        email = email,
                        username = username,
                        onFullNameChange = { fullName = it },
                        onEmailChange = { email = it },
                        onUsernameChange = { username = it },
                        onNext = { step = 2 }
                    )
                    2 -> StepTwo(
                        password = password,
                        confirmPassword = confirmPassword,
                        passwordVisible = passwordVisible,
                        isLoading = uiState.isLoading,
                        onPasswordChange = { password = it },
                        onConfirmPasswordChange = { confirmPassword = it },
                        onToggleVisibility = { passwordVisible = !passwordVisible },
                        onBack = { step = 1 },
                        onSubmit = {
                            if (password == confirmPassword) {
                                viewModel.register(fullName, email, username, password)
                            }
                        }
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(totalSteps) { index ->
            val stepNumber = index + 1
            val isActive = stepNumber == currentStep
            val isDone = stepNumber < currentStep

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> SignalGreen
                            isActive -> AccentBlue
                            else -> BorderColor
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("$stepNumber", color = if (isActive) Color.White else TextOnCanvasSecondary, fontWeight = FontWeight.Bold)
                }
            }

            if (index < totalSteps - 1) {
                HorizontalDivider(modifier = Modifier.width(32.dp), color = if (isDone) SignalGreen else BorderColor)
            }
        }
    }
}

@Composable
private fun StepOne(
    fullName: String, email: String, username: String,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DarkTextField(value = fullName, onValueChange = onFullNameChange, label = "Full Name", leadingIcon = Icons.Default.Person)
        DarkTextField(value = email, onValueChange = onEmailChange, label = "Email", leadingIcon = Icons.Default.Email)
        DarkTextField(value = username, onValueChange = onUsernameChange, label = "Username", leadingIcon = Icons.Default.AccountCircle)
        TurnoutButton(
            text = "Next",
            onClick = onNext,
            enabled = fullName.isNotBlank() && email.isNotBlank() && username.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepTwo(
    password: String, confirmPassword: String,
    passwordVisible: Boolean, isLoading: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DarkTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "Toggle password",
                        tint = TextOnCanvasSecondary
                    )
                }
            }
        )

        PasswordStrengthBar(password = password)

        DarkTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm Password",
            leadingIcon = Icons.Default.Lock,
            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
            errorMessage = "Passwords do not match",
            visualTransformation = PasswordVisualTransformation()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TurnoutButton(text = "Back", onClick = onBack, variant = ButtonVariant.OUTLINE, modifier = Modifier.weight(1f))
            TurnoutButton(
                text = "Create Account",
                onClick = onSubmit,
                isLoading = isLoading,
                enabled = password.isNotBlank() && password == confirmPassword,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

