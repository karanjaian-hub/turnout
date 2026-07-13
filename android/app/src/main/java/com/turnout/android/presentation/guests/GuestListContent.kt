package com.turnout.android.presentation.guests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.*
import com.turnout.android.core.theme.*
import com.turnout.android.core.utils.deterministicColor
import com.turnout.android.domain.model.Guest

private val guestFilterOptions = listOf(null, "CONFIRMED", "PENDING", "DECLINED", "WAITLISTED")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestListContent(
    eventId: Long,
    eventStatus: String,
    onNavigateToImport: () -> Unit,
    viewModel: GuestListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var searchActive by remember { mutableStateOf(false) }
    var selectedGuest by remember { mutableStateOf<Guest?>(null) }
    var guestPendingDelete by remember { mutableStateOf<Guest?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(eventId) { viewModel.initialize(eventId) }

    // Trigger the next page load once the user scrolls near the bottom of what's loaded.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val total = uiState.guests.size
                if (lastVisibleIndex != null && lastVisibleIndex >= total - 5) {
                    viewModel.loadNextPageIfNeeded()
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search guests...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onNavigateToImport) {
                Icon(Icons.Default.Upload, contentDescription = "Import CSV", tint = AccentBlue)
            }
        }

                val allPending = eventStatus == "ACTIVE" && uiState.guests.isNotEmpty() && uiState.guests.all { it.rsvpStatus == "PENDING" }
        AnimatedVisibility(visible = allPending) {
            TurnoutCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = { viewModel.sendAllInvitations() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = AccentBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("All guests are pending — tap to send invitations", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text("Send Now", color = AccentBlue, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            guestFilterOptions.forEach { status ->
                val label = status ?: "ALL"
                val selected = uiState.filter == status
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setFilter(status) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        when {
            uiState.isLoading -> Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(64.dp)) }
            }

            uiState.guests.isEmpty() -> {
                // Contextual empty state: differs depending on whether guests were ever
                // imported for this event at all, versus a search/filter just matching nothing.
                val (icon, title, subtitle, actionLabel, onAction) = when {
                    uiState.searchQuery.isNotBlank() || uiState.filter != null ->
                        EmptyStateSpec(Icons.Default.SearchOff, "No matches", "Try a different search or filter", null, null)
                    eventStatus == "DRAFT" ->
                        // No action button here on purpose — importing guests into a
                        // DRAFT event that isn't activated yet would be premature.
                        EmptyStateSpec(Icons.Default.EditCalendar, "Activate this event first", "Guests can be imported once the event is active", null, null)
                    else ->
                        EmptyStateSpec(Icons.Default.People, "No guests yet", "Import a CSV to add guests to this event", "Import Guests", onNavigateToImport)
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    EmptyState(icon = icon, title = title, subtitle = subtitle, actionLabel = actionLabel, onAction = onAction ?: {})
                }
            }

            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.guests, key = { it.id }) { guest ->
                    SwipeableGuestRow(
                        guest = guest,
                        onClick = { selectedGuest = guest },
                        onDeleteRequest = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            guestPendingDelete = guest
                        },
                        onResend = { viewModel.resendInvitation(guest.id) }
                    )
                }
                if (uiState.isLoadingMore) {
                    item { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(64.dp)) }
                }
            }
        }
    }

    selectedGuest?.let { guest ->
        GuestDetailSheet(guest = guest, onDismiss = { selectedGuest = null })
    }

    guestPendingDelete?.let { guest ->
        AlertDialog(
            onDismissRequest = { guestPendingDelete = null },
            title = { Text("Remove ${guest.fullName}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteGuest(guest.id); guestPendingDelete = null }) {
                    Text("Remove", color = DangerRed)
                }
            },
            dismissButton = { TextButton(onClick = { guestPendingDelete = null }) { Text("Cancel") } }
        )
    }
}

private data class EmptyStateSpec(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val actionLabel: String?,
    val onAction: (() -> Unit)?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableGuestRow(
    guest: Guest,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    onResend: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> onDeleteRequest()
                SwipeToDismissBoxValue.StartToEnd -> onResend()
                else -> Unit
            }
            false // always reset visually — deletion needs confirmation, resend is instant but shouldn't leave the row dismissed
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (direction == SwipeToDismissBoxValue.StartToEnd) AccentBlue else DangerRed)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Mail else Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        enableDismissFromStartToEnd = true
    ) {
        TurnoutCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarColor = deterministicColor(guest.fullName)
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(guest.fullName.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(guest.fullName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text(guest.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                StatusBadge(status = guest.rsvpStatus)
                if (guest.rsvpStatus == "PENDING") {
                    IconButton(onClick = onResend) {
                        Icon(Icons.Default.Send, contentDescription = "Resend invitation", tint = AccentBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestDetailSheet(guest: Guest, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val avatarColor = deterministicColor(guest.fullName)
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(guest.fullName.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
            Text(guest.fullName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            StatusBadge(status = guest.rsvpStatus)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(guest.email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            guest.phone?.let { phone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(phone, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}
