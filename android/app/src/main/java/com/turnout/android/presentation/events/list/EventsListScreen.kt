package com.turnout.android.presentation.events.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.*
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.AdaptiveLayoutConfig
import com.turnout.android.core.utils.TurnoutWindowSize
import com.turnout.android.core.utils.deterministicGradient
import com.turnout.android.domain.model.Event
import com.turnout.android.presentation.events.detail.EventDetailContent

private val filterOptions = listOf(null, "DRAFT", "ACTIVE", "COMPLETED", "CANCELLED")

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EventsListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToImport: (Long) -> Unit = {},
    adaptiveConfig: AdaptiveLayoutConfig,
    viewModel: EventsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchActive by remember { mutableStateOf(false) }
    var eventPendingDelete by remember { mutableStateOf<Event?>(null) }
    val compactListState = rememberLazyListState()
    val fabVisible = adaptiveConfig.windowSize != TurnoutWindowSize.Compact || compactListState.isScrollingUp()

    val pullRefreshState = rememberBrandedRefreshState(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            val result = snackbarHostState.showSnackbar(message, actionLabel = "Retry")
            if (result == SnackbarResult.ActionPerformed) viewModel.loadEvents()
        }
    }

    val navItems = remember { emptyList<AdaptiveNavItem>() }

    Scaffold(
        topBar = {
            TurnoutTopBar(
                title = "Events",
                actions = listOf(Icons.Default.Search to { searchActive = !searchActive })
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Create Event") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = onNavigateToCreate,
                    containerColor = AccentBlue,
                    contentColor = Color.White
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = searchActive,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = { Text("Search events...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { status ->
                        val label = status ?: "ALL"
                        val selected = uiState.filter == status
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setFilter(status) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = BorderColor
                            )
                        )
                    }
                }

                when {
                    uiState.isLoading -> LoadingSkeletons(columnCount = adaptiveConfig.columnCount, windowSize = adaptiveConfig.windowSize)
                    uiState.filteredEvents.isEmpty() -> EmptyState(
                        icon = Icons.Default.CalendarMonth,
                        title = "No events yet",
                        subtitle = "Create your first event to get started",
                        actionLabel = "Create Event",
                        onAction = onNavigateToCreate
                    )
                    adaptiveConfig.windowSize == TurnoutWindowSize.Expanded -> {
                        TwoPaneLayout(
                            listPane = {
                                EventsGridOrList(
                                    events = uiState.filteredEvents,
                                    windowSize = adaptiveConfig.windowSize,
                                    listState = compactListState,
                                    onEventClick = { id -> viewModel.selectEvent(id) },
                                    onDeleteRequest = { event -> eventPendingDelete = event }
                                )
                            },
                            detailPane = {
                                uiState.selectedEventId?.let { id -> EventDetailContent(eventId = id, onNavigateToImport = onNavigateToImport) }
                            },
                            showDetail = uiState.selectedEventId != null
                        )
                    }
                    else -> EventsGridOrList(
                        events = uiState.filteredEvents,
                        windowSize = adaptiveConfig.windowSize,
                        listState = compactListState,
                        onEventClick = onNavigateToDetail,
                        onDeleteRequest = { event -> eventPendingDelete = event }
                    )
                }
            }

            PullRefreshIndicatorCompat(pullRefreshState = pullRefreshState, isRefreshing = uiState.isRefreshing)
        }
    }

    eventPendingDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventPendingDelete = null },
            title = { Text("Delete \"${event.title}\"?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEvent(event.id)
                    eventPendingDelete = null
                }) { Text("Delete", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { eventPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PullRefreshIndicatorCompat(pullRefreshState: androidx.compose.material.pullrefresh.PullRefreshState, isRefreshing: Boolean) {
    BrandedRefreshIndicator(pullRefreshState = pullRefreshState, isRefreshing = isRefreshing)
}

@Composable
private fun EventsGridOrList(
    events: List<Event>,
    windowSize: TurnoutWindowSize,
    listState: LazyListState,
    onEventClick: (Long) -> Unit,
    onDeleteRequest: (Event) -> Unit
) {
    if (windowSize == TurnoutWindowSize.Compact) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events, key = { it.id }) { event ->
                EventCard(event = event, onClick = { onEventClick(event.id) }, onDeleteRequest = { onDeleteRequest(event) })
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events, key = { it.id }) { event ->
                EventCard(event = event, onClick = { onEventClick(event.id) }, onDeleteRequest = { onDeleteRequest(event) })
            }
        }
    }
}

@Composable
private fun LoadingSkeletons(columnCount: Int, windowSize: TurnoutWindowSize) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) { index ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(index * 50L)
                visible = true
            }
            val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, label = "skeleton_stagger")
            SkeletonLoader(modifier = Modifier.fillMaxWidth().height(180.dp).graphicsLayer { this.alpha = alpha })
        }
    }
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit, onDeleteRequest: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val (colorA, colorB) = deterministicGradient(event.id)
    val capacityFraction = if (event.capacity > 0) event.confirmedCount.toFloat() / event.capacity else 0f

    val progressColor = when {
        capacityFraction >= 0.9f -> DangerRed
        capacityFraction >= 0.7f -> WarningAmber
        else -> SignalGreen
    }

    val glowAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (capacityFraction >= 0.9f) {
            infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse)
        } else {
            tween(0)
        },
        label = "capacity_glow"
    )

    TurnoutCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(8.dp))) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(colorA, colorB)))
            )
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                StatusBadge(status = event.status)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            event.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(event.eventDate, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        Spacer(Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Place, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(event.location, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { capacityFraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .graphicsLayer { alpha = if (capacityFraction >= 0.9f) glowAlpha else 1f },
            color = progressColor,
            trackColor = BorderColor
        )

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${event.confirmedCount}/${event.capacity}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("View") }, onClick = { menuExpanded = false; onClick() })
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuExpanded = false; onClick() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuExpanded = false; onDeleteRequest() })
                }
            }
        }
    }
}

/** True while the list is scrolling up (or at rest) — used to keep the FAB visible
 *  except while actively scrolling down, where it hides to give content more room. */
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}
