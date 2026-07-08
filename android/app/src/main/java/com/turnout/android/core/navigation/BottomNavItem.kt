package com.turnout.android.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Home",     Icons.Default.House),
    BottomNavItem(Screen.Events,    "Events",   Icons.Default.CalendarMonth),
    BottomNavItem(Screen.Ai,        "AI",       Icons.Default.AutoAwesome),
    BottomNavItem(Screen.Settings,  "Settings", Icons.Default.Settings)
)
