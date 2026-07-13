package com.turnout.android.core.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Shared between DashboardScreen (event cards, avatar initials) and EventsListScreen
 * (event card gradient headers) — same visual language, one source of truth so a future
 * tweak to the color scheme doesn't need to happen in two places and risk drifting.
 */
fun deterministicColor(seed: String): Color {
    val hash = abs(seed.hashCode())
    val hue = (hash % 360).toFloat()
    return Color.hsv(hue, 0.55f, 0.75f)
}

fun deterministicGradient(seed: Long): Pair<Color, Color> {
    val hash = abs(seed.hashCode())
    val hue = (hash % 360).toFloat()
    val colorA = Color.hsv(hue, 0.6f, 0.55f)
    val colorB = Color.hsv((hue + 40f) % 360f, 0.6f, 0.7f)
    return colorA to colorB
}
