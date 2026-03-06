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
import androidx.compose.ui.graphics.compositeOver
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

// ── Text Colors (Light Mode) ────────────────────────────────────────
val LightTextPrimary = Color(0xFF1A1A1A)
val LightTextSecondary = Color(0xFF6B6B6B)
val LightTextMuted = Color(0xFF94A3B8)

// ── Light Mode Backgrounds ──────────────────────────────────────────
val LightBackground = Color(0xFFD9F3FF)        // Deep sky blue
val LightSurface = Color(0xFFFFFFFF)            // Pure white cards
val LightSurfaceVariant = Color(0xFFE8F4FD)     // Soft blue tint
val LightCard = Color(0xFFFFFFFF)               // Pure white cards

// ── Light Mode Accent Colors ────────────────────────────────────────
val SkyBlue = Color(0xFF4FC3F7)
val SkyBlueDark = Color(0xFF039BE5)
val HighlighterGreen = Color(0xFF39FF14)
val SoftHighlighterGreen = Color(0xFF66FF47)    // Slightly softer green

// ── Light Mode Glass Effect Colors ───────────────────────────────────
val LightGlassCard = Color(0x59FFFFFF)          // ~35% white translucency
val LightGlassBorderStrong = Color(0x66FFFFFF)  // ~40% white border
val LightGlassShadow = Color(0x224FC3F7)        // Sky blue shadow

// ── Light Mode Glass ────────────────────────────────────────────────
val LightGlassBorder = Color(0x224FC3F7)         // 13% sky blue
val LightGlassOverlay = Color(0x0A4FC3F7)        // 4% sky blue

// ═══════════════════════════════════════════════════════════════════════
// THEME-AWARE COMPOSABLE ACCESSORS
// ═══════════════════════════════════════════════════════════════════════

object AppColors {
    val textPrimary: Color
        @Composable get() = if (MaterialTheme.colorScheme.background == DarkBackground) TextPrimary else LightTextPrimary

    val textSecondary: Color
        @Composable get() = if (MaterialTheme.colorScheme.background == DarkBackground) TextSecondary else LightTextSecondary

    val textMuted: Color
        @Composable get() = if (MaterialTheme.colorScheme.background == DarkBackground) TextMuted else LightTextMuted

    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val surface: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val cardBackground: Color
        @Composable get() = if (MaterialTheme.colorScheme.background == DarkBackground) DarkCard.copy(alpha = 0.7f) else LightCard

    val glassBorder: Color
        @Composable get() = if (MaterialTheme.colorScheme.background == DarkBackground) GlassBorder else LightGlassBorder

    val navBarBackground: Color
        @Composable get() = if (isDark) Color(0xFF080810) else Color(0xCCFFFFFF)  // 80% white glass

    val navBarBorder: Color
        @Composable get() = if (isDark) Color(0xFF1A1A2E) else LightGlassBorderStrong

    val isDark: Boolean
        @Composable get() = MaterialTheme.colorScheme.background == DarkBackground

    /** Card gradient background — dark: ultra-dark glass, light: white surface */
    val cardGradientBrush: Brush
        @Composable get() = if (isDark) CardGlassBrush else LightCardBrush

    /** Section card gradient with an accent tint */
    @Composable
    fun sectionGradient(accent: Color): Brush {
        return if (isDark) {
            Brush.verticalGradient(
                listOf(
                    accent.copy(alpha = 0.08f).compositeOver(Color(0xFF0A0A10)),
                    Color(0xFF060608)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White,
                    accent.copy(alpha = 0.06f).compositeOver(Color.White)
                )
            )
        }
    }

    /** Divider / border color */
    val dividerColor: Color
        @Composable get() = if (isDark) GlassBorder else LightGlassBorder

    /** Section border brush with accent */
    @Composable
    fun sectionBorder(accent: Color): Brush {
        return if (isDark) {
            Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.5f), accent.copy(alpha = 0.1f))
            )
        } else {
            Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.25f), accent.copy(alpha = 0.08f))
            )
        }
    }

    /** Primary accent gradient for buttons (Sky Blue → Green in light, Purple in dark) */
    val accentGradient: Brush
        @Composable get() = if (isDark) {
            Brush.horizontalGradient(listOf(PrimaryPurple, NeonPurple))
        } else {
            Brush.horizontalGradient(listOf(SkyBlue, SoftHighlighterGreen))
        }

    /** Background gradient brush for the overall screen */
    val backgroundGradient: Brush
        @Composable get() = if (isDark) {
            CinematicGradient
        } else {
            Brush.verticalGradient(
                listOf(
                    LightBackground,      // #D9F3FF deep sky blue top
                    Color(0xFFF3FFF6)      // very light neon green tint bottom
                )
            )
        }
}

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

val LightCardBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0x05000000),
        Color(0x02000000)
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
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = Color.White,

    secondary = HighlighterGreen,
    onSecondary = Color(0xFF003300),
    secondaryContainer = HighlighterGreen.copy(alpha = 0.15f),
    onSecondaryContainer = Color(0xFF003300),

    tertiary = SkyBlueDark,
    onTertiary = Color.White,
    tertiaryContainer = SkyBlue.copy(alpha = 0.12f),
    onTertiaryContainer = SkyBlueDark,

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,

    error = ErrorRed,
    onError = Color.White,

    outline = SkyBlue.copy(alpha = 0.2f),
    outlineVariant = SkyBlue.copy(alpha = 0.1f)
)

@Composable
fun FitnessAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = Color.Transparent.toArgb()
                it.navigationBarColor = if (darkTheme) DarkBackground.toArgb() else LightBackground.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
