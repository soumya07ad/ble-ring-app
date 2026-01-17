package com.fitness.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM DASHBOARD - Modern Fitness App Design
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DashboardScreenWithHeader(stressLevel: Int, pairedRing: SmartRing?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Animated gradient background
        AnimatedGradientBackground()
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            PremiumHeader(stressLevel = stressLevel, pairedRing = pairedRing)
            
            // Scrollable content
            DashboardContent()
        }
    }
}

@Composable
private fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        PrimaryPurple.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(animatedOffset, 200f),
                    radius = 600f
                )
            )
    )
}

@Composable
fun PremiumHeader(stressLevel: Int, pairedRing: SmartRing?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Stress Level Card
        GlassCard(
            modifier = Modifier.weight(1f),
            gradientColors = listOf(
                getStressColor(stressLevel).copy(alpha = 0.3f),
                getStressColor(stressLevel).copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STRESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$stressLevel",
                    style = MaterialTheme.typography.displaySmall,
                    color = getStressColor(stressLevel),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getStressStatus(stressLevel),
                    style = MaterialTheme.typography.labelMedium,
                    color = getStressColor(stressLevel)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Device Status Card
        if (pairedRing != null) {
            GlassCard(
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    SuccessGreen.copy(alpha = 0.3f),
                    SuccessGreen.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pairedRing.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "🔋 ${pairedRing.batteryLevel?.let { "$it%" } ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Welcome Section
        Text(
            text = "Welcome Back 👋",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Battery Card - Premium Design
        PremiumBatteryCard(battery = MockData.calories / 5) // Using mock data
        
        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,  // Using Star instead of DirectionsWalk
                label = "STEPS",
                value = "${MockData.steps}",
                subtitle = "of 10k goal",
                progress = MockData.steps.toFloat() / 10000f,
                gradientColors = listOf(PrimaryPurple, AccentPink)
            )
            PremiumMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Add,
                label = "CALORIES",
                value = "${MockData.calories}",
                subtitle = "of 750 goal",
                progress = MockData.calories.toFloat() / 750f,
                gradientColors = listOf(AccentOrange, WarningAmber)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Vitals Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VitalCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Favorite,
                label = "HEART RATE",
                value = "${MockData.heartRate}",
                unit = "bpm",
                status = "Normal",
                color = ErrorRed
            )
            VitalCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FavoriteBorder,  // Using FavoriteBorder instead of WaterDrop
                label = "BLOOD O2",
                value = "98",
                unit = "%",
                status = "Healthy",
                color = AccentCyan
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Daily Summary
        DailySummaryCard()
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM COMPONENTS
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(GlassWhite, GlassWhite),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(gradientColors),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        content()
    }
}

@Composable
fun PremiumBatteryCard(battery: Int) {
    val clampedBattery = battery.coerceIn(0, 100)
    val batteryColor = when {
        clampedBattery > 50 -> SuccessGreen
        clampedBattery > 20 -> WarningAmber
        else -> ErrorRed
    }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        gradientColors = listOf(
            batteryColor.copy(alpha = 0.2f),
            batteryColor.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🔋 BATTERY",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$clampedBattery%",
                    style = MaterialTheme.typography.displayLarge,
                    color = batteryColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (clampedBattery > 50) "Good Status" else "Charge Soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            // Battery circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                batteryColor.copy(alpha = 0.3f),
                                batteryColor.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(3.dp, batteryColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔋",
                    fontSize = 40.sp,
                )
            }
        }
    }
}

@Composable
fun PremiumMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    progress: Float,
    gradientColors: List<Color>
) {
    GlassCard(
        modifier = modifier,
        gradientColors = listOf(
            gradientColors[0].copy(alpha = 0.2f),
            gradientColors[1].copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = gradientColors[0],
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
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
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(gradientColors)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun VitalCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    status: String,
    color: Color
) {
    GlassCard(
        modifier = modifier,
        gradientColors = listOf(
            color.copy(alpha = 0.2f),
            color.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun DailySummaryCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        gradientColors = listOf(GlassWhite, GlassWhite)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Daily Summary",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SummaryRow(icon = "🏃", label = "Active Time", value = "${MockData.workoutMinutes} mins")
            HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 12.dp))
            SummaryRow(icon = "💧", label = "Water Intake", value = "6/8 cups")
            HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 12.dp))
            SummaryRow(icon = "🛌", label = "Sleep", value = "${MockData.sleepHours}h")
        }
    }
}

@Composable
private fun SummaryRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = AccentCyan
        )
    }
}

fun getStressColor(level: Int): Color = when {
    level < 30 -> SuccessGreen
    level < 60 -> WarningAmber
    else -> ErrorRed
}

fun getStressStatus(level: Int): String = when {
    level < 30 -> "Low"
    level < 60 -> "Medium"
    else -> "High"
}

// Keep old function for compatibility
@Composable
fun DashboardScreen() {
    DashboardContent()
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS - View in Android Studio
// ═══════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400, heightDp = 800)
@Composable
private fun DashboardPreview() {
    FitnessAppTheme(darkTheme = true) {
        DashboardScreenWithHeader(
            stressLevel = 35,
            pairedRing = SmartRing(
                name = "R9 Smart Ring",
                macAddress = "D8:36:03:02:07:87",
                batteryLevel = 85
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400)
@Composable
private fun BatteryCardPreview() {
    FitnessAppTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            PremiumBatteryCard(battery = 75)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400)
@Composable
private fun MetricCardsPreview() {
    FitnessAppTheme(darkTheme = true) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                label = "STEPS",
                value = "8,432",
                subtitle = "of 10k goal",
                progress = 0.84f,
                gradientColors = listOf(PrimaryPurple, AccentPink)
            )
            PremiumMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Add,
                label = "CALORIES",
                value = "520",
                subtitle = "of 750 goal",
                progress = 0.7f,
                gradientColors = listOf(AccentOrange, WarningAmber)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400)
@Composable
private fun VitalCardsPreview() {
    FitnessAppTheme(darkTheme = true) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VitalCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Favorite,
                label = "HEART RATE",
                value = "72",
                unit = "bpm",
                status = "Normal",
                color = ErrorRed
            )
            VitalCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FavoriteBorder,
                label = "BLOOD O2",
                value = "98",
                unit = "%",
                status = "Healthy",
                color = AccentCyan
            )
        }
    }
}
