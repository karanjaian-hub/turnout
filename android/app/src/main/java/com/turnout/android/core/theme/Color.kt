package com.turnout.android.core.theme

import androidx.compose.ui.graphics.Color

// ── Brand palette ─────────────────────────────────────────────────────────────
val Navy        = Color(0xFF1E3A5F)
val Blue        = Color(0xFF2563EB)
val BlueDark    = Color(0xFF1D4ED8)
val BlueLight   = Color(0xFFEFF6FF)

// ── Semantic colours ──────────────────────────────────────────────────────────
val SuccessGreen  = Color(0xFF16A34A)
val WarningAmber  = Color(0xFFD97706)
val ErrorRed      = Color(0xFFDC2626)
val Purple        = Color(0xFF7C3AED)   // WAITLISTED status

// ── Light surface ─────────────────────────────────────────────────────────────
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceWhite    = Color(0xFFFFFFFF)
val TextPrimary     = Color(0xFF0F172A)
val TextSecondary   = Color(0xFF64748B)
val BorderColor     = Color(0xFFE2E8F0)

// ── Dark surface ──────────────────────────────────────────────────────────────
val DarkBackground = Color(0xFF0F172A)
val DarkSurface    = Color(0xFF1E293B)
val DarkBorder     = Color(0xFF334155)

// ── 2.3 design system tokens ─────────────────────────────────────────────────
// These are additive, not replacements — existing screens (Login, TurnoutTextField,
// etc.) already import Navy/Blue/ErrorRed/etc. by their current names. Renaming those
// would break every screen using them. New code going forward should prefer these
// names where they overlap conceptually with an old one (e.g. AccentBlue vs Blue).
val Canvas                 = Color(0xFF0B1422)
val CanvasSurface           = Color(0xFF1E293B)
val NavyPrimary             = Color(0xFF1E3A5F)
val AccentBlue              = Color(0xFF2563EB)
val AccentGlow              = Color(0x803B82F6)
val SignalGreen             = Color(0xFF16A34A)
val DangerRed               = Color(0xFFDC2626)
val InfoPurple              = Color(0xFF7C3AED)
val SurfaceLight            = Color(0xFFF8FAFC)
val TextOnCanvas            = Color(0xFFE2E8F0)
val TextOnCanvasSecondary   = Color(0xFF94A3B8)
