package com.turnout.android.presentation.guests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.Result
import com.turnout.android.domain.model.Guest
import com.turnout.android.domain.usecase.DeleteGuestUseCase
import com.turnout.android.domain.usecase.GetGuestsParams
import com.turnout.android.domain.usecase.GetGuestsUseCase
import com.turnout.android.domain.usecase.ResendInvitationUseCase
import com.turnout.android.domain.usecase.SendInvitationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuestListUiState(
    val guests: List<Guest> = emptyList(),
    val filter: String? = null, // null = ALL
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null
)

private const val PAGE_SIZE = 20

@OptIn(FlowPreview::class)
@HiltViewModel
class GuestListViewModel @Inject constructor(
    private val getGuestsUseCase: GetGuestsUseCase,
    private val deleteGuestUseCase: DeleteGuestUseCase,
    private val resendInvitationUseCase: ResendInvitationUseCase,
    private val sendInvitationsUseCase: SendInvitationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuestListUiState())
    val uiState = _uiState.asStateFlow()

    private var currentEventId: Long = 0L
    private var currentPage = 0

    // Raw query updates immediately for the text field itself; the debounced version
    // (below) is what actually triggers a network reload, so fast typing doesn't fire
    // a request per keystroke.
    private val _rawSearchQuery = MutableStateFlow("")

    init {
        _rawSearchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                _uiState.value = _uiState.value.copy(searchQuery = query)
                reload()
            }
            .launchIn(viewModelScope)
    }

    fun initialize(eventId: Long) {
        if (currentEventId == eventId && _uiState.value.guests.isNotEmpty()) return
        currentEventId = eventId
        reload()
    }

    fun setSearchQuery(query: String) {
        _rawSearchQuery.value = query
    }

    fun setFilter(status: String?) {
        _uiState.value = _uiState.value.copy(filter = status)
        reload()
    }

    private fun reload() = viewModelScope.launch {
        currentPage = 0
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val result = getGuestsUseCase(
            GetGuestsParams(currentEventId, page = 0, search = _uiState.value.searchQuery, status = _uiState.value.filter)
        )
        when (result) {
            is Result.Success -> _uiState.value = _uiState.value.copy(
                isLoading = false,
                guests = result.data.guests,
                hasMorePages = currentPage < result.data.totalPages - 1
            )
            is Result.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
        }
    }

    fun loadNextPageIfNeeded() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMorePages) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val nextPage = currentPage + 1
            val result = getGuestsUseCase(
                GetGuestsParams(currentEventId, page = nextPage, search = state.searchQuery, status = state.filter)
            )
            when (result) {
                is Result.Success -> {
                    currentPage = nextPage
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        guests = _uiState.value.guests + result.data.guests,
                        hasMorePages = currentPage < result.data.totalPages - 1
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun deleteGuest(guestId: Long) = viewModelScope.launch {
        // Optimistic removal, same pattern as EventsListViewModel — instant feedback,
        // rolled back if the network call actually fails.
        val previousGuests = _uiState.value.guests
        _uiState.value = _uiState.value.copy(guests = previousGuests.filter { it.id != guestId })

        when (val result = deleteGuestUseCase(guestId)) {
            is Result.Success -> Unit
            is Result.Error -> _uiState.value = _uiState.value.copy(guests = previousGuests, error = result.message)
        }
    }

    fun resendInvitation(guestId: Long) = viewModelScope.launch {
        when (val result = resendInvitationUseCase(guestId)) {
            is Result.Success -> Unit
            is Result.Error -> _uiState.value = _uiState.value.copy(error = result.message)
        }
    }

    fun sendAllInvitations() = viewModelScope.launch {
        when (val result = sendInvitationsUseCase(currentEventId)) {
            is Result.Success -> reload()
            is Result.Error -> _uiState.value = _uiState.value.copy(error = result.message)
        }
    }
}
