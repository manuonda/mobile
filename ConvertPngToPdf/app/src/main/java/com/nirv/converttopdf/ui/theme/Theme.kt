package com.nirv.converttopdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Plazo Design System – Color Tokens ───────────────────────────────────────

// Primary — lima/yellow-green (CTA buttons, checkboxes, nav selected)
val PlazoLime        = Color(0xFFCDD94A)
val PlazoLimeDark    = Color(0xFFB8C43A)   // pressed / dark variant

// Accent — lavanda/lila (action icon circles, secondary elements)
val PlazoLilac       = Color(0xFFC9B8F0)
val PlazoLilacDark   = Color(0xFFAA96D8)

// Background / Surface
val PlazoBackground  = Color(0xFFEDEDED)   // app background — gray visible (Plazo style)
val PlazoSurface     = Color(0xFFFFFFFF)   // cards & bars — white

// Text
val PlazoText        = Color(0xFF1C1C1C)   // primary text — near-black
val PlazoMuted       = Color(0xFF9E9E9E)   // secondary / placeholder text
val PlazoOlive       = Color(0xFF6B7D1A)   // dates / metadata — olive green

// Borders / dividers
val PlazoOutline     = Color(0xFFE8E8E8)

// Keep for backwards compatibility in previews
val UberGray400      = PlazoMuted

// ── Light scheme ─────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary             = PlazoLime,
    onPrimary           = PlazoText,          // dark text on lime buttons
    primaryContainer    = Color(0xFFEAF29A),  // light lime tint for chips
    onPrimaryContainer  = PlazoText,

    secondary           = PlazoLilac,
    onSecondary         = PlazoText,
    secondaryContainer  = Color(0xFFEDE5FF),
    onSecondaryContainer = PlazoText,

    background          = PlazoBackground,
    onBackground        = PlazoText,

    surface             = PlazoSurface,
    onSurface           = PlazoText,
    surfaceVariant      = PlazoBackground,    // slightly darker surface for inputs
    onSurfaceVariant    = PlazoMuted,

    outline             = PlazoOutline,
    error               = Color(0xFFE53935),
    onError             = Color(0xFFFFFFFF)
)

// ── Dark scheme ───────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = PlazoLime,
    onPrimary           = PlazoText,
    primaryContainer    = Color(0xFF4A5010),
    onPrimaryContainer  = PlazoLime,

    secondary           = PlazoLilacDark,
    onSecondary         = PlazoText,
    secondaryContainer  = Color(0xFF3A2F60),
    onSecondaryContainer = PlazoLilac,

    background          = Color(0xFF141414),
    onBackground        = Color(0xFFF0F0F0),

    surface             = Color(0xFF1E1E1E),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF2A2A2A),
    onSurfaceVariant    = PlazoMuted,

    outline             = Color(0xFF3A3A3A),
    error               = Color(0xFFEF5350),
    onError             = Color(0xFF1C1C1C)
)

@Composable
fun ConvertPngToPdfTheme(
    darkTheme: Boolean = false,   // Plazo is always light — ignore system dark mode
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content     = content
    )
}
