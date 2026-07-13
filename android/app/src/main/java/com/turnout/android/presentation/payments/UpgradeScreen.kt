package com.turnout.android.presentation.payments

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.PulseLine
import com.turnout.android.core.components.TurnoutButton
import com.turnout.android.core.components.TurnoutCard
import com.turnout.android.core.components.TurnoutTextField
import com.turnout.android.core.components.TurnoutTopBar
import com.turnout.android.core.components.ButtonVariant
import com.turnout.android.core.theme.*
import com.turnout.android.domain.model.SubscriptionPlan
import kotlinx.coroutines.flow.collectLatest

@Composable
fun UpgradeScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPlanForPayment by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var showEnterpriseSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PaymentEvent.OpenStripeCheckout -> {
                    val customTabsIntent = CustomTabsIntent.Builder().build()
                    customTabsIntent.launchUrl(context, Uri.parse(event.url))
                }
                is PaymentEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = { TurnoutTopBar(title = "Upgrade", onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.plans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        currentPlanName = uiState.subscription?.plan,
                        onSelect = { selectedPlanForPayment = plan }
                    )
                }

                item {
                    TurnoutCard(modifier = Modifier.fillMaxWidth(), onClick = { showEnterpriseSheet = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = InfoPurple)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Need something bigger?", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Contact us for Enterprise pricing", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }
        }
    }

    selectedPlanForPayment?.let { plan ->
        PaymentMethodBottomSheet(
            plan = plan,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = {
                selectedPlanForPayment = null
                viewModel.cancelMpesaFlow()
            }
        )
    }

    if (showEnterpriseSheet) {
        EnterpriseSheet(
            onDismiss = { showEnterpriseSheet = false },
            onSubmit = { company, email, notes -> viewModel.requestEnterprise(company, email, notes) }
        )
    }
}

@Composable
private fun PlanCard(plan: SubscriptionPlan, currentPlanName: String?, onSelect: () -> Unit) {
    val isCurrent = plan.name.equals(currentPlanName, ignoreCase = true)

    TurnoutCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (isCurrent) null else onSelect
    ) {
        if (plan.isRecommended) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(AccentBlue.copy(alpha = 0.1f)).padding(6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("RECOMMENDED", style = MaterialTheme.typography.labelSmall, color = AccentBlue)
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(plan.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "KES ${plan.monthlyPriceKes.toInt()}",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SpaceGroteskFontFamily),
                color = TextPrimary
            )
            Text(" /mo", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Text("or \$${plan.monthlyPriceUsd} USD", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        Spacer(Modifier.height(12.dp))

        plan.features.forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(feature, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isCurrent) {
            TurnoutButton(text = "Current Plan", onClick = {}, enabled = false, variant = ButtonVariant.OUTLINE, modifier = Modifier.fillMaxWidth())
        } else {
            TurnoutButton(text = "Choose ${plan.name}", onClick = onSelect, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodBottomSheet(
    plan: SubscriptionPlan,
    uiState: PaymentUiState,
    viewModel: PaymentViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            when (val flowState = uiState.mpesaFlowState) {
                is MpesaFlowState.Idle -> PaymentMethodChoice(
                    plan = plan,
                    savedPhoneNumber = uiState.savedPhoneNumber,
                    onPayWithMpesa = { phone, save -> viewModel.initiateMpesa(phone, plan.id, save) },
                    onPayWithStripe = { viewModel.createStripeSession(plan.id) }
                )
                is MpesaFlowState.Waiting -> MpesaWaitingView(secondsRemaining = flowState.secondsRemaining, onCancel = { viewModel.cancelMpesaFlow() })
                is MpesaFlowState.Success -> MpesaSuccessView(onDone = onDismiss)
                is MpesaFlowState.Timeout -> MpesaTimeoutView(
                    onRetry = { viewModel.cancelMpesaFlow() },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodChoice(
    plan: SubscriptionPlan,
    savedPhoneNumber: String,
    onPayWithMpesa: (String, Boolean) -> Unit,
    onPayWithStripe: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf(savedPhoneNumber) }
    var savePhone by remember { mutableStateOf(true) }
    var showPhoneEntry by remember { mutableStateOf(false) }

    if (!showPhoneEntry) {
        Text("Pay for ${plan.name}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        TurnoutCard(modifier = Modifier.fillMaxWidth(), onClick = { showPhoneEntry = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(SignalGreen), contentAlignment = Alignment.Center) {
                    Text("M", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                Text("M-Pesa", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        TurnoutCard(modifier = Modifier.fillMaxWidth(), onClick = onPayWithStripe) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(InfoPurple), contentAlignment = Alignment.Center) {
                    Text("S", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                Text("Card via Stripe", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
        }
    } else {
        Text("M-Pesa Payment", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        TurnoutTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = "Phone number (e.g. 254712345678)")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Checkbox(checked = savePhone, onCheckedChange = { savePhone = it })
            Text("Save this number for next time", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.height(16.dp))
        TurnoutButton(
            text = "Send STK Push",
            onClick = { onPayWithMpesa(phoneNumber, savePhone) },
            enabled = phoneNumber.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MpesaWaitingView(secondsRemaining: Int, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = SignalGreen)
        Spacer(Modifier.height(16.dp))
        Text("Check your phone", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text("Enter your M-Pesa PIN to complete payment", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(
            "${secondsRemaining}s remaining",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        TurnoutButton(text = "Cancel", onClick = onCancel, variant = ButtonVariant.OUTLINE)
    }
}

@Composable
private fun MpesaSuccessView(onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Payment successful!", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text("Your plan has been upgraded", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        TurnoutButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MpesaTimeoutView(onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Didn't receive confirmation", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text("If you completed the payment, it may take a moment to reflect. Otherwise, try again.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TurnoutButton(text = "Close", onClick = onDismiss, variant = ButtonVariant.OUTLINE, modifier = Modifier.weight(1f))
            TurnoutButton(text = "Try Again", onClick = onRetry, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnterpriseSheet(onDismiss: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var companyName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Enterprise Inquiry", style = MaterialTheme.typography.titleLarge)
            TurnoutTextField(value = companyName, onValueChange = { companyName = it }, label = "Company name")
            TurnoutTextField(value = email, onValueChange = { email = it }, label = "Contact email")
            TurnoutTextField(value = notes, onValueChange = { notes = it }, label = "What do you need?", singleLine = false)
            TurnoutButton(
                text = "Submit",
                onClick = { onSubmit(companyName, email, notes); onDismiss() },
                enabled = companyName.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
