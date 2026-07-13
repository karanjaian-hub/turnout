package com.turnout.android.presentation.guests

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.TurnoutButton
import com.turnout.android.core.components.TurnoutCard
import com.turnout.android.core.components.TurnoutTopBar
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.TurnoutWindowSize
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ImportCsvScreen(
    eventId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ImportCsvViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSize = LocalAdaptiveConfig.current.windowSize
    val snackbarHostState = remember { SnackbarHostState() }
    var showFailures by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.selectFile(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ImportCsvEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = { TurnoutTopBar(title = "Import Guests", onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            val contentModifier = if (windowSize == TurnoutWindowSize.Compact) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.widthIn(max = 700.dp)
            }

            Column(modifier = contentModifier.padding(16.dp)) {
                AnimatedContent(targetState = uiState.importResult != null, label = "import_state") { showResults ->
                    if (showResults) {
                        ResultsView(
                            uiState = uiState,
                            showFailures = showFailures,
                            onToggleFailures = { showFailures = !showFailures },
                            onDone = onNavigateBack
                        )
                    } else {
                        if (windowSize == TurnoutWindowSize.Expanded) {
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    UploadArea(uiState = uiState, windowSize = windowSize, onPick = { filePicker.launch("*/*") })
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormatAndActions(
                                        uiState = uiState,
                                        eventId = eventId,
                                        viewModel = viewModel
                                    )
                                }
                            }
                        } else {
                            Column {
                                UploadArea(uiState = uiState, windowSize = windowSize, onPick = { filePicker.launch("*/*") })
                                Spacer(Modifier.height(16.dp))
                                FormatAndActions(uiState = uiState, eventId = eventId, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadArea(uiState: ImportCsvUiState, windowSize: TurnoutWindowSize, onPick: () -> Unit) {
    val height = if (windowSize == TurnoutWindowSize.Expanded) 200.dp else 160.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = BorderColor,
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 6.dp.toPx()), 0f)
                )
            )
        }

        if (uiState.selectedFileUri == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = AccentBlue.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Tap to select CSV file", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Supported format: .csv with full_name and email columns",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text(uiState.fileName ?: "", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                uiState.fileSize?.let { size ->
                    Text("${size / 1024} KB", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                TextButton(onClick = onPick) { Text("Change file", color = AccentBlue) }
            }
        }
    }
}

@Composable
private fun FormatAndActions(uiState: ImportCsvUiState, eventId: Long, viewModel: ImportCsvViewModel) {
    Column {
        TurnoutCard(modifier = Modifier.fillMaxWidth()) {
            Text("Required columns:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "full_name, email",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = AccentBlue
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.downloadSampleTemplate() }) {
                Icon(Icons.Default.Download, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download Sample Template", color = AccentBlue)
            }
        }

        Spacer(Modifier.height(16.dp))

        TurnoutButton(
            text = "Upload & Import",
            onClick = { viewModel.importGuests(eventId) },
            enabled = uiState.selectedFileUri != null && !uiState.isUploading,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.isUploading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        uiState.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ResultsView(
    uiState: ImportCsvUiState,
    showFailures: Boolean,
    onToggleFailures: () -> Unit,
    onDone: () -> Unit
) {
    val result = uiState.importResult ?: return

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TurnoutCard(modifier = Modifier.weight(1f)) {
                Text("${result.successCount}", style = MaterialTheme.typography.headlineMedium, color = SignalGreen)
                Text("imported", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            TurnoutCard(modifier = Modifier.weight(1f)) {
                Text("${result.failureCount}", style = MaterialTheme.typography.headlineMedium, color = DangerRed)
                Text("failed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        if (result.failureCount > 0) {
            TextButton(onClick = onToggleFailures) {
                Text(if (showFailures) "Hide failures" else "See failures", color = DangerRed)
            }
            // Note: individual failed-row detail (row number, email, reason) isn't
            // available here — ImportGuestsUseCase's result only carries success/failure
            // counts, not the per-row ImportError list the API itself returns. That detail
            // exists in ImportResultDto but currently gets discarded in the repository
            // mapping. Flagging rather than fabricating fake row data to fill this in.
            AnimatedVisibility(visible = showFailures) {
                Text(
                    "Failure details aren't available yet — only the total failure count. " +
                        "This can be added if the import flow needs to expose per-row errors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        TurnoutButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}
