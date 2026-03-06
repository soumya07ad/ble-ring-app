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
import com.fitness.app.ui.components.AnimatedHeartWaveform
import com.fitness.app.ui.components.AnimatedRadialChart
import com.fitness.app.ui.components.NeonGlassCard
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM RING DATA CARDS — Neon Glass + Animated Metrics
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun BatteryCard(batteryLevel: Int?, isCharging: Boolean = false) {
    val battery = batteryLevel ?: 0
    val batteryColor = when {
        isCharging -> NeonGreen
        battery > 50 -> NeonGreen
        battery > 20 -> NeonOrange
        else -> ErrorRed
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCharging) 600 else 1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    NeonGlassCard(glowColor = batteryColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Battery radial gauge
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedRadialChart(
                    modifier = Modifier.fillMaxSize(),
                    progress = battery / 100f,
                    gradientColors = listOf(batteryColor, batteryColor.copy(alpha = 0.6f)),
                    strokeWidth = 6f,
                    glowRadius = 4f
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = batteryColor.copy(alpha = if (battery < 20 || isCharging) pulseAlpha else 1f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BATTERY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isCharging && (batteryLevel == null || battery == 0) -> "⚡"
                        isCharging -> "$battery% ⚡"
                        batteryLevel != null -> "$battery%"
                        else -> "—"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        isCharging -> "Charging"
                        batteryLevel == null -> "Unknown"
                        battery > 50 -> "Good"
                        battery > 20 -> "Low"
                        else -> "Critical"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = batteryColor
                )
            }
        }
    }
}

@Composable
fun HeartRateCard(heartRate: Int?, isMeasuring: Boolean = false) {
    val hr = heartRate ?: 0

    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isMeasuring) 400 else 800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    NeonGlassCard(glowColor = ErrorRed) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Heart icon with pulse
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size((48 * if (hr > 0 || isMeasuring) heartScale else 1f).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ErrorRed.copy(alpha = 0.2f),
                                    ErrorRed.copy(alpha = 0.03f)
                                )
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HEART RATE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = when {
                            isMeasuring && hr == 0 -> "..."
                            heartRate != null && hr > 0 -> "$hr"
                            else -> "—"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (hr > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "bpm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }

                // Live waveform
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedHeartWaveform(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    color = ErrorRed,
                    isActive = hr > 0 || isMeasuring
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isMeasuring -> "Measuring..."
                        heartRate == null || hr == 0 -> "Tap to measure"
                        hr < 60 -> "Low"
                        hr in 60..100 -> "Normal"
                        else -> "Elevated"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isMeasuring -> NeonOrange
                        heartRate == null || hr == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        hr < 60 -> NeonCyan
                        hr in 60..100 -> NeonGreen
                        else -> NeonOrange
                    }
                )
            }
        }
    }
}

@Composable
fun StepsCard(steps: Int?) {
    val stepCount = steps ?: 0
    val goal = 10000
    val progress = (stepCount.toFloat() / goal).coerceIn(0f, 1f)

    NeonGlassCard(glowColor = PrimaryPurple) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radial progress
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedRadialChart(
                    modifier = Modifier.fillMaxSize(),
                    progress = progress,
                    gradientColors = listOf(PrimaryPurple, NeonPink),
                    strokeWidth = 6f,
                    glowRadius = 4f
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STEPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (steps != null) "$stepCount" else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                // Progress bar
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PrimaryPurple, NeonPink)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progress * 100).toInt()}% of goal",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progress >= 1f) NeonGreen else PrimaryPurple
                )
            }
        }
    }
}

@Composable
fun SpO2Card(spO2: Int?) {
    val oxygen = spO2 ?: 0

    NeonGlassCard(glowColor = NeonCyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedRadialChart(
                    modifier = Modifier.fillMaxSize(),
                    progress = if (oxygen > 0) oxygen / 100f else 0f,
                    gradientColors = listOf(NeonCyan, NeonBlue),
                    strokeWidth = 6f,
                    glowRadius = 4f
                )
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BLOOD OXYGEN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (spO2 != null && oxygen > 0) "$oxygen" else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (oxygen > 0) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        spO2 == null || oxygen == 0 -> "Measuring..."
                        oxygen >= 95 -> "Healthy"
                        oxygen >= 90 -> "Low"
                        else -> "Critical"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        spO2 == null || oxygen == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        oxygen >= 95 -> NeonGreen
                        oxygen >= 90 -> NeonOrange
                        else -> ErrorRed
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════

@Preview(name = "Battery Card", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun BatteryCardPreview() {
    FitnessAppTheme(darkTheme = true) {
        BatteryCard(batteryLevel = 62, isCharging = false)
    }
}

@Preview(name = "Battery Charging", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun BatteryChargingPreview() {
    FitnessAppTheme(darkTheme = true) {
        BatteryCard(batteryLevel = 85, isCharging = true)
    }
}

@Preview(name = "Heart Rate Card", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun HeartRateCardPreview() {
    FitnessAppTheme(darkTheme = true) {
        HeartRateCard(heartRate = 72, isMeasuring = false)
    }
}

@Preview(name = "Steps Card", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun StepsCardPreview() {
    FitnessAppTheme(darkTheme = true) {
        StepsCard(steps = 8432)
    }
}

@Preview(name = "SpO2 Card", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun SpO2CardPreview() {
    FitnessAppTheme(darkTheme = true) {
        SpO2Card(spO2 = 98)
    }
}
