package com.turnout.android.presentation.events.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.*
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.deterministicGradient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val tabTitles = listOf("Overview", "Guests", "Email Logs", "AI Insights")

/** Thin routed-screen wrapper — just a generic top bar with back navigation. The real
 *  event title/hero/actions all live inside EventDetailContent itself, which is also
 *  used directly (without this wrapper) inside EventsListScreen's Expanded two-pane view. */
@Composable
fun EventDetailScreen(eventId: Long, onNavigateBack: () -> Unit, onNavigateToImport: (Long) -> Unit = {}) {
    Scaffold(topBar = { TurnoutTopBar(title = "Event Details", onNavigateBack = onNavigateBack) }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            EventDetailContent(eventId = eventId, onDeleted = onNavigateBack, onNavigateToImport = onNavigateToImport)
        }
    }
}

/**
 * Fully self-contained event detail body — hero, status menu, delete, send-invitations
 * FAB, tabs, pager. Deliberately owns its own FAB/delete trigger (not via a Scaffold)
 * so it works identically whether wrapped by EventDetailScreen's Scaffold above, or
 * embedded directly inside EventsListScreen's two-pane detail slot, which has no
 * Scaffold of its own around this content.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EventDetailContent(
    eventId: Long,
    onDeleted: () -> Unit = {},
    onNavigateToImport: (Long) -> Unit = {},
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSendSheet by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(eventId) { viewModel.initialize(eventId) }
    LaunchedEffect(pagerState.currentPage) { viewModel.setTab(pagerState.currentPage) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EventDetailEvent.NavigateBackAfterDelete -> onDeleted()
                is EventDetailEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading || uiState.event == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (uiState.error != null) {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = "Couldn't load event",
                        subtitle = uiState.error ?: "",
                        actionLabel = "Retry",
                        onAction = { viewModel.initialize(eventId) }
                    )
                } else {
                    SkeletonLoader(modifier = Modifier.fillMaxWidth().padding(16.dp).height(200.dp))
                }
            }
        } else {
            val event = uiState.event!!
            val (colorA, colorB) = deterministicGradient(event.id)

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Brush.linearGradient(listOf(colorA, colorB)))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopEnd), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete event", tint = Color.White)
                        }
                    }

                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        StatusBadge(status = event.status)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            event.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SpaceGroteskFontFamily),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(event.eventDate, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { showStatusMenu = true }) {
                                Text("Change status", color = Color.White)
                            }
                            DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                                listOf("DRAFT", "ACTIVE", "COMPLETED", "CANCELLED").forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status) },
                                        onClick = { viewModel.changeStatus(status); showStatusMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }

                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title) }
                        )
                    }
                }

                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    when (page) {
                        0 -> OverviewTab(uiState = uiState)
                        1 -> com.turnout.android.presentation.guests.GuestListContent(
                            eventId = eventId,
                            eventStatus = event.status,
                            onNavigateToImport = { onNavigateToImport(eventId) }
                        )
                        2 -> EmailLogsTab(uiState = uiState, onRetry = viewModel::retryEmail)
                        else -> AiInsightsTab(uiState = uiState)
                    }
                }
            }

            ExtendedFloatingActionButton(
                text = { Text(if (uiState.sendInvitesInProgress) "Sending..." else "Send Invitations") },
                icon = { Icon(Icons.Default.Send, contentDescription = null) },
                onClick = { showSendSheet = true },
                containerColor = AccentBlue,
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this event?") },
            text = { Text("This cannot be undone. All guest data for this event will also be removed.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteEvent() }) {
                    Text("Delete", color = DangerRed)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSendSheet) {
        ModalBottomSheet(onDismissRequest = { showSendSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Send Invitations", style = MaterialTheme.typography.titleLarge)

                if (uiState.sendInvitesInProgress) {
                    PulseLine(isActive = true)
                    Text(
                        "${uiState.emailSent} / ${uiState.emailTotal} sent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        "This will send invitation emails to all guests who haven't been invited yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    TurnoutButton(
                        text = "Confirm & Send",
                        onClick = { viewModel.sendInvitations() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(uiState: EventDetailUiState) {
    val stats = uiState.stats
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (stats == null) {
            SkeletonLoader(modifier = Modifier.fillMaxWidth().height(100.dp))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Confirmed", stats.confirmedCount, SignalGreen, Modifier.weight(1f))
                StatChip("Pending", stats.pendingCount, WarningAmber, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Declined", stats.declinedCount, DangerRed, Modifier.weight(1f))
                StatChip("Waitlisted", stats.waitlistedCount, InfoPurple, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))

        uiState.event?.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text("Description", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun StatChip(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    TurnoutCard(modifier = modifier) {
        AnimatedCounter(targetValue = value)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun GuestsTabPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Default.People,
            title = "Guest List",
            subtitle = "Full guest management arrives in Phase 6"
        )
    }
}

@Composable
private fun EmailLogsTab(uiState: EventDetailUiState, onRetry: (Long) -> Unit) {
    when {
        uiState.emailLogsLoading -> Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(56.dp)) }
        }
        uiState.emailLogs.isEmpty() -> EmptyState(
            icon = Icons.Default.Email,
            title = "No emails sent yet",
            subtitle = "Send invitations to see delivery status here"
        )
        else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.emailLogs, key = { it.id }) { log ->
                TurnoutCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.guestName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(log.timestamp, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            log.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = DangerRed) }
                        }
                        StatusBadge(status = log.status)
                        if (log.status == "FAILED") {
                            IconButton(onClick = { onRetry(log.id) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = AccentBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiInsightsTab(uiState: EventDetailUiState) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TurnoutCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue)
                Spacer(Modifier.width(8.dp))
                Text("RSVP Insights", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            if (uiState.aiInsightsLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(14.dp)) }
                }
            } else if (uiState.aiInsights != null) {
                TypewriterText(fullText = uiState.aiInsights, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Not available yet", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        TurnoutCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue)
                Spacer(Modifier.width(8.dp))
                Text("Capacity Forecast", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            if (uiState.aiForecastLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(14.dp)) }
                }
            } else if (uiState.aiForecast != null) {
                TypewriterText(fullText = uiState.aiForecast, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Not available yet", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
