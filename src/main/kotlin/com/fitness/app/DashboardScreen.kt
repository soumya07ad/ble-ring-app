package com.fitness.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreenWithHeader(stressLevel: Int, pairedRing: SmartRing?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Header with stress and device info
        DashboardHeader(stressLevel = stressLevel, pairedRing = pairedRing)
        
        // Scrollable dashboard content
        DashboardContent()
    }
}

@Composable
fun DashboardHeader(stressLevel: Int, pairedRing: SmartRing?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFFFF))
            .padding(16.dp)
    ) {
        // Stress Level and Device Info Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Stress Level Widget (left side)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                color = getStressColor(stressLevel).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, getStressColor(stressLevel))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Stress Level",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "$stressLevel/100",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = getStressColor(stressLevel)
                    )
                    Text(
                        text = getStressStatus(stressLevel),
                        fontSize = 11.sp,
                        color = getStressColor(stressLevel)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Device Info (right side)
            if (pairedRing != null) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Connected",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = pairedRing.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "🔋 ${pairedRing.batteryLevel}%",
                            fontSize = 12.sp,
                            color = Color(0xFF22C55E),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Divider
        Divider(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun DashboardContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome heading
        Text(
            text = "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Battery Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF22C55E),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("🔋 BATTERY LEVEL", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                    Text("87%", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Device - Good Status", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
                
                // Battery circle
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF16A34A))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("87%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Steps and Calories Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "STEPS",
                value = "${MockData.steps}",
                progress = 10,
                subtitle = "of 10k goal",
                gradient = listOf(Color(0xFFA855F7), Color(0xFFEC4899))
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "CALORIES",
                value = "${MockData.calories}",
                progress = (MockData.calories.toFloat() / 750 * 100).toInt(),
                subtitle = "of 750 goal",
                gradient = listOf(Color(0xFF3B82F6), Color(0xFF22C55E))
            )
        }

        // Heart Rate and Blood Pressure
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VitalCard(
                modifier = Modifier.weight(1f),
                emoji = "❤️",
                title = "HEART RATE",
                value = "${MockData.heartRate}",
                unit = "bpm",
                status = "Normal",
                gradient = listOf(Color(0xFFFF6B6B), Color(0xFFFF9C81))
            )
            VitalCard(
                modifier = Modifier.weight(1f),
                emoji = "🩸",
                title = "BLOOD PRESSURE",
                value = "120/80",
                unit = "mmHg",
                status = "Normal",
                gradient = listOf(Color(0xFFFB923C), Color(0xFFF97316))
            )
        }

        // Daily Summary
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Daily Summary",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                SummaryItem("🏃 Active Time", "${MockData.workoutMinutes} mins")
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                SummaryItem("💧 Water Intake", "6/8 cups")
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                SummaryItem("🛌 Sleep Last Night", "${MockData.sleepHours}h")
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

fun getStressColor(level: Int): Color = when {
    level < 30 -> Color(0xFF22C55E)  // Green
    level < 60 -> Color(0xFFFB923C)  // Orange
    else -> Color(0xFFEF4444)        // Red
}

fun getStressStatus(level: Int): String = when {
    level < 30 -> "Low"
    level < 60 -> "Medium"
    else -> "High"
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    progress: Int,
    subtitle: String,
    gradient: List<Color>
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = gradient[0],
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            // Progress bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = Color.White.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress / 100f)
                        .background(Color.White, RoundedCornerShape(2.dp))
                ) {}
            }
            
            Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun VitalCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    value: String,
    unit: String,
    status: String,
    gradient: List<Color>
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = gradient[0],
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(emoji, fontSize = 32.sp)
                Column {
                    Text(title, fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                    Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text("$unit - $status", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF4B5563))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
    }
}

// Keep old DashboardScreen for compatibility
@Composable
fun DashboardScreen() {
    DashboardContent()
}
