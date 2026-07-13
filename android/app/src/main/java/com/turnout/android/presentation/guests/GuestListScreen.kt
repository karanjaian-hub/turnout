package com.turnout.android.presentation.guests

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.EmptyState
import com.turnout.android.core.components.TurnoutTopBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import com.turnout.android.domain.usecase.GetEventUseCase
import com.turnout.android.core.utils.Result
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Standalone route needs the event's status for GuestListContent's contextual empty
// states, but doesn't have it the way EventDetailContent does (already loaded there).
// This tiny ViewModel exists solely to fetch that one field before rendering the real content.
@HiltViewModel
class GuestListScreenViewModel @Inject constructor(
    private val getEventUseCase: GetEventUseCase
) : ViewModel() {
    private val _eventStatus = MutableStateFlow<String?>(null)
    val eventStatus = _eventStatus.asStateFlow()

    fun loadStatus(eventId: Long) = viewModelScope.launch {
        when (val result = getEventUseCase(eventId)) {
            is Result.Success -> _eventStatus.value = result.data.status
            is Result.Error -> _eventStatus.value = "ACTIVE" // fallback — non-fatal, just affects which empty-state copy shows
        }
    }
}

@Composable
fun GuestListScreen(
    eventId: Long,
    onNavigateBack: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    viewModel: GuestListScreenViewModel = hiltViewModel()
) {
    val eventStatus by viewModel.eventStatus.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) { viewModel.loadStatus(eventId) }

    Scaffold(topBar = { TurnoutTopBar(title = "Guests", onNavigateBack = onNavigateBack) }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val status = eventStatus
            if (status == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                GuestListContent(eventId = eventId, eventStatus = status, onNavigateToImport = onNavigateToImport)
            }
        }
    }
}
