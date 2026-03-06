package com.fitness.app.presentation.ring.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import com.fitness.app.PreviewData
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.SignalQuality
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM DEVICE CARD — Neon Glass Design with Magnetic Press
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DeviceCard(
    ring: Ring,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.96f
            isSelected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val shape = RoundedCornerShape(20.dp)

    val borderBrush = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(
                NeonCyan.copy(alpha = 0.6f),
                PrimaryPurple.copy(alpha = 0.3f),
                NeonCyan.copy(alpha = 0.1f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(AppColors.dividerColor, AppColors.dividerColor)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clip(shape)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                brush = borderBrush,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = shape,
        color = if (isSelected) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .background(CardGlassBrush)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bluetooth Icon with neon glow
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isSelected)
                                listOf(NeonCyan.copy(alpha = 0.25f), NeonCyan.copy(alpha = 0.05f))
                            else
                                listOf(PrimaryPurple.copy(alpha = 0.15f), PrimaryPurple.copy(alpha = 0.03f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Device",
                    tint = if (isSelected) NeonCyan else PrimaryPurple,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Device Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ring.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ring.macAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Signal Strength
            PremiumSignalIndicator(
                quality = ring.signalQuality,
                rssi = ring.rssi
            )

            // Selection glow dot
            if (isSelected) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonCyan, NeonCyan.copy(alpha = 0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(14.dp)
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
        SignalQuality.EXCELLENT -> NeonGreen
        SignalQuality.GOOD -> NeonGreen.copy(alpha = 0.8f)
        SignalQuality.FAIR -> NeonOrange
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
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(4) { index ->
                val barHeight = 5.dp + (index * 4).dp
                val isActive = index < barCount

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) color
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${rssi} dBm",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════

@Preview(name = "Selected", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DeviceCardSelectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        DeviceCard(
            ring = PreviewData.mockDevices[0],
            isSelected = true,
            onClick = {}
        )
    }
}

@Preview(name = "Unselected", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DeviceCardUnselectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        DeviceCard(
            ring = PreviewData.mockDevices[1],
            isSelected = false,
            onClick = {}
        )
    }
}

@Preview(name = "Weak Signal", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun DeviceCardWeakSignalPreview() {
    FitnessAppTheme(darkTheme = true) {
        DeviceCard(
            ring = PreviewData.mockDevices[2],
            isSelected = false,
            onClick = {}
        )
    }
}
