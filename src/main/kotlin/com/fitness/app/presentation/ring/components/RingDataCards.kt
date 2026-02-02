package com.fitness.app.presentation.ring.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM RING DATA CARDS - Health Metrics with Glassmorphism
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun BatteryCard(batteryLevel: Int?, isCharging: Boolean = false) {
    val battery = batteryLevel ?: 0
    val batteryColor = when {
        isCharging -> SuccessGreen  // Green when charging
        battery > 50 -> SuccessGreen
        battery > 20 -> WarningAmber
        else -> ErrorRed
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCharging) 600 else 1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    PremiumDataCard(
        icon = Icons.Default.Info,  // Battery icon
        iconTint = batteryColor,
        iconScale = if (battery < 20 || isCharging) pulseScale else 1f,
        label = "BATTERY",
        value = when {
            isCharging && (batteryLevel == null || battery == 0) -> "⚡"
            isCharging -> "$battery% ⚡"
            batteryLevel != null -> "$battery%"
            else -> "—"
        },
        status = when {
            isCharging -> "Charging"
            batteryLevel == null -> "Unknown"
            battery > 50 -> "Good"
            battery > 20 -> "Low"
            else -> "Critical"
        },
        statusColor = batteryColor,
        gradientColors = listOf(
            batteryColor.copy(alpha = 0.2f),
            batteryColor.copy(alpha = 0.05f)
        )
    )
}

@Composable
fun HeartRateCard(heartRate: Int?, isMeasuring: Boolean = false) {
    val hr = heartRate ?: 0
    
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isMeasuring) 400 else 800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )
    
    PremiumDataCard(
        icon = Icons.Default.Favorite,
        iconTint = ErrorRed,
        iconScale = if (hr > 0 || isMeasuring) heartScale else 1f,
        label = "HEART RATE",
        value = when {
            isMeasuring && hr == 0 -> "..."
            heartRate != null && hr > 0 -> "$hr bpm"
            else -> "—"
        },
        status = when {
            isMeasuring -> "Measuring..."
            heartRate == null || hr == 0 -> "Tap to measure"
            hr < 60 -> "Low"
            hr in 60..100 -> "Normal"
            else -> "Elevated"
        },
        statusColor = when {
            isMeasuring -> WarningAmber
            heartRate == null || hr == 0 -> TextSecondary
            hr < 60 -> AccentCyan
            hr in 60..100 -> SuccessGreen
            else -> WarningAmber
        },
        gradientColors = listOf(
            ErrorRed.copy(alpha = 0.2f),
            ErrorRed.copy(alpha = 0.05f)
        )
    )
}

@Composable
fun StepsCard(steps: Int?) {
    val stepCount = steps ?: 0
    val goal = 10000
    val progress = (stepCount.toFloat() / goal).coerceIn(0f, 1f)
    
    PremiumDataCard(
        icon = Icons.Default.Star,  // Using Star instead of DirectionsWalk
        iconTint = PrimaryPurple,
        label = "STEPS",
        value = if (steps != null) "$stepCount" else "—",
        status = "${(progress * 100).toInt()}% of goal",
        statusColor = if (progress >= 1f) SuccessGreen else PrimaryPurple,
        gradientColors = listOf(
            PrimaryPurple.copy(alpha = 0.2f),
            AccentPink.copy(alpha = 0.05f)
        ),
        showProgress = true,
        progress = progress,
        progressColors = listOf(PrimaryPurple, AccentPink)
    )
}

@Composable
fun SpO2Card(spO2: Int?) {
    val oxygen = spO2 ?: 0
    
    PremiumDataCard(
        icon = Icons.Default.FavoriteBorder,  // Using FavoriteBorder instead of Air
        iconTint = AccentCyan,
        label = "BLOOD OXYGEN",
        value = if (spO2 != null && oxygen > 0) "$oxygen%" else "—",
        status = when {
            spO2 == null || oxygen == 0 -> "Measuring..."
            oxygen >= 95 -> "Healthy"
            oxygen >= 90 -> "Low"
            else -> "Critical"
        },
        statusColor = when {
            spO2 == null || oxygen == 0 -> TextSecondary
            oxygen >= 95 -> SuccessGreen
            oxygen >= 90 -> WarningAmber
            else -> ErrorRed
        },
        gradientColors = listOf(
            AccentCyan.copy(alpha = 0.2f),
            AccentCyan.copy(alpha = 0.05f)
        )
    )
}

@Composable
private fun PremiumDataCard(
    icon: ImageVector,
    iconTint: Color,
    iconScale: Float = 1f,
    label: String,
    value: String,
    status: String,
    statusColor: Color,
    gradientColors: List<Color>,
    showProgress: Boolean = false,
    progress: Float = 0f,
    progressColors: List<Color> = listOf(PrimaryPurple, AccentPink)
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(gradientColors), RoundedCornerShape(20.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with glow
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                iconTint.copy(alpha = 0.3f),
                                iconTint.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size((32 * iconScale).dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                if (showProgress) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(progressColors))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 380)
@Composable
private fun BatteryCardPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            BatteryCard(batteryLevel = 85)
            Spacer(modifier = Modifier.height(12.dp))
            BatteryCard(batteryLevel = 25)
            Spacer(modifier = Modifier.height(12.dp))
            BatteryCard(batteryLevel = null)
            Spacer(modifier = Modifier.height(12.dp))
            BatteryCard(batteryLevel = 60, isCharging = true)
            Spacer(modifier = Modifier.height(12.dp))
            BatteryCard(batteryLevel = null, isCharging = true)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 380)
@Composable
private fun HeartRateCardPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            HeartRateCard(heartRate = 72)
            Spacer(modifier = Modifier.height(12.dp))
            HeartRateCard(heartRate = null)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 380)
@Composable
private fun StepsCardPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            StepsCard(steps = 8432)
            Spacer(modifier = Modifier.height(12.dp))
            StepsCard(steps = 12500)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 380)
@Composable
private fun AllCardsPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            BatteryCard(batteryLevel = 75)
            Spacer(modifier = Modifier.height(12.dp))
            HeartRateCard(heartRate = 68)
            Spacer(modifier = Modifier.height(12.dp))
            StepsCard(steps = 6543)
            Spacer(modifier = Modifier.height(12.dp))
            SpO2Card(spO2 = 98)
        }
    }
}
