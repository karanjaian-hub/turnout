package com.turnout.android.core.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.turnout.android.core.theme.SignalGreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.turnout.android.core.utils.TurnoutWindowSize


/** SignalGreen 8dp dot with a continuous, gentle alpha pulse — used on nav items with
 * unread live activity. A separate small composable so both the bottom bar and the
 * rail render the identical badge rather than each defining their own animation. */
@Composable
private fun PulsingNavBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "nav_badge_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nav_badge_alpha"
    )
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .graphicsLayer { this.alpha = alpha }
            .background(SignalGreen)
    )
}

@Composable
private fun NavIcon(item: AdaptiveNavItem) {
    if (item.showBadge) {
        BadgedBox(badge = { PulsingNavBadge() }) {
            Icon(item.icon, contentDescription = item.label)
        }
    } else {
        Icon(item.icon, contentDescription = item.label)
    }
}

/** One destination shown in either the bottom bar (compact) or nav rail (medium/expanded). */
data class AdaptiveNavItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
    val showBadge: Boolean = false
)

/**
 * Switches the entire navigation shell based on window size, so individual screens never
 * branch on screen size themselves — they just supply content and a nav item list once.
 *
 * Compact: standard Scaffold, items rendered as a bottom bar.
 * Medium/Expanded: Row with a NavigationRail on the left instead — bottom bars on tablets
 * waste horizontal space and look like an afterthought; a rail uses the space properly.
 *
 * Note: innerPadding from Scaffold is deliberately left unapplied here — each screen's
 * content decides how to consume it (e.g. LazyColumn contentPadding vs Modifier.padding),
 * since a single blanket application here doesn't fit every layout this wraps.
 */
@Composable
fun AdaptiveScaffold(
    windowSize: TurnoutWindowSize,
    navItems: List<AdaptiveNavItem>,
    topBar: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    when (windowSize) {
        TurnoutWindowSize.Compact -> {
            Scaffold(
                topBar = topBar,
                bottomBar = { TurnoutBottomBar(navItems) }
            ) { innerPadding ->
                content(Modifier.fillMaxSize().padding(innerPadding))
            }
        }
        TurnoutWindowSize.Medium, TurnoutWindowSize.Expanded -> {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    navItems.forEach { item ->
                        NavigationRailItem(
                            selected = item.selected,
                            onClick = item.onClick,
                            icon = { NavIcon(item) },
                            label = { Text(item.label) },
                            colors = NavigationRailItemDefaults.colors()
                        )
                    }
                }
                Scaffold(topBar = topBar) { innerPadding ->
                    content(Modifier.fillMaxSize().padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun TurnoutBottomBar(navItems: List<AdaptiveNavItem>) {
    NavigationBar {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { NavIcon(item) },
                label = { Text(item.label) }
            )
        }
    }
}
