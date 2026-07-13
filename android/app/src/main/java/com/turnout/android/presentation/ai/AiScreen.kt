package com.turnout.android.presentation.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.SkeletonLoader
import com.turnout.android.core.components.TurnoutButton
import com.turnout.android.core.components.TurnoutCard
import com.turnout.android.core.components.TurnoutTextField
import com.turnout.android.core.components.TurnoutTopBar
import com.turnout.android.core.components.TypewriterText
import com.turnout.android.core.theme.*
import kotlinx.coroutines.launch

private data class FeatureMeta(val feature: AiFeature, val label: String, val icon: ImageVector)

private val featureMetas = listOf(
    FeatureMeta(AiFeature.DESCRIPTION, "Event Description", Icons.Default.Description),
    FeatureMeta(AiFeature.INVITATION, "Invitation Copy", Icons.Default.MailOutline),
    FeatureMeta(AiFeature.RSVP_INSIGHTS, "RSVP Insights", Icons.Default.Insights),
    FeatureMeta(AiFeature.SEND_TIME, "Best Send Time", Icons.Default.Schedule),
    FeatureMeta(AiFeature.CAPACITY_FORECAST, "Capacity Forecast", Icons.Default.TrendingUp),
    FeatureMeta(AiFeature.FOLLOWUP, "Follow-up Suggestions", Icons.Default.Forum)
)

@Composable
fun AiScreen(viewModel: AiViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedFeature by remember { mutableStateOf<AiFeature?>(null) }

    Scaffold(topBar = { TurnoutTopBar(title = "AI Tools") }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (uiState.events.isNotEmpty()) {
                    EventPicker(
                        events = uiState.events,
                        selectedEventId = uiState.selectedEventId,
                        onSelect = viewModel::selectEvent
                    )
                }
            }

            items(featureMetas) { meta ->
                FeatureCard(
                    meta = meta,
                    state = uiState.featureStates[meta.feature] ?: AiFeatureState.Idle,
                    isExpanded = expandedFeature == meta.feature,
                    hasEventSelected = uiState.selectedEventId != null,
                    onToggleExpand = {
                        expandedFeature = if (expandedFeature == meta.feature) null else meta.feature
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventPicker(events: List<com.turnout.android.domain.model.Event>, selectedEventId: Long?, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedEvent = events.firstOrNull { it.id == selectedEventId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TurnoutTextField(
            value = selectedEvent?.title ?: "Select an event",
            onValueChange = {},
            label = "Event context",
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            events.forEach { event ->
                DropdownMenuItem(
                    text = { Text(event.title) },
                    onClick = { onSelect(event.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    meta: FeatureMeta,
    state: AiFeatureState,
    isExpanded: Boolean,
    hasEventSelected: Boolean,
    onToggleExpand: () -> Unit,
    viewModel: AiViewModel
) {
    TurnoutCard(modifier = Modifier.fillMaxWidth(), onClick = onToggleExpand) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(meta.icon, contentDescription = null, tint = AccentBlue)
            Spacer(Modifier.width(12.dp))
            Text(meta.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            if (state is AiFeatureState.Success) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
            )
        }

        AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                FeatureContent(meta = meta, state = state, hasEventSelected = hasEventSelected, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun FeatureContent(
    meta: FeatureMeta,
    state: AiFeatureState,
    hasEventSelected: Boolean,
    viewModel: AiViewModel
) {
    val clipboard = LocalClipboardManager.current
    val snackbarScope = rememberCoroutineScope()

    // Two genuinely different input shapes: description/invitation take free-text notes,
    // the four event-context features just need a button (event already picked above).
    if (meta.feature == AiFeature.DESCRIPTION || meta.feature == AiFeature.INVITATION) {
        var notes by remember { mutableStateOf("") }
        TurnoutTextField(value = notes, onValueChange = { notes = it }, label = "Notes about the event", singleLine = false)
        Spacer(Modifier.height(8.dp))
        TurnoutButton(
            text = "Generate",
            onClick = {
                if (meta.feature == AiFeature.DESCRIPTION) viewModel.generateDescription(notes)
                else viewModel.generateInvitation(notes)
            },
            enabled = notes.isNotBlank() && state !is AiFeatureState.Loading,
            modifier = Modifier.fillMaxWidth()
        )
    } else if (meta.feature == AiFeature.FOLLOWUP) {
        var tone by remember { mutableStateOf("friendly") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("friendly", "professional", "urgent").forEach { option ->
                FilterChip(
                    selected = tone == option,
                    onClick = { tone = option },
                    label = { Text(option.replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        TurnoutButton(
            text = "Generate",
            onClick = { viewModel.generateFollowupSuggestions(tone) },
            enabled = hasEventSelected && state !is AiFeatureState.Loading,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        TurnoutButton(
            text = "Generate",
            onClick = {
                when (meta.feature) {
                    AiFeature.RSVP_INSIGHTS -> viewModel.generateRsvpInsights()
                    AiFeature.SEND_TIME -> viewModel.generateSendTimeOptimization()
                    AiFeature.CAPACITY_FORECAST -> viewModel.generateCapacityForecast()
                    else -> Unit
                }
            },
            enabled = hasEventSelected && state !is AiFeatureState.Loading,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(12.dp))

    when (state) {
        is AiFeatureState.Idle -> Unit
        is AiFeatureState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(14.dp)) }
        }
        is AiFeatureState.Success -> Column {
            TypewriterText(fullText = state.result, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Generated ${formatTimeAgo(state.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { clipboard.setText(AnnotatedString(state.result)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", color = AccentBlue)
                }
            }
        }
        is AiFeatureState.Error -> Column {
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = DangerRed)
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val minutes = diffMs / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
    }
}
