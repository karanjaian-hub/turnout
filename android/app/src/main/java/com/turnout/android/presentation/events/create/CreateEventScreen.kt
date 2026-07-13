package com.turnout.android.presentation.events.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.*
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val suggestedCapacities = listOf(50, 100, 500, 1000, 5000, 10000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSize = LocalAdaptiveConfig.current.windowSize
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showDiscardDialog by remember { mutableStateOf(false) }

    val handleBack: () -> Unit = {
        if (viewModel.hasUnsavedContent()) showDiscardDialog = true else onNavigateBack()
    }

    LaunchedEffect(uiState.draftRestored) {
        if (uiState.draftRestored) {
            snackbarHostState.showSnackbar("Draft restored — continue where you left off")
        }
    }

    LaunchedEffect(uiState.draftSaved) {
        if (uiState.draftSaved) onNavigateBack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { TurnoutTopBar(title = "Create Event", onNavigateBack = handleBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            val contentModifier = if (windowSize == TurnoutWindowSize.Compact) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.widthIn(max = 600.dp)
            }

            Column(
                modifier = contentModifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                StepIndicatorRow(currentStep = uiState.currentStep)
                Spacer(Modifier.height(24.dp))

                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "create_event_step"
                ) { step ->
                    when (step) {
                        1 -> StepBasicInfo(uiState = uiState, viewModel = viewModel)
                        2 -> StepDateLocation(uiState = uiState, viewModel = viewModel)
                        3 -> StepCapacity(uiState = uiState, viewModel = viewModel, haptic = haptic)
                        else -> StepReview(uiState = uiState, viewModel = viewModel)
                    }
                }

                uiState.stepError?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = DangerRed, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(24.dp))

                if (uiState.currentStep < 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (uiState.currentStep > 1) {
                            TurnoutButton(
                                text = "Back",
                                onClick = { viewModel.goToPreviousStep() },
                                variant = ButtonVariant.OUTLINE,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TurnoutButton(
                            text = "Next",
                            onClick = { viewModel.goToNextStep() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your draft has been auto-saved and can be restored next time you create an event.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onNavigateBack() }) {
                    Text("Leave", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }
}

@Composable
private fun StepIndicatorRow(currentStep: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        for (step in 1..4) {
            StepDot(step = step, currentStep = currentStep)
            if (step < 4) {
                val lineProgress by animateFloatAsState(
                    targetValue = if (step < currentStep) 1f else 0f,
                    label = "step_line_progress"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(BorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(lineProgress)
                            .background(SignalGreen)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepDot(step: Int, currentStep: Int) {
    val isPast = step < currentStep
    val isCurrent = step == currentStep

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                when {
                    isPast -> SignalGreen
                    isCurrent -> AccentBlue
                    else -> Color.Transparent
                }
            )
            .then(
                if (!isPast && !isCurrent) Modifier.background(Color.Transparent) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isPast -> Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            isCurrent -> Text("$step", color = Color.White, fontWeight = FontWeight.Bold)
            else -> Text("$step", color = TextSecondary)
        }
    }
}

@Composable
private fun StepBasicInfo(uiState: CreateEventUiState, viewModel: CreateEventViewModel) {
    var aiAssistExpanded by remember { mutableStateOf(false) }
    var aiNotes by remember { mutableStateOf("") }
    var descriptionTypedOnce by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TurnoutTextField(value = uiState.title, onValueChange = viewModel::updateTitle, label = "Event Title", isError = false)

        if (uiState.aiLoading) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(16.dp)) }
            }
        } else if (descriptionTypedOnce) {
            TurnoutTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = "Description",
                singleLine = false
            )
        } else {
            TypewriterText(
                fullText = uiState.description.ifBlank { " " },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.description.isNotBlank()) {
                LaunchedEffect(uiState.description) {
                    kotlinx.coroutines.delay((uiState.description.length * 15L) + 200)
                    descriptionTypedOnce = true
                }
            } else {
                TurnoutTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = "Description",
                    singleLine = false
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            TextButton(onClick = { aiAssistExpanded = !aiAssistExpanded }) {
                Text("Generate with AI", color = AccentBlue)
            }
        }

        if (aiAssistExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TurnoutTextField(
                    value = aiNotes,
                    onValueChange = { aiNotes = it },
                    label = "Describe your event briefly...",
                    singleLine = false
                )
                TurnoutButton(
                    text = "Generate",
                    onClick = {
                        descriptionTypedOnce = false
                        viewModel.generateDescription(aiNotes)
                        aiAssistExpanded = false
                    },
                    enabled = aiNotes.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepDateLocation(uiState: CreateEventUiState, viewModel: CreateEventViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    val formattedDate = remember(uiState.eventDate) {
        runCatching {
            LocalDateTime.parse(uiState.eventDate).format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
        }.getOrNull()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TurnoutCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AccentBlue)
                Spacer(Modifier.width(12.dp))
                Text(
                    formattedDate ?: "Tap to select date and time",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (formattedDate != null) TextPrimary else TextSecondary
                )
            }
        }

        TurnoutTextField(value = uiState.location, onValueChange = viewModel::updateLocation, label = "Location")
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = pendingDateMillis ?: System.currentTimeMillis()
                    val date = java.time.Instant.ofEpochMilli(dateMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                    val dateTime = LocalDateTime.of(date, java.time.LocalTime.of(timePickerState.hour, timePickerState.minute))
                    viewModel.updateEventDate(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StepCapacity(uiState: CreateEventUiState, viewModel: CreateEventViewModel, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedCounter(targetValue = uiState.capacity)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { viewModel.updateCapacity(uiState.capacity - 10) },
                enabled = uiState.capacity > 10
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = if (uiState.capacity > 10) DangerRed else BorderColor)
            }
            Slider(
                value = uiState.capacity.toFloat(),
                onValueChange = { viewModel.updateCapacity(it.toInt()) },
                valueRange = 10f..50000f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
            )
            IconButton(onClick = { viewModel.updateCapacity(uiState.capacity + 10) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = AccentBlue)
            }
        }

        if (uiState.capacity < 10) {
            Text("Minimum capacity is 10", style = MaterialTheme.typography.labelSmall, color = DangerRed)
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestedCapacities) { value ->
                AssistChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.updateCapacity(value)
                    },
                    label = { Text(value.toString()) }
                )
            }
        }
    }
}

@Composable
private fun StepReview(uiState: CreateEventUiState, viewModel: CreateEventViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TurnoutCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewRow("Title", uiState.title)
                ReviewRow("Description", uiState.description.ifBlank { "—" })
                ReviewRow("Date", uiState.eventDate.ifBlank { "—" })
                ReviewRow("Location", uiState.location.ifBlank { "—" })
                ReviewRow("Capacity", uiState.capacity.toString())
            }
        }

        if (uiState.isLoading) {
            PulseLine(isActive = true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TurnoutButton(
                text = "Save as Draft",
                onClick = { viewModel.submit(activate = false) },
                isLoading = uiState.isLoading,
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.weight(1f)
            )
            TurnoutButton(
                text = "Create & Activate",
                onClick = { viewModel.submit(activate = true) },
                isLoading = uiState.isLoading,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(2f))
    }
}
