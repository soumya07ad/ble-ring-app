package com.fitness.app.presentation.dashboard.screens

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fitness.app.MockData
import com.fitness.app.PreviewData
import com.fitness.app.presentation.dashboard.DashboardUiState
import com.fitness.app.presentation.dashboard.DashboardViewModel
import com.fitness.app.presentation.dashboard.SmartRingViewModel
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.RingConnectionState
import com.fitness.app.ui.components.*
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM DASHBOARD — Cinematic Silicon Valley Health-Tech
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = viewModel(),
    smartRingViewModel: SmartRingViewModel = viewModel(),
    navController: NavController? = null
) {
    val state by viewModel.uiState.collectAsState()
    val ringConnectionState by smartRingViewModel.connectionState.collectAsState()

    DashboardScreenWithHeader(
        state = state,
        ringConnectionState = ringConnectionState,
        onConnectClick = {
            navController?.navigate("ringSetup")
        },
        onDisconnectClick = {
            smartRingViewModel.disconnectRing()
        }
    )
}

@Composable
fun DashboardScreenWithHeader(
    state: DashboardUiState,
    ringConnectionState: RingConnectionState = if (state.isConnected) RingConnectionState.CONNECTED else RingConnectionState.DISCONNECTED,
    onConnectClick: () -> Unit = {},
    onDisconnectClick: () -> Unit = {}
) {
    val stressLevel = state.stressLevel.coerceIn(0, 100)
    val pairedRing = state.connectedRing
    val isConnected = ringConnectionState == RingConnectionState.CONNECTED

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {

            // Hero Section
            HeroDashboardHeader(
                pairedRing = pairedRing,
                isConnected = isConnected,
                batteryLevel = state.batteryLevel,
                ringConnectionState = ringConnectionState,
                onConnectClick = onConnectClick,
                onDisconnectClick = onDisconnectClick
            )

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
                        text = if (isConnected) "Live" else "Offline",
                        color = if (isConnected) NeonGreen else TextMuted
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
                        value = if (!isConnected) "--" else if (state.heartRate > 0) "${state.heartRate}" else "--",
                        unit = "bpm",
                        progress = if (isConnected) (state.heartRate / 200f).coerceIn(0f, 1f) else 0f,
                        gradientColors = listOf(ErrorRed, NeonPink),
                        glowColor = ErrorRed
                    )
                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FavoriteBorder,
                        label = "BLOOD O₂",
                        value = if (!isConnected) "--" else if (state.spO2 > 0) "${state.spO2.toInt()}" else "--",
                        unit = "%",
                        progress = if (isConnected) (state.spO2 / 100f).coerceIn(0f, 1f) else 0f,
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
                        value = if (isConnected) "${state.steps}" else "0",
                        unit = "",
                        progress = if (isConnected) (state.steps / 10000f).coerceIn(0f, 1f) else 0f,
                        gradientColors = listOf(PrimaryPurple, NeonPink),
                        glowColor = PrimaryPurple
                    )
                    FloatingMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Build,
                        label = "DISTANCE",
                        value = if (isConnected) "${state.distance}" else "0",
                        unit = "m",
                        progress = if (isConnected) (state.distance / 5000f).coerceIn(0f, 1f) else 0f,
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

                // Weekly Emotions Chart
                WeeklyEmotionsChart()

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Insights
                DailyInsightsCard()

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// HERO SECTION — Animated ring + status
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun HeroDashboardHeader(
    pairedRing: Ring?,
    isConnected: Boolean,
    batteryLevel: Int?,
    ringConnectionState: RingConnectionState = if (isConnected) RingConnectionState.CONNECTED else RingConnectionState.DISCONNECTED,
    onConnectClick: () -> Unit = {},
    onDisconnectClick: () -> Unit = {}
) {
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

        // Smart Ring Card with connection management
        SmartRingCard(
            connectionState = ringConnectionState,
            ringName = pairedRing?.name ?: "Smart Ring",
            batteryLevel = batteryLevel,
            onConnectClick = onConnectClick,
            onDisconnectClick = onDisconnectClick,
            modifier = Modifier.padding(horizontal = 0.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        GlowDivider(color = NeonCyan.copy(alpha = 0.5f))
    }
}


// ═══════════════════════════════════════════════════════════════════════
// WEEKLY EMOTIONS CHART — Canvas bar + line chart
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun WeeklyEmotionsChart() {
    val moodData = listOf(
        Triple("Mon", 65f, "Good"),
        Triple("Tue", 72f, "Great"),
        Triple("Wed", 58f, "Okay"),
        Triple("Thu", 85f, "Exc."),
        Triple("Fri", 78f, "Great"),
        Triple("Sat", 92f, "Exc."),
        Triple("Sun", 75f, "Great")
    )

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1400, easing = FastOutSlowInEasing)
        )
    }

    // 3D Glass card shell
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), ambientColor = NeonCyan.copy(alpha = 0.25f), spotColor = NeonCyan.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0E1A1F),  // dark teal-tinted glass
                        Color(0xFF080E12)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(NeonCyan.copy(alpha = 0.5f), NeonBlue.copy(alpha = 0.15f))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonCyan))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "WEEKLY EMOTIONS TREND",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val w = size.width
                val h = size.height
                val padBottom = 36f
                val padTop = 16f
                val chartH = h - padBottom - padTop
                val slotW = w / moodData.size
                val barW = slotW * 0.5f

                // Grid lines
                for (i in 0..4) {
                    val y = padTop + chartH * i / 4f
                    drawLine(NeonCyan.copy(alpha = 0.1f), Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                }

                val points = mutableListOf<Offset>()
                moodData.forEachIndexed { i, (_, value, _) ->
                    val x = slotW * i + slotW / 2f
                    val barH = chartH * (value / 100f) * animProgress.value
                    val top = padTop + chartH - barH
                    val bottom = padTop + chartH

                    // Bar with rounded top
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.7f), NeonBlue.copy(alpha = 0.3f)),
                            startY = top, endY = bottom
                        ),
                        topLeft = Offset(x - barW / 2f, top),
                        size = androidx.compose.ui.geometry.Size(barW, barH)
                    )

                    val dotY = padTop + chartH * (1f - value / 100f * animProgress.value)
                    points.add(Offset(x, dotY))
                }

                // Trend line
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (j in 1 until points.size) {
                            val cx = (points[j - 1].x + points[j].x) / 2f
                            cubicTo(cx, points[j - 1].y, cx, points[j].y, points[j].x, points[j].y)
                        }
                    }
                    drawPath(path, NeonCyan, style = Stroke(width = 3f, cap = StrokeCap.Round))
                }

                // Dots
                points.forEach { pt ->
                    drawCircle(NeonCyan, radius = 6f, center = pt)
                    drawCircle(Color(0xFF000000), radius = 3f, center = pt)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day labels with mood %
            Row(modifier = Modifier.fillMaxWidth()) {
                moodData.forEach { (day, value, _) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${value.toInt()}%",
                            fontSize = 10.sp,
                            color = NeonCyan.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonCyan))
                    Text(text = "Mood Level", fontSize = 12.sp, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier
                        .height(3.dp)
                        .width(24.dp)
                        .background(Brush.horizontalGradient(listOf(NeonCyan, NeonBlue)))
                    )
                    Text(text = "Trend", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun DailyInsightsCard() {
    data class Insight(val emoji: String, val label: String, val value: String, val color: Color)
    val insights = listOf(
        Insight("⚡", "Active Time",     "45 mins",  NeonCyan),
        Insight("🔥", "Calories Burned",  "524 kcal", NeonOrange),
        Insight("🧘", "Meditation",      "12 mins",  PrimaryPurple),
        Insight("👟", "Steps Taken",     "8,432",    NeonGreen),
        Insight("❤️", "Heart Rate Avg",  "72 bpm",   NeonPink)
    )

    // 3D Glass card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), ambientColor = PrimaryPurple.copy(alpha = 0.25f), spotColor = PrimaryPurple.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF120D1E),  // dark purple-tinted glass
                        Color(0xFF0A0812)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(PrimaryPurple.copy(alpha = 0.5f), NeonPink.copy(alpha = 0.15f))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PrimaryPurple))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DAILY INSIGHTS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            insights.forEachIndexed { idx, insight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(insight.color)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = insight.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = insight.label,
                        fontSize = 15.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = insight.value,
                        fontSize = 16.sp,
                        color = insight.color,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (idx < insights.lastIndex) {
                    HorizontalDivider(
                        color = GlassBorder,
                        thickness = 0.5.dp
                    )
                }
            }
        }
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
// DashboardRoute is already defined above at line 41

// Legacy compatibility
@Composable
fun DashboardScreen() {
    DashboardScreenWithHeader(
        state = DashboardUiState(),
        ringConnectionState = RingConnectionState.DISCONNECTED
    )
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS — Multi-state, ViewModel-free
// ═══════════════════════════════════════════════════════════════════════

@Preview(name = "Connected", showBackground = true, backgroundColor = 0xFF050508, device = Devices.PIXEL_6)
@Composable
private fun DashboardConnectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedDashboardState)
    }
}

@Preview(name = "Disconnected", showBackground = true, backgroundColor = 0xFF050508, device = Devices.PIXEL_6)
@Composable
private fun DashboardDisconnectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.disconnectedDashboardState)
    }
}

@Preview(name = "High Stress", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DashboardHighStressPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.highStressDashboardState)
    }
}

@Preview(name = "Low Battery", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DashboardLowBatteryPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.lowBatteryDashboardState)
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DashboardDarkModePreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedDashboardState)
    }
}

@Preview(name = "Tablet", showBackground = true, backgroundColor = 0xFF050508, device = Devices.TABLET)
@Composable
private fun DashboardTabletPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedDashboardState)
    }
}

@Preview(name = "Landscape", showBackground = true, backgroundColor = 0xFF050508, widthDp = 800, heightDp = 400)
@Composable
private fun DashboardLandscapePreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.connectedDashboardState)
    }
}

@Preview(name = "Loading", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DashboardLoadingPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(state = PreviewData.loadingDashboardState)
    }
}
