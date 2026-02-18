package com.fitness.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════════════
// CINEMATIC DARK COLOR SCHEME — Silicon Valley Health-Tech
// ═══════════════════════════════════════════════════════════════════════

// ── Ultra-dark backgrounds ──────────────────────────────────────────
val DarkBackground = Color(0xFF050508)        // Near-black
val DarkSurface = Color(0xFF0A0A10)           // Slightly lifted
val DarkSurfaceVariant = Color(0xFF111118)    // Card base
val DarkCard = Color(0xFF0D0D14)              // Floating card bg

// ── Primary: Electric Purple ────────────────────────────────────────
val PrimaryPurple = Color(0xFF8B5CF6)
val PrimaryPurpleLight = Color(0xFFA78BFA)
val PrimaryPurpleDark = Color(0xFF7C3AED)

// ── Neon Accents ────────────────────────────────────────────────────
val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFFBF5AF2)
val NeonPink = Color(0xFFFF2D78)
val NeonGreen = Color(0xFF30D158)
val NeonBlue = Color(0xFF0A84FF)
val NeonOrange = Color(0xFFFF9F0A)

// ── Original accent names (kept for backward compatibility) ─────────
val AccentCyan = NeonCyan
val AccentPink = NeonPink
val AccentOrange = NeonOrange
val AccentBlue = NeonBlue

// ── Status Colors ───────────────────────────────────────────────────
val SuccessGreen = Color(0xFF30D158)
val WarningAmber = Color(0xFFFFD60A)
val ErrorRed = Color(0xFFFF453A)

// ── Glass Effect Colors ─────────────────────────────────────────────
val GlassWhite = Color(0x0DFFFFFF)           // 5% white
val GlassBorder = Color(0x1AFFFFFF)          // 10% white
val GlassHighlight = Color(0x26FFFFFF)       // 15% white
val GlassOverlay = Color(0x33FFFFFF)         // 20% white

// ── Depth & Glow ────────────────────────────────────────────────────
val DepthShadow = Color(0xFF000000)
val NeonGlow = Color(0x4D00F0FF)             // 30% cyan glow
val PurpleGlow = Color(0x4D8B5CF6)           // 30% purple glow
val PinkGlow = Color(0x4DFF2D78)             // 30% pink glow

// ── Text Colors (Dark Mode) ─────────────────────────────────────────
val TextPrimary = Color(0xFFF5F5F7)          // Apple-style white
val TextSecondary = Color(0xFF8E8E93)        // iOS secondary
val TextMuted = Color(0xFF48484A)            // iOS tertiary

// ═══════════════════════════════════════════════════════════════════════
// GRADIENT & BRUSH HELPERS
// ═══════════════════════════════════════════════════════════════════════

val CinematicGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF050508),
        Color(0xFF080810),
        Color(0xFF0A0A14),
        Color(0xFF050508)
    )
)

val NeonCyanGradient = Brush.horizontalGradient(
    colors = listOf(NeonCyan, NeonBlue)
)

val NeonPurpleGradient = Brush.horizontalGradient(
    colors = listOf(PrimaryPurple, NeonPink)
)

val NeonGreenGradient = Brush.horizontalGradient(
    colors = listOf(NeonGreen, NeonCyan)
)

val CardGlassBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0x0DFFFFFF),
        Color(0x05FFFFFF)
    )
)

fun neonEdgeGlow(color: Color = NeonCyan) = Brush.radialGradient(
    colors = listOf(
        color.copy(alpha = 0.4f),
        color.copy(alpha = 0.15f),
        Color.Transparent
    )
)

// ═══════════════════════════════════════════════════════════════════════
// MATERIAL COLOR SCHEME
// ═══════════════════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurpleDark,
    onPrimaryContainer = Color.White,

    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = NeonCyan.copy(alpha = 0.15f),
    onSecondaryContainer = NeonCyan,

    tertiary = NeonPink,
    onTertiary = Color.White,
    tertiaryContainer = NeonPink.copy(alpha = 0.15f),
    onTertiaryContainer = NeonPink,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = Color.White,

    outline = GlassBorder,
    outlineVariant = Color(0xFF1C1C1E)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
)

@Composable
fun FitnessAppTheme(
    darkTheme: Boolean = true,  // Always dark for cinematic look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = Color.Transparent.toArgb()
                it.navigationBarColor = DarkBackground.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
