package com.turnout.android.presentation.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turnout.android.core.components.SkeletonLoader
import com.turnout.android.core.components.TurnoutButton
import com.turnout.android.core.components.TurnoutCard
import com.turnout.android.core.components.TurnoutTopBar
import com.turnout.android.core.theme.*
import com.turnout.android.domain.model.Subscription
import com.turnout.android.domain.model.Transaction

@Composable
fun PaymentsScreen(
    onNavigateToUpgrade: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TurnoutTopBar(title = "Payments") }) { innerPadding ->
        if (uiState.isLoading) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(120.dp))
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(200.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { CurrentPlanCard(subscription = uiState.subscription, onUpgrade = onNavigateToUpgrade) }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                        TextButton(onClick = { /* TODO: full transaction history screen, not in current scope */ }) {
                            Text("See all", color = AccentBlue)
                        }
                    }
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        Text("No transactions yet", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                } else {
                    items(uiState.transactions.take(5), key = { it.id }) { transaction ->
                        TransactionRow(transaction = transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentPlanCard(subscription: Subscription?, onUpgrade: () -> Unit) {
    TurnoutCard(modifier = Modifier.fillMaxWidth()) {
        val plan = subscription?.plan ?: "FREE"
        val isFree = plan == "FREE"

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isFree) WarningAmber else SignalGreen)
            )
            Spacer(Modifier.width(8.dp))
            Text(plan, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "Up to ${subscription?.eventLimit ?: 3} events · ${subscription?.guestLimit ?: 50} guests per event",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )

        if (!isFree) {
            subscription?.expiresAt?.let { expiresAt ->
                Spacer(Modifier.height(4.dp))
                Text("Renews $expiresAt", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        if (isFree) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlue.copy(alpha = 0.08f))
                    .leftBorder(3.dp, AccentBlue)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Upgrade to PRO for unlimited events",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TurnoutButton(text = "Upgrade Now", onClick = onUpgrade)
            }
        }
    }
}

// Small helper for the 3dp AccentBlue left border on the upgrade banner — using a
// plain Modifier.border() would draw all four sides, not just the left edge.
private fun Modifier.leftBorder(width: androidx.compose.ui.unit.Dp, color: Color): Modifier = this.then(
    Modifier.drawBehind {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, size.height),
            strokeWidth = width.toPx()
        )
    }
)



@Composable
private fun TransactionRow(transaction: Transaction) {
    TurnoutCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val isMpesa = transaction.provider.equals("MPESA", ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isMpesa) SignalGreen else InfoPurple),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isMpesa) "M" else "S", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.provider, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${transaction.currency} ${transaction.amount}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = TextPrimary
                )
                Text(transaction.status, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(transaction.createdAt, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}
