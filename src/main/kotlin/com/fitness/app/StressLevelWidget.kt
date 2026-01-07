package com.fitness.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StressLevelWidget(stressLevel: State<Int>) {
    val level = stressLevel.value
    
    // Color based on stress level
    val levelColor = when {
        level < 30 -> Color(0xFF22C55E)  // Green - Low
        level < 60 -> Color(0xFFF97316)  // Orange - Medium
        else -> Color(0xFFEF4444)        // Red - High
    }
    
    val levelLabel = when {
        level < 30 -> "Low"
        level < 60 -> "Medium"
        else -> "High"
    }
    
    val levelDescription = when {
        level < 30 -> "You're relaxed"
        level < 60 -> "Moderate stress"
        else -> "High stress - take a break"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Stress Level",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF9CA3AF)
                )
            }

            // Gauge and Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Gauge
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFFF3F4F6), shape = CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner circle for gradient effect
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(
                                color = levelColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "$level",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = levelColor
                            )
                            Text(
                                "/100",
                                fontSize = 10.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }

                // Status Text
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(levelColor, shape = CircleShape)
                            )
                            Text(
                                levelLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = levelColor
                            )
                        }
                        Text(
                            levelDescription,
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    Text(
                        "Based on: Heart rate variability & activity",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFE5E7EB), shape = RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(level / 100f)
                        .background(levelColor)
                )
            }

            // Tips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF2F2), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    when {
                        level < 30 -> "💚 Keep up your healthy routine!"
                        level < 60 -> "🟠 Try a 10-minute meditation or walk"
                        else -> "🔴 Take a break and practice deep breathing"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563)
                )
            }
        }
    }
}

// For preview and testing
@Composable
fun StressLevelWidgetPreview() {
    val mockStressState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(65) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7FF))
            .padding(16.dp)
    ) {
        StressLevelWidget(mockStressState)
    }
}
