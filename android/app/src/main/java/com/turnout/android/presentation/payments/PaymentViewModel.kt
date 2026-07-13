package com.turnout.android.presentation.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turnout.android.core.utils.Result
import com.turnout.android.data.local.UserPreferences
import com.turnout.android.domain.model.Subscription
import com.turnout.android.domain.model.SubscriptionPlan
import com.turnout.android.domain.model.Transaction
import com.turnout.android.domain.usecase.CreateStripeSessionUseCase
import com.turnout.android.domain.usecase.GetCurrentSubscriptionUseCase
import com.turnout.android.domain.usecase.GetPlansUseCase
import com.turnout.android.domain.usecase.GetTransactionsUseCase
import com.turnout.android.domain.usecase.InitiateMpesaParams
import com.turnout.android.domain.usecase.InitiateMpesaUseCase
import com.turnout.android.domain.usecase.RequestEnterpriseParams
import com.turnout.android.domain.usecase.RequestEnterpriseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MpesaFlowState {
    data object Idle : MpesaFlowState()
    data class Waiting(val secondsRemaining: Int) : MpesaFlowState()
    data object Success : MpesaFlowState()
    data object Timeout : MpesaFlowState()
}

sealed class PaymentEvent {
    data class OpenStripeCheckout(val url: String) : PaymentEvent()
    data class ShowSnackbar(val message: String) : PaymentEvent()
}

data class PaymentUiState(
    val subscription: Subscription? = null,
    val plans: List<SubscriptionPlan> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val mpesaFlowState: MpesaFlowState = MpesaFlowState.Idle,
    val savedPhoneNumber: String = ""
)

private const val MPESA_TIMEOUT_SECONDS = 60
private const val POLL_INTERVAL_MS = 3_000L

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val getPlansUseCase: GetPlansUseCase,
    private val getCurrentSubscriptionUseCase: GetCurrentSubscriptionUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val initiateMpesaUseCase: InitiateMpesaUseCase,
    private val createStripeSessionUseCase: CreateStripeSessionUseCase,
    private val requestEnterpriseUseCase: RequestEnterpriseUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PaymentEvent>(replay = 0)
    val events = _events.asSharedFlow()

    private var pollingJob: Job? = null
    private var countdownJob: Job? = null

    init {
        loadAll()
        viewModelScope.launch {
            val savedPhone = userPreferences.mpesaPhoneNumber.first() ?: ""
            _uiState.value = _uiState.value.copy(savedPhoneNumber = savedPhone)
        }
    }

    private fun loadAll() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val subscriptionResult = getCurrentSubscriptionUseCase()
        val plansResult = getPlansUseCase()
        val transactionsResult = getTransactionsUseCase(0)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            subscription = (subscriptionResult as? Result.Success)?.data,
            plans = (plansResult as? Result.Success)?.data ?: emptyList(),
            transactions = (transactionsResult as? Result.Success)?.data?.transactions ?: emptyList()
        )
    }

    fun refreshSubscription() = viewModelScope.launch {
        when (val result = getCurrentSubscriptionUseCase()) {
            is Result.Success -> _uiState.value = _uiState.value.copy(subscription = result.data)
            is Result.Error -> Unit
        }
    }

    fun initiateMpesa(phoneNumber: String, planId: String, savePhone: Boolean) {
        if (savePhone) {
            viewModelScope.launch { userPreferences.saveMpesaPhoneNumber(phoneNumber) }
        }

        viewModelScope.launch {
            when (initiateMpesaUseCase(InitiateMpesaParams(phoneNumber, planId))) {
                is Result.Success -> startPollingForPlanChange()
                is Result.Error -> _events.emit(PaymentEvent.ShowSnackbar("Could not start M-Pesa payment. Please try again."))
            }
        }
    }

    private fun startPollingForPlanChange() {
        val originalPlan = _uiState.value.subscription?.plan
        _uiState.value = _uiState.value.copy(mpesaFlowState = MpesaFlowState.Waiting(MPESA_TIMEOUT_SECONDS))

        // Two independent timers running concurrently: the countdown is purely visual
        // (ticks every 1s), while polling actually checks the backend (every 3s) — they
        // don't need to be in lockstep, just both cancelled together when either resolves.
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = MPESA_TIMEOUT_SECONDS
            while (remaining > 0) {
                delay(1_000)
                remaining--
                val current = _uiState.value.mpesaFlowState
                if (current is MpesaFlowState.Waiting) {
                    _uiState.value = _uiState.value.copy(mpesaFlowState = MpesaFlowState.Waiting(remaining))
                }
            }
        }

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val deadline = System.currentTimeMillis() + (MPESA_TIMEOUT_SECONDS * 1000L)
            while (System.currentTimeMillis() < deadline) {
                delay(POLL_INTERVAL_MS)
                when (val result = getCurrentSubscriptionUseCase()) {
                    is Result.Success -> {
                        if (result.data.plan != originalPlan) {
                            _uiState.value = _uiState.value.copy(
                                subscription = result.data,
                                mpesaFlowState = MpesaFlowState.Success
                            )
                            countdownJob?.cancel()
                            return@launch
                        }
                    }
                    is Result.Error -> Unit // transient poll failure — keep trying until deadline
                }
            }
            // Deadline reached with no plan change detected.
            _uiState.value = _uiState.value.copy(mpesaFlowState = MpesaFlowState.Timeout)
            countdownJob?.cancel()
        }
    }

    fun cancelMpesaFlow() {
        pollingJob?.cancel()
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(mpesaFlowState = MpesaFlowState.Idle)
    }

    fun createStripeSession(planId: String) = viewModelScope.launch {
        when (val result = createStripeSessionUseCase(planId)) {
            is Result.Success -> _events.emit(PaymentEvent.OpenStripeCheckout(result.data))
            is Result.Error -> _events.emit(PaymentEvent.ShowSnackbar("Could not start Stripe checkout. Please try again."))
        }
    }

    fun requestEnterprise(companyName: String, contactEmail: String, notes: String) = viewModelScope.launch {
        when (val result = requestEnterpriseUseCase(RequestEnterpriseParams(companyName, contactEmail, notes))) {
            is Result.Success -> _events.emit(PaymentEvent.ShowSnackbar("Request submitted — we'll be in touch soon."))
            is Result.Error -> _events.emit(PaymentEvent.ShowSnackbar(result.message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        countdownJob?.cancel()
    }
}
