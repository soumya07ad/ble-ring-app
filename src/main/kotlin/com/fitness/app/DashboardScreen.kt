package com.fitness.app

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitness.app.presentation.ring.RingUiState
import com.fitness.app.presentation.ring.RingViewModel
import com.fitness.app.domain.model.Ring
import com.fitness.app.ui.components.*
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM DASHBOARD — Cinematic Silicon Valley Health-Tech
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DashboardRoute(
    viewModel: RingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    DashboardScreenWithHeader(state = state)
}

@Composable
fun DashboardScreenWithHeader(state: RingUiState) {
    val stressLevel = state.ringData.stress.coerceIn(0, 100)
    val pairedRing = state.connectedRing

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Section
            HeroDashboardHeader(pairedRing = pairedRing, isConnected = state.isConnected, batteryLevel = state.batteryLevel)

            Spacer(modifier = Modifier.height(24.dp))

            // Health Metrics Grid
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HEALTH METRICS",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    StatusBadge(
                        text = if (state.isConnected) "Live" else "Offline",
                        color = if (state.isConnected) NeonGreen else TextMuted
                    )
                }

                // 2-column metric grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Favorite,
                        label = "HEART RATE",
                        value = if (state.ringData.heartRate > 0) "${state.ringData.heartRate}" else "--",
                        unit = "bpm",
                        progress = (state.ringData.heartRate / 200f).coerceIn(0f, 1f),
                        gradientColors = listOf(ErrorRed, NeonPink),
                        glowColor = ErrorRed
                    )
                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FavoriteBorder,
                        label = "BLOOD O₂",
                        value = if (state.ringData.spO2 > 0) "${state.ringData.spO2.toInt()}" else "--",
                        unit = "%",
                        progress = (state.ringData.spO2 / 100f).coerceIn(0f, 1f),
                        gradientColors = listOf(NeonCyan, NeonBlue),
                        glowColor = NeonCyan
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Star,
                        label = "STEPS",
                        value = "${state.ringData.steps}",
                        unit = "",
                        progress = (state.ringData.steps / 10000f).coerceIn(0f, 1f),
                        gradientColors = listOf(PrimaryPurple, NeonPink),
                        glowColor = PrimaryPurple
                    )
                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Build,
                        label = "DISTANCE",
                        value = "${state.ringData.distance}",
                        unit = "m",
                        progress = (state.ringData.distance / 5000f).coerceIn(0f, 1f),
                        gradientColors = listOf(NeonOrange, WarningAmber),
                        glowColor = NeonOrange
                    )
                }

                // Stress Card
                NeonGlassCard(glowColor = getStressColor(stressLevel)) {
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
                                progress = stressLevel / 100f,
                                gradientColors = listOf(
                                    getStressColor(stressLevel),
                                    getStressColor(stressLevel).copy(alpha = 0.5f)
                                ),
                                strokeWidth = 6f,
                                glowRadius = 4f
                            )
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = getStressColor(stressLevel),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STRESS LEVEL",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$stressLevel",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(
                                    text = getStressStatus(stressLevel),
                                    color = getStressColor(stressLevel)
                                )
                            }
                        }
                    }
                }

                // Battery card
                state.batteryLevel?.let { battery ->
                    PremiumBatteryCard(battery = battery)
                }

                // Daily Summary
                DailySummaryCard()

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// HERO SECTION — Animated ring + status
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun HeroDashboardHeader(pairedRing: Ring?, isConnected: Boolean, batteryLevel: Int?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Small ring animation
        AnimatedRing3D(
            modifier = Modifier.size(100.dp),
            primaryColor = if (isConnected) NeonCyan else TextMuted,
            secondaryColor = if (isConnected) PrimaryPurple else TextMuted.copy(alpha = 0.5f),
            isConnected = isConnected
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = pairedRing?.name ?: "Smart Ring",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) NeonGreen else TextMuted
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected)
                    "Connected ${batteryLevel?.let { "• $it%" } ?: ""}"
                else "Not connected",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        GlowDivider(color = NeonCyan.copy(alpha = 0.5f))
    }
}



// ═══════════════════════════════════════════════════════════════════════
// PREMIUM BATTERY CARD — Circular gauge + glow
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun PremiumBatteryCard(battery: Int) {
    val batteryColor = when {
        battery > 50 -> NeonGreen
        battery > 20 -> NeonOrange
        else -> ErrorRed
    }

    NeonGlassCard(glowColor = batteryColor) {
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
                    progress = battery / 100f,
                    gradientColors = listOf(batteryColor, batteryColor.copy(alpha = 0.5f)),
                    strokeWidth = 6f,
                    glowRadius = 4f
                )
                Text(
                    text = "$battery",
                    style = MaterialTheme.typography.titleSmall,
                    color = batteryColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RING BATTERY",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$battery%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            battery > 80 -> "Fully charged"
                            battery > 50 -> "Good"
                            battery > 20 -> "Low"
                            else -> "Critical"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = batteryColor,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DAILY SUMMARY CARD — Activity overview
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DailySummaryCard() {
    NeonGlassCard(
        glowColor = PrimaryPurple,
        showGlow = false
    ) {
        Text(
            text = "DAILY SUMMARY",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        SummaryRow(icon = "🏃", label = "Distance", value = "5.2 km")
        SummaryRow(icon = "⏱️", label = "Active Time", value = "1h 23m")
        SummaryRow(icon = "🔥", label = "Avg Heart Rate", value = "68 bpm")
        SummaryRow(icon = "😴", label = "Sleep Score", value = "85%")
    }
}

@Composable
private fun SummaryRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════

fun getStressColor(level: Int): Color = when {
    level <= 30 -> NeonGreen
    level <= 60 -> NeonOrange
    else -> ErrorRed
}

fun getStressStatus(level: Int): String = when {
    level <= 30 -> "Low"
    level <= 60 -> "Moderate"
    else -> "High"
}

// ═══════════════════════════════════════════════════════════════════════
// ROUTE — ViewModel-owning wrapper (use in navigation)
// ═══════════════════════════════════════════════════════════════════════
// DashboardRoute is already defined above at line 39

// Legacy compatibility
@Composable
fun DashboardScreen() {
    DashboardScreenWithHeader(state = RingUiState())
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS — Multi-state, ViewModel-free
// ═══════════════════════════════════════════════════════════════════════

@Preview(name = "Connected", showBackground = true, backgroundColor = 0xFF050508, device = Devices.PIXEL_6)
@Composable
private fun DashboardConnectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedState)
    }
}

@Preview(name = "Disconnected", showBackground = true, backgroundColor = 0xFF050508, device = Devices.PIXEL_6)
@Composable
private fun DashboardDisconnectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.disconnectedState)
    }
}

@Preview(name = "High Stress", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DashboardHighStressPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.highStressState)
    }
}

@Preview(name = "Low Battery", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DashboardLowBatteryPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.lowBatteryState)
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DashboardDarkModePreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedState)
    }
}

@Preview(name = "Tablet", showBackground = true, backgroundColor = 0xFF050508, device = Devices.TABLET)
@Composable
private fun DashboardTabletPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedState)
    }
}

@Preview(name = "Landscape", showBackground = true, backgroundColor = 0xFF050508, widthDp = 800, heightDp = 400)
@Composable
private fun DashboardLandscapePreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedState)
    }
}

@Preview(name = "Loading", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DashboardLoadingPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.loadingState)
    }
}
