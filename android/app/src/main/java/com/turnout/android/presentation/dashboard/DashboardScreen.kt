package com.turnout.android.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.*
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.LocalAdaptiveConfig
import com.turnout.android.core.utils.deterministicColor
import com.turnout.android.core.utils.deterministicGradient
import com.turnout.android.core.utils.hapticClick
import com.turnout.android.core.utils.TurnoutWindowSize
import com.turnout.android.core.utils.WsState
import com.turnout.android.domain.model.Event
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

@Composable
fun DashboardScreen(
    onNavigateToCreateEvent: () -> Unit = {},
    onNavigateToEventsList: () -> Unit = {},
    onNavigateToAi: () -> Unit = {},
    onNavigateToEventDetail: (Long) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val wsState by viewModel.wsState.collectAsStateWithLifecycle()
    val adaptiveConfig = LocalAdaptiveConfig.current
    val haptic = LocalHapticFeedback.current

    val flashController = remember { PulseLineFlashController() }
    LaunchedEffect(Unit) {
        viewModel.wsFlash.collectLatest { flashController.flash() }
    }

    var showLiveLabel by remember { mutableStateOf(false) }
    LaunchedEffect(wsState) {
        if (wsState is WsState.Connected) {
            showLiveLabel = true
            kotlinx.coroutines.delay(2000)
            showLiveLabel = false
        }
    }

    Scaffold(topBar = { TurnoutTopBar(title = "Dashboard") }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    PulseLine(isActive = wsState is WsState.Connected, flashController = flashController)
                }
                Spacer(Modifier.width(8.dp))
                when {
                    wsState is WsState.Disconnected || wsState is WsState.Connecting -> {
                        Text("Reconnecting...", style = MaterialTheme.typography.labelSmall, color = TextOnCanvasSecondary)
                    }
                    wsState is WsState.Connected -> {
                        AnimatedVisibility(visible = showLiveLabel) {
                            Text("Live", style = MaterialTheme.typography.labelSmall, color = SignalGreen)
                        }
                    }
                    else -> Unit
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    if (uiState.isLoading) {
                        SkeletonLoader(modifier = Modifier.fillMaxWidth().height(100.dp))
                    } else {
                        GreetingCard(uiState = uiState)
                    }
                }

                item {
                    if (uiState.isLoading) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(2) { SkeletonLoader(modifier = Modifier.weight(1f).height(90.dp)) }
                        }
                    } else {
                        StatsSection(uiState = uiState, columnCount = adaptiveConfig.columnCount)
                    }
                }

                item {
                    QuickActionsRow(
                        haptic = haptic,
                        onCreateEvent = onNavigateToCreateEvent,
                        onImport = onNavigateToEventsList,
                        onSendInvites = onNavigateToEventsList,
                        onAiTools = onNavigateToAi
                    )
                }

                item {
                    LiveActivitySection(
                        items = uiState.liveActivity,
                        isConnected = wsState is WsState.Connected,
                        windowSize = adaptiveConfig.windowSize
                    )
                }

                item {
                    MyEventsSection(
                        events = uiState.events,
                        isLoading = uiState.isLoading,
                        windowSize = adaptiveConfig.windowSize,
                        onEventClick = onNavigateToEventDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun GreetingCard(uiState: DashboardUiState) {
    TurnoutCard(modifier = Modifier.fillMaxWidth(), elevation = 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(NavyPrimary, AccentBlue)))
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = uiState.greeting,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceGroteskFontFamily),
                    color = Color.White
                )
                uiState.contextualNote?.let { note ->
                    Spacer(Modifier.height(4.dp))
                    Text(note, style = MaterialTheme.typography.bodyMedium, color = TextOnCanvasSecondary)
                }
                uiState.capacityWarning?.let { warning ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(warning, style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSection(uiState: DashboardUiState, columnCount: Int) {
    val stats = listOf(
        Triple("My Events", uiState.totalEvents, Icons.Default.Event),
        Triple("Confirmed RSVPs", uiState.totalConfirmedRsvps, Icons.Default.CheckCircle),
        Triple("Guests Invited", uiState.totalGuestsInvited, Icons.Default.People),
        Triple("Pending", uiState.pendingRsvps, Icons.Default.Schedule)
    )

    ResponsiveGrid(
        columnCount = columnCount,
        itemSpacing = 8.dp,
        modifier = Modifier.height(if (columnCount <= 2) 190.dp else 90.dp)
    ) {
        items(stats) { (label, value, icon) ->
            StatCard(label = label, value = value, icon = icon)
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, icon: ImageVector) {
    TurnoutCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.Start) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            AnimatedCounter(targetValue = value)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

private data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun QuickActionsRow(
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onCreateEvent: () -> Unit,
    onImport: () -> Unit,
    onSendInvites: () -> Unit,
    onAiTools: () -> Unit
) {
    val actions = listOf(
        QuickAction("Create Event", Icons.Default.Add, onCreateEvent),
        QuickAction("Import", Icons.Default.Upload, onImport),
        QuickAction("Send Invites", Icons.Default.Send, onSendInvites),
        QuickAction("AI Tools", Icons.Default.AutoAwesome, onAiTools)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(actions) { action ->
            ActionChip(action = action, haptic = haptic)
        }
    }
}

@Composable
private fun ActionChip(action: QuickAction, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .then(Modifier.hapticClick(haptic) { action.onClick() })
            .background(Color.Transparent)
            .padding(1.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AccentBlue.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(action.icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(action.label, style = MaterialTheme.typography.labelLarge, color = AccentBlue)
    }
}

@Composable
private fun LiveActivitySection(items: List<LiveActivityItem>, isConnected: Boolean, windowSize: TurnoutWindowSize) {
    TurnoutCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Live Activity", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            if (isConnected) {
                PulsingDot()
                Spacer(Modifier.width(4.dp))
                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = SignalGreen)
            }
        }

        Spacer(Modifier.height(12.dp))

        val listHeight = if (windowSize == TurnoutWindowSize.Compact) 240.dp else 320.dp

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(listHeight)) {
                EmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = "No activity yet",
                    subtitle = "Send invitations to get started"
                )
            }
        } else {
            LazyColumn(modifier = Modifier.height(listHeight), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(items, key = { it.id }) { item ->
                    RsvpActivityRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun RsvpActivityRow(item: LiveActivityItem) {
    var visible by remember { mutableStateOf(false) }
    val flashAlpha = remember { Animatable(0.15f) }

    LaunchedEffect(item.id) {
        visible = true
        flashAlpha.animateTo(0f, tween(1500))
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -16 }) + fadeIn(tween(300))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AccentBlue.copy(alpha = flashAlpha.value))
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarColor = deterministicColor(item.guestName)
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.guestName.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.guestName, style = MaterialTheme.typography.bodyMedium, color = TextOnCanvas, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.eventName, style = MaterialTheme.typography.bodySmall, color = TextOnCanvasSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(status = item.status)
        }
    }
}

@Composable
private fun MyEventsSection(
    events: List<Event>,
    isLoading: Boolean,
    windowSize: TurnoutWindowSize,
    onEventClick: (Long) -> Unit
) {
    Column {
        Text("My Events", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

        if (isLoading) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { SkeletonLoader(modifier = Modifier.width(200.dp).height(140.dp)) }
            }
        } else if (windowSize == TurnoutWindowSize.Expanded) {
            ResponsiveGrid(columnCount = 3, itemSpacing = 12.dp, modifier = Modifier.height(160.dp * ((events.size / 3) + 1))) {
                items(events, key = { it.id }) { event -> EventCard(event = event, onClick = { onEventClick(event.id) }) }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(events, key = { it.id }) { event ->
                    Box(modifier = Modifier.width(200.dp)) {
                        EventCard(event = event, onClick = { onEventClick(event.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
    val (colorA, colorB) = deterministicGradient(event.id)

    TurnoutCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(colorA, colorB)))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            event.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(event.eventDate, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        val progress = if (event.capacity > 0) event.confirmedCount.toFloat() / event.capacity else 0f
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = AccentBlue,
            trackColor = BorderColor
        )
        Spacer(Modifier.height(6.dp))
        StatusBadge(status = event.status)
    }
}

