package com.fitness.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.domain.model.RingConnectionState
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// SMART RING CARD — Connection management on Dashboard
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SmartRingCard(
    connectionState: RingConnectionState,
    ringName: String = "Smart Ring",
    batteryLevel: Int? = null,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState == RingConnectionState.CONNECTED

    // Animated glow color
    val glowColor by animateColorAsState(
        targetValue = if (isConnected) NeonGreen else NeonCyan,
        animationSpec = tween(600),
        label = "ringCardGlow"
    )

    // Subtle pulse for connected state
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    NeonGlassCard(
        modifier = modifier,
        glowColor = glowColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ring icon with status glow
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = if (isConnected) 0.25f * pulseAlpha else 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Inner icon circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = 0.2f),
                                    glowColor.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Ring",
                        tint = glowColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ringName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) NeonGreen.copy(alpha = pulseAlpha)
                                else TextMuted
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected)
                            "Connected${batteryLevel?.let { " • $it%" } ?: ""}"
                        else
                            "Not connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) NeonGreen else TextSecondary
                    )
                }
            }

            // Action button
            val buttonShape = RoundedCornerShape(14.dp)

            if (isConnected) {
                // Disconnect button
                OutlinedButton(
                    onClick = onDisconnectClick,
                    shape = buttonShape,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(ErrorRed.copy(alpha = 0.6f), ErrorRed.copy(alpha = 0.3f))
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRed
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ErrorRed
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Disconnect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                // Connect button with gradient
                Button(
                    onClick = onConnectClick,
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(PrimaryPurple, NeonPurple)
                                ),
                                shape = buttonShape
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Connect Ring",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
