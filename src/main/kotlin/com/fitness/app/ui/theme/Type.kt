package com.fitness.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Premium Typography for Fitness App
val Typography = Typography(
    // Large titles (e.g., "Welcome Back")
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    
    // Headlines (section headers)
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    
    // Titles (card headers)
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Labels (buttons, small text)
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

// ═══════════════════════════════════════════════════════════════════════
// TYPOGRAPHY PREVIEWS
// ═══════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400)
@Composable
private fun TypographyPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .background(DarkBackground)
                .padding(16.dp)
        ) {
            Text(
                text = "✨ Typography",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Display styles
            Text("Display Styles", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Display Large", style = MaterialTheme.typography.displayLarge, color = TextPrimary)
            Text("Display Medium", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
            Text("Display Small", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Headline styles
            Text("Headline Styles", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Headline Large", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Text("Headline Medium", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text("Headline Small", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400)
@Composable
private fun BodyTypographyPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .background(DarkBackground)
                .padding(16.dp)
        ) {
            // Title styles
            Text("Title Styles", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Title Large", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text("Title Medium", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("Title Small", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Body styles
            Text("Body Styles", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Body Large - Used for main content", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text("Body Medium - Used for descriptions", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text("Body Small - Used for captions", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Label styles
            Text("Label Styles", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("LABEL LARGE", style = MaterialTheme.typography.labelLarge, color = AccentCyan)
            Text("LABEL MEDIUM", style = MaterialTheme.typography.labelMedium, color = AccentCyan)
            Text("LABEL SMALL", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
        }
    }
}
