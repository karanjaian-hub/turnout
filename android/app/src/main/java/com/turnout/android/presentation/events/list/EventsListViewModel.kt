package com.turnout.android.presentation.events.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Event
import com.turnout.android.domain.usecase.DeleteEventUseCase
import com.turnout.android.domain.usecase.GetEventsParams
import com.turnout.android.domain.usecase.GetEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsListUiState(
    val events: List<Event> = emptyList(),
    val filter: String? = null, // null = ALL
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedEventId: Long? = null // used only on Expanded two-pane layout
) {
    val filteredEvents: List<Event>
        get() = events
            .filter { filter == null || it.status == filter }
            .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
}

@HiltViewModel
class EventsListViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase,
    private val deleteEventUseCase: DeleteEventUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        when (val result = getEventsUseCase(GetEventsParams())) {
            is Result.Success -> _uiState.value = _uiState.value.copy(isLoading = false, events = result.data)
            is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
        }
    }

    fun refresh() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        when (val result = getEventsUseCase(GetEventsParams())) {
            is Result.Success -> _uiState.value = _uiState.value.copy(isRefreshing = false, events = result.data)
            is Result.Error -> _uiState.value = _uiState.value.copy(isRefreshing = false, error = result.message)
        }
    }

    fun setFilter(status: String?) {
        _uiState.value = _uiState.value.copy(filter = status)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectEvent(eventId: Long?) {
        _uiState.value = _uiState.value.copy(selectedEventId = eventId)
    }

    fun deleteEvent(eventId: Long) = viewModelScope.launch {
        // Optimistic removal: the event disappears from the list immediately, before
        // the network call even completes — feels instant. If the call fails, the
        // original list is restored and an error surfaces, rather than leaving the
        // UI showing a deletion that didn't actually happen server-side.
        val previousEvents = _uiState.value.events
        _uiState.value = _uiState.value.copy(events = previousEvents.filter { it.id != eventId })

        when (val result = deleteEventUseCase(eventId)) {
            is Result.Success -> Unit // already removed optimistically, nothing further to do
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(events = previousEvents, error = result.message)
            }
        }
    }
}
