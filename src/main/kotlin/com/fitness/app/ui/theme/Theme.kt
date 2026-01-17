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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM DARK COLOR SCHEME - Modern Fitness App Design
// ═══════════════════════════════════════════════════════════════════════

// Primary Colors
val PrimaryPurple = Color(0xFF8B5CF6)
val PrimaryPurpleLight = Color(0xFFA78BFA)
val PrimaryPurpleDark = Color(0xFF7C3AED)

// Accent Colors
val AccentCyan = Color(0xFF06B6D4)
val AccentPink = Color(0xFFF472B6)
val AccentOrange = Color(0xFFFB923C)
val AccentBlue = Color(0xFF3B82F6)

// Status Colors
val SuccessGreen = Color(0xFF22C55E)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Background & Surface Colors (Dark Mode)
val DarkBackground = Color(0xFF0F172A)      // Deep Navy
val DarkSurface = Color(0xFF1E293B)         // Slate
val DarkSurfaceVariant = Color(0xFF334155)  // Light Slate
val DarkCard = Color(0xFF1E293B)

// Glass Effect Colors
val GlassWhite = Color(0x1AFFFFFF)          // 10% white
val GlassBorder = Color(0x33FFFFFF)         // 20% white

// Text Colors (Dark Mode)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurpleDark,
    onPrimaryContainer = Color.White,
    
    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = AccentCyan.copy(alpha = 0.3f),
    onSecondaryContainer = AccentCyan,
    
    tertiary = AccentPink,
    onTertiary = Color.White,
    tertiaryContainer = AccentPink.copy(alpha = 0.3f),
    onTertiaryContainer = AccentPink,
    
    background = DarkBackground,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    error = ErrorRed,
    onError = Color.White,
    
    outline = GlassBorder,
    outlineVariant = Color(0xFF475569)
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
    darkTheme: Boolean = true,  // Default to dark theme for premium look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
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

// ═══════════════════════════════════════════════════════════════════════
// THEME PREVIEWS
// ═══════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400)
@Composable
private fun ColorPalettePreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .background(DarkBackground)
                .padding(16.dp)
        ) {
            Text(
                text = "🎨 Color Palette",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Primary colors
            Text("Primary Colors", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorSwatch("Purple", PrimaryPurple)
                ColorSwatch("Light", PrimaryPurpleLight)
                ColorSwatch("Dark", PrimaryPurpleDark)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Accent colors
            Text("Accent Colors", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorSwatch("Cyan", AccentCyan)
                ColorSwatch("Pink", AccentPink)
                ColorSwatch("Orange", AccentOrange)
                ColorSwatch("Blue", AccentBlue)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status colors
            Text("Status Colors", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorSwatch("Success", SuccessGreen)
                ColorSwatch("Warning", WarningAmber)
                ColorSwatch("Error", ErrorRed)
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400)
@Composable
private fun DarkThemePreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "🌙 Dark Theme",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Surface Card", style = MaterialTheme.typography.titleMedium)
                    Text("This is how cards look in dark mode", 
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Primary Button")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun LightThemePreview() {
    FitnessAppTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "☀️ Light Theme",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Surface Card", style = MaterialTheme.typography.titleMedium)
                    Text("This is how cards look in light mode", 
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Primary Button")
            }
        }
    }
}
