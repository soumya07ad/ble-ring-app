package com.fitness.app.presentation.ring.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.SignalQuality
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM DEVICE CARD - Glassmorphism Design
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DeviceCard(
    ring: Ring,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    val borderColor = if (isSelected) PrimaryPurple else GlassBorder
    val gradientColors = if (isSelected) {
        listOf(PrimaryPurple.copy(alpha = 0.2f), PrimaryPurple.copy(alpha = 0.05f))
    } else {
        listOf(GlassWhite, GlassWhite)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(gradientColors), RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bluetooth Icon with glow
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimaryPurple.copy(alpha = 0.3f),
                                PrimaryPurple.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,  // Bluetooth device icon
                    contentDescription = "Device",
                    tint = PrimaryPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Device Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ring.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ring.macAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            // Signal Strength - use derived property
            PremiumSignalIndicator(
                quality = ring.signalQuality,
                rssi = ring.rssi
            )
            
            // Selection Indicator
            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumSignalIndicator(
    quality: SignalQuality,
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val color = when (quality) {
        SignalQuality.EXCELLENT -> SuccessGreen
        SignalQuality.GOOD -> Color(0xFF84CC16)  // Lime
        SignalQuality.FAIR -> WarningAmber
        SignalQuality.POOR -> ErrorRed
    }
    
    val barCount = when (quality) {
        SignalQuality.EXCELLENT -> 4
        SignalQuality.GOOD -> 3
        SignalQuality.FAIR -> 2
        SignalQuality.POOR -> 1
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(4) { index ->
                val barHeight = 6.dp + (index * 5).dp
                val isActive = index < barCount
                
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) color 
                            else Color.White.copy(alpha = 0.1f)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = "${rssi} dBm",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 380)
@Composable
private fun DeviceCardPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ring constructor: macAddress, name, rssi, isConnected
            DeviceCard(
                ring = Ring(
                    macAddress = "D8:36:03:02:07:87",
                    name = "R9 Smart Ring",
                    rssi = -45
                ),
                isSelected = true,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(12.dp))
            DeviceCard(
                ring = Ring(
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    name = "Unknown Device",
                    rssi = -75
                ),
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SignalIndicatorPreview() {
    FitnessAppTheme(darkTheme = true) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            PremiumSignalIndicator(SignalQuality.EXCELLENT, -40)
            PremiumSignalIndicator(SignalQuality.GOOD, -55)
            PremiumSignalIndicator(SignalQuality.FAIR, -70)
            PremiumSignalIndicator(SignalQuality.POOR, -85)
        }
    }
}
