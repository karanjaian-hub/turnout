package com.turnout.android.presentation.auth.otp

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.TurnoutButton
import com.turnout.android.core.theme.*
import com.turnout.android.presentation.auth.AuthEvent
import com.turnout.android.presentation.auth.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun OtpVerificationScreen(
    email: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusedIndex = remember { mutableIntStateOf(-1) }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    var resendCountdown by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var showCheckmark by remember { mutableStateOf(false) }
    var hasAutoSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
        canResend = true
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
        viewModel.events.collectLatest { event ->
            when (event) {
                is AuthEvent.OtpVerified -> {
                    showCheckmark = true
                    // Let the checkmark animation actually play before navigating away —
                    // this delay is deliberate UI sequencing, not filler.
                    delay(900)
                    onNavigateToDashboard()
                }
                else -> Unit
            }
        }
    }

    // Auto-submit the moment all 6 digits are filled — guarded by hasAutoSubmitted so
    // it fires exactly once, not on every recomposition after the 6th digit is entered.
    LaunchedEffect(otpValues.toList()) {
        if (otpValues.all { it.isNotEmpty() } && !hasAutoSubmitted) {
            hasAutoSubmitted = true
            viewModel.verifyOtp(email, otpValues.joinToString(""))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Canvas)) {
        AnimatedContent(targetState = showCheckmark, label = "otp_success") { success ->
            if (success) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CheckmarkAnimation()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(Modifier.height(48.dp))

                    Text(
                        "Verify your email",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextOnCanvas
                    )
                    Text(
                        text = "A 6-digit code was sent to\n$email",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextOnCanvasSecondary,
                        textAlign = TextAlign.Center
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        otpValues.forEachIndexed { index, value ->
                            OtpBox(
                                value = value,
                                isFocused = focusedIndex.intValue == index,
                                focusRequester = focusRequesters[index],
                                onFocusChange = { focused -> if (focused) focusedIndex.intValue = index },
                                onValueChange = { newChar ->
                                    hasAutoSubmitted = false
                                    if (newChar.isEmpty()) {
                                        otpValues[index] = ""
                                        if (index > 0) focusRequesters[index - 1].requestFocus()
                                    } else {
                                        otpValues[index] = newChar.last().toString()
                                        if (index < 5) focusRequesters[index + 1].requestFocus()
                                    }
                                }
                            )
                        }
                    }

                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        Text(uiState.errorMessage ?: "", color = DangerRed, style = MaterialTheme.typography.bodySmall)
                    }

                    TurnoutButton(
                        text = "Verify",
                        onClick = { viewModel.verifyOtp(email, otpValues.joinToString("")) },
                        isLoading = uiState.isLoading,
                        enabled = otpValues.all { it.isNotEmpty() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (canResend) {
                        TextButton(onClick = {
                            resendCountdown = 60
                            canResend = false
                            viewModel.resendOtp(email)
                        }) {
                            Text("Resend OTP", color = AccentBlue)
                        }
                    } else {
                        Text(
                            "Resend OTP in ${resendCountdown}s",
                            color = TextOnCanvasSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpBox(
    value: String,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }

    // Three distinct states, exactly per spec: empty/unfocused, actively focused, filled.
    val borderColor = when {
        value.isNotEmpty() -> SignalGreen
        isFocused -> AccentBlue
        else -> BorderColor
    }
    val borderWidth = if (isFocused || value.isNotEmpty()) 2.dp else 1.dp

    BasicTextField(
        value = fieldValue,
        onValueChange = { new ->
            fieldValue = new.copy(selection = TextRange(new.text.length))
            onValueChange(new.text)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(AccentBlue),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = TextOnCanvas,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier
            .size(width = 48.dp, height = 56.dp)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChange(it.isFocused) },
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) { innerTextField() }
        }
    )
}

/**
 * Custom canvas-drawn success indicator — an expanding circular arc rather than a stock
 * Material icon, since the spec explicitly wants a drawn animation here, not a static asset.
 */
@Composable
private fun CheckmarkAnimation() {
    val sweep = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        sweep.animateTo(360f, animationSpec = androidx.compose.animation.core.tween(500))
    }

    Canvas(modifier = Modifier.size(80.dp)) {
        drawArc(
            color = SignalGreen,
            startAngle = -90f,
            sweepAngle = sweep.value,
            useCenter = false,
            style = Stroke(width = 6.dp.toPx()),
            topLeft = Offset.Zero,
            size = size
        )
    }
}
