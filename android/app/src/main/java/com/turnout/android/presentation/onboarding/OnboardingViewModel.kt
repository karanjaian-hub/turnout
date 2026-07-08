package com.turnout.android.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OnboardingEvent {
    // Fires immediately on init if onboarding was already completed previously —
    // the screen never actually renders in this case, just redirects straight through.
    data object SkipToDashboard : OnboardingEvent()
    data object NavigateToDashboard : OnboardingEvent()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _events = MutableSharedFlow<OnboardingEvent>(replay = 0)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            // .first() rather than collecting the Flow continuously — we only need
            // the current value once, at startup, not to keep observing it afterward.
            val alreadyCompleted = userPreferences.onboardingCompleted.first()
            if (alreadyCompleted) {
                _events.emit(OnboardingEvent.SkipToDashboard)
            }
        }
    }

    fun completeOnboarding() = viewModelScope.launch {
        userPreferences.setOnboardingCompleted(true)
        _events.emit(OnboardingEvent.NavigateToDashboard)
    }
}
