package com.fitness.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitness.app.ble.MeasurementTimer
import com.fitness.app.ui.theme.FitnessAppTheme

/**
 * UI component showing 30-second measurement countdown timer.
 * Delegates to MeasurementTimerContent for previewable rendering.
 */
@Composable
fun MeasurementTimerUI(
    timer: MeasurementTimer,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (timer.isActive) {
        MeasurementTimerContent(
            title = "Measuring ${timer.measurementType.displayName}",
            remainingSeconds = timer.remainingSeconds,
            progress = timer.progressPercent,
            onCancel = onCancel,
            modifier = modifier
        )
    }
}

/**
 * Pure-UI timer content — accepts primitives, fully previewable.
 */
@Composable
fun MeasurementTimerContent(
    title: String,
    remainingSeconds: Int,
    progress: Float,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Circular Progress with Countdown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$remainingSeconds",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "seconds",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Linear Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructions
            Text(
                text = "Please keep still and relaxed...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Cancel Button
            FilledTonalButton(
                onClick = onCancel,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Cancel Measurement")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════

@Preview(name = "Timer Active", showBackground = true)
@Composable
private fun MeasurementTimerPreview() {
    FitnessAppTheme {
        MeasurementTimerContent(
            title = "Measuring Heart Rate",
            remainingSeconds = 18,
            progress = 0.6f
        )
    }
}

@Preview(name = "Timer Starting", showBackground = true)
@Composable
private fun MeasurementTimerStartPreview() {
    FitnessAppTheme {
        MeasurementTimerContent(
            title = "Measuring SpO2",
            remainingSeconds = 30,
            progress = 0f
        )
    }
}
