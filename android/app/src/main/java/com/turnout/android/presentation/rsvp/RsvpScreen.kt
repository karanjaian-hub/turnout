package com.turnout.android.presentation.rsvp

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.PulseLine
import com.turnout.android.core.components.StatusBadge
import com.turnout.android.core.components.TurnoutButton
import com.turnout.android.core.components.TurnoutCard
import com.turnout.android.core.components.ButtonVariant
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun RsvpScreen(
    token: String,
    viewModel: RsvpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus
    val showCalendarSheet by viewModel.showCalendarSheet
    val windowSize = LocalAdaptiveConfig.current.windowSize
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(token) { viewModel.initialize(token) }

    Box(modifier = Modifier.fillMaxSize().background(Canvas)) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { androidx.compose.animation.fadeIn(tween(400)) togetherWith androidx.compose.animation.fadeOut(tween(400)) },
            label = "rsvp_state"
        ) { state ->
            when (state) {
                is RsvpUiState.Loading -> LoadingState()
                is RsvpUiState.Valid -> ValidState(
                    state = state,
                    windowSize = windowSize,
                    selectedStatus = selectedStatus,
                    onSelect = { viewModel.selectStatus(it) },
                    onSubmit = { viewModel.submitRsvp() }
                )
                is RsvpUiState.Submitting -> ValidState(
                    state = state.validState,
                    windowSize = windowSize,
                    selectedStatus = selectedStatus,
                    onSelect = {},
                    onSubmit = {},
                    submitting = true
                )
                is RsvpUiState.Success -> SuccessState(
                    state = state,
                    windowSize = windowSize,
                    showCalendarSheet = showCalendarSheet,
                    onShowCalendarSheet = { viewModel.showCalendarSheet.value = true },
                    onDismissCalendarSheet = { viewModel.showCalendarSheet.value = false },
                    snackbarHostState = snackbarHostState
                )
                is RsvpUiState.AlreadyResponded -> AlreadyRespondedState(
                    state = state,
                    onChangeResponse = { viewModel.changeResponse() }
                )
                is RsvpUiState.InvalidToken -> InvalidTokenState(reason = state.reason)
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "TURNOUT",
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = SpaceGroteskFontFamily),
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            PulseLine(isActive = true, speedMultiplier = 0.5f, modifier = Modifier.width(160.dp))
        }
    }
}

@Composable
private fun ValidState(
    state: RsvpUiState.Valid,
    windowSize: TurnoutWindowSize,
    selectedStatus: String?,
    onSelect: (String) -> Unit,
    onSubmit: () -> Unit,
    submitting: Boolean = false
) {
    var confettiActive by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(3000)
        confettiActive = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(NavyPrimary, AccentBlue)))
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(
                            "Hi ${state.guestName}!",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SpaceGroteskFontFamily),
                            color = Color.White
                        )
                        Text(state.eventTitle, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(state.eventDate, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(state.eventLocation, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }
                }
            }

            item {
                val contentWidthModifier = if (windowSize == TurnoutWindowSize.Compact) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(max = 500.dp)
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(modifier = contentWidthModifier.padding(24.dp)) {
                        TurnoutCard(modifier = Modifier.fillMaxWidth()) {
                            Text("How will you respond?", style = MaterialTheme.typography.labelLarge, color = TextOnCanvasSecondary)
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectionTile(
                                    label = "I'll be there",
                                    icon = Icons.Default.Check,
                                    tileColor = SignalGreen,
                                    isSelected = selectedStatus == "CONFIRMED",
                                    enabled = !submitting,
                                    onSelect = { onSelect("CONFIRMED") }
                                )
                                SelectionTile(
                                    label = "Maybe",
                                    icon = Icons.Default.HelpOutline,
                                    tileColor = WarningAmber,
                                    isSelected = selectedStatus == "MAYBE",
                                    enabled = !submitting,
                                    onSelect = { onSelect("MAYBE") }
                                )
                                SelectionTile(
                                    label = "Can't make it",
                                    icon = Icons.Default.Close,
                                    tileColor = DangerRed,
                                    isSelected = selectedStatus == "DECLINED",
                                    enabled = !submitting,
                                    onSelect = { onSelect("DECLINED") }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (submitting) {
                            PulseLine(isActive = true)
                        } else {
                            TurnoutButton(
                                text = "Submit RSVP",
                                onClick = onSubmit,
                                enabled = selectedStatus != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        if (confettiActive) {
            ConfettiOverlay(particleCount = 60, durationMs = 3000)
        }
    }
}

@Composable
private fun SelectionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tileColor: Color,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(1.02f, spring(stiffness = Spring.StiffnessMedium))
            scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayerScale(scale.value)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) tileColor else BorderColor,
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) tileColor.copy(alpha = 0.05f) else Color.Transparent)
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSelect()
            }
            .alpha(if (enabled) 1f else 0.6f)
            .padding(horizontal = 16.dp),
        color = Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(tileColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tileColor)
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = TextOnCanvas, modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tileColor)
            }
        }
    }
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

private fun Modifier.alpha(alpha: Float): Modifier = this.then(
    Modifier.graphicsLayer(alpha = alpha)
)

// ---- Confetti ----

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radius: Float
)

@Composable
private fun ConfettiOverlay(particleCount: Int, durationMs: Int, wideSpread: Boolean = false) {
    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            val colors = listOf(SignalGreen, AccentBlue, WarningAmber, InfoPurple, DangerRed)
            repeat(particleCount) {
                add(
                    ConfettiParticle(
                        x = Random.nextFloat(),
                        y = -0.1f - Random.nextFloat() * 0.3f,
                        vx = if (wideSpread) (Random.nextFloat() - 0.5f) * 0.016f else (Random.nextFloat() - 0.5f) * 0.006f,
                        vy = 0.003f + Random.nextFloat() * 0.004f,
                        color = colors.random(),
                        radius = 4f + Random.nextFloat() * 4f
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val endTime = System.currentTimeMillis() + durationMs
        while (System.currentTimeMillis() < endTime) {
            particles.indices.forEach { i ->
                val p = particles[i]
                particles[i] = p.copy(x = p.x + p.vx, y = p.y + p.vy)
            }
            delay(16)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            drawCircle(
                color = particle.color,
                radius = particle.radius,
                center = Offset(particle.x * size.width, particle.y * size.height)
            )
        }
    }
}

private fun ConfettiParticle.copy(x: Float = this.x, y: Float = this.y) =
    ConfettiParticle(x, y, vx, vy, color, radius)

// ---- Success ----

@Composable
private fun SuccessState(
    state: RsvpUiState.Success,
    windowSize: TurnoutWindowSize,
    showCalendarSheet: Boolean,
    onShowCalendarSheet: () -> Unit,
    onDismissCalendarSheet: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.status) {
            "CONFIRMED" -> {
                ConfettiOverlay(particleCount = 150, durationMs = 5000, wideSpread = true)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedCheckmark(size = if (windowSize == TurnoutWindowSize.Expanded) 140.dp else 100.dp)
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "See you there, ${state.guestName}!",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceGroteskFontFamily),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(state.eventTitle, style = MaterialTheme.typography.bodyMedium, color = TextOnCanvasSecondary)
                        Text(state.eventDate, style = MaterialTheme.typography.bodyMedium, color = TextOnCanvasSecondary)
                        Spacer(Modifier.height(24.dp))
                        TurnoutButton(text = "Add to Calendar", onClick = onShowCalendarSheet, variant = ButtonVariant.SECONDARY)
                    }
                }
            }
            "WAITLISTED" -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val rotation by rememberInfiniteRotation()
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(80.dp).rotate(rotation)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text("You are on the waitlist!", style = MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceGroteskFontFamily), color = Color.White)
                    Text("We will notify you if a spot opens", style = MaterialTheme.typography.bodyMedium, color = TextOnCanvasSecondary)
                }
            }
            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { // DECLINED
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ThumbDown, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(24.dp))
                    Text("Thanks for letting us know.", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
        }
    }

    if (showCalendarSheet) {
        CalendarBottomSheet(
            eventTitle = state.eventTitle,
            eventDate = state.eventDate,
            eventLocation = state.eventLocation,
            onDismiss = onDismissCalendarSheet,
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
private fun rememberInfiniteRotation(): State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "hourglass_rotation")
    return infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "rotation"
    )
}

@Composable
private fun AnimatedCheckmark(size: androidx.compose.ui.unit.Dp) {
    val progress = remember { Animatable(0f) }
    val circleScale = remember { Animatable(0.3f) }

    LaunchedEffect(Unit) {
        circleScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        progress.animateTo(1f, tween(600))
    }

    Canvas(modifier = Modifier.size(size).graphicsLayerScale(circleScale.value)) {
        val strokeWidth = 6.dp.toPx()
        drawCircle(color = SignalGreen, style = Stroke(width = strokeWidth))

        // Checkmark drawn as two connected line segments, revealed progressively by
        // only drawing up to `progress` fraction of the combined path length.
        val w = this.size.width
        val h = this.size.height
        val p1 = Offset(w * 0.28f, h * 0.52f)
        val p2 = Offset(w * 0.44f, h * 0.68f)
        val p3 = Offset(w * 0.74f, h * 0.34f)

        val firstLegLength = (p2 - p1).getDistance()
        val secondLegLength = (p3 - p2).getDistance()
        val totalLength = firstLegLength + secondLegLength
        val revealedLength = totalLength * progress.value

        if (revealedLength > 0f) {
            if (revealedLength <= firstLegLength) {
                val t = revealedLength / firstLegLength
                drawLine(SignalGreen, p1, lerp(p1, p2, t), strokeWidth = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            } else {
                drawLine(SignalGreen, p1, p2, strokeWidth = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                val t = (revealedLength - firstLegLength) / secondLegLength
                drawLine(SignalGreen, p2, lerp(p2, p3, t.coerceIn(0f, 1f)), strokeWidth = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}

private fun lerp(start: Offset, end: Offset, t: Float): Offset =
    Offset(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t)

// ---- Calendar bottom sheet ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarBottomSheet(
    eventTitle: String,
    eventDate: String,
    eventLocation: String,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Canvas
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Add to calendar", style = MaterialTheme.typography.titleMedium, color = TextOnCanvas)
            Spacer(Modifier.height(16.dp))

            CalendarOptionRow(
                icon = Icons.Default.Event,
                iconTint = SignalGreen,
                label = "Google Calendar",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    openGoogleCalendar(context, eventTitle, eventDate, eventLocation)
                    onDismiss()
                }
            )
            CalendarOptionRow(
                icon = Icons.Default.CalendarMonth,
                iconTint = AccentBlue,
                label = "Device Calendar",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    openDeviceCalendar(context, eventTitle, eventLocation)
                    onDismiss()
                }
            )
            CalendarOptionRow(
                icon = Icons.Default.ContentCopy,
                iconTint = TextOnCanvasSecondary,
                label = "Copy Event Details",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    clipboard.setText(AnnotatedString("$eventTitle\n$eventDate\n$eventLocation"))
                    onDismiss()
                    coroutineScope.launch { snackbarHostState.showSnackbar("Event details copied") }
                }
            )
        }
    }
}

@Composable
private fun CalendarOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextOnCanvas)
    }
}

private fun openGoogleCalendar(context: Context, title: String, date: String, location: String) {
    // Google Calendar's own content provider intent — opens Calendar app directly
    // to a pre-filled "add event" screen rather than a generic share intent.
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = android.net.Uri.parse("content://com.android.calendar/events")
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        putExtra("beginTime", System.currentTimeMillis())
    }
    runCatching { context.startActivity(intent) }
}

private fun openDeviceCalendar(context: Context, title: String, location: String) {
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        putExtra("beginTime", System.currentTimeMillis())
    }
    runCatching { context.startActivity(intent) }
}

// ---- Already responded / invalid token ----

@Composable
private fun AlreadyRespondedState(state: RsvpUiState.AlreadyResponded, onChangeResponse: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StatusBadge(status = state.status)
            Spacer(Modifier.height(16.dp))
            Text(
                "You already responded to this invitation.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            TurnoutButton(text = "Change my response", onClick = onChangeResponse, variant = ButtonVariant.OUTLINE)
        }
    }
}

@Composable
private fun InvalidTokenState(reason: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(DangerRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = DangerRed, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "This invitation link has expired",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(reason, style = MaterialTheme.typography.bodyMedium, color = TextOnCanvasSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Contact the event organizer for a new invitation link.",
                style = MaterialTheme.typography.bodySmall,
                color = TextOnCanvasSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
