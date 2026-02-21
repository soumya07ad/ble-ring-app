package com.fitness.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitness.app.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════
// ANIMATED 3D RING — Rotating ring with neon glow
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedRing3D(
    modifier: Modifier = Modifier,
    primaryColor: Color = NeonCyan,
    secondaryColor: Color = PrimaryPurple,
    isConnected: Boolean = false,
    isScanning: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring3d")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val tilt by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnected) 1200 else 2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(
        modifier = modifier.size(180.dp)
    ) {
        val cx = size.width / 2
        val cy = size.height / 2
        val ringRadius = size.width * 0.35f
        val ringThickness = size.width * 0.06f

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.15f * glowPulse),
                    primaryColor.copy(alpha = 0.05f * glowPulse),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = ringRadius * 1.8f
            ),
            radius = ringRadius * 1.8f,
            center = Offset(cx, cy)
        )

        // Ring shadow (depth illusion)
        drawArc(
            color = Color.Black.copy(alpha = 0.3f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(cx - ringRadius + 3, cy - ringRadius * 0.45f + 5),
            size = Size(ringRadius * 2, ringRadius * 0.9f),
            style = Stroke(width = ringThickness + 4, cap = StrokeCap.Round)
        )

        // Main ring (3D tilt via oval)
        val tiltFactor = 0.4f + 0.1f * sin(Math.toRadians(tilt.toDouble())).toFloat()
        val ovalHeight = ringRadius * 2 * tiltFactor

        // Ring gradient segments
        val sweepSegments = 12
        val segmentAngle = 360f / sweepSegments
        for (i in 0 until sweepSegments) {
            val angle = i * segmentAngle + rotation
            val progress = i.toFloat() / sweepSegments
            val segColor = lerp(primaryColor, secondaryColor, progress)

            drawArc(
                color = segColor,
                startAngle = angle,
                sweepAngle = segmentAngle + 2,
                useCenter = false,
                topLeft = Offset(cx - ringRadius, cy - ovalHeight / 2),
                size = Size(ringRadius * 2, ovalHeight),
                style = Stroke(width = ringThickness, cap = StrokeCap.Round)
            )
        }

        // Highlight streak (light reflection)
        val highlightAngle = rotation * 1.5f
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.6f),
                    Color.Transparent
                )
            ),
            startAngle = highlightAngle,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(cx - ringRadius, cy - ovalHeight / 2),
            size = Size(ringRadius * 2, ovalHeight),
            style = Stroke(width = ringThickness - 2, cap = StrokeCap.Round)
        )

        // Inner shine
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(cx, cy)
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(cx - ringRadius + ringThickness, cy - ovalHeight / 2 + ringThickness / 2),
            size = Size(ringRadius * 2 - ringThickness * 2, ovalHeight - ringThickness),
            style = Stroke(width = 2f)
        )
    }
}

private fun lerp(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    )
}

// ═══════════════════════════════════════════════════════════════════════
// ENERGY PULSE WAVE — Expanding concentric circles (scanning)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun EnergyPulseWave(
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    pulseCount: Int = 3,
    isActive: Boolean = true
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulses = (0 until pulseCount).map { index ->
        val delay = index * 600
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, delayMillis = delay, easing = EaseOut),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse_$index"
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val maxRadius = size.minDimension * 0.45f

        pulses.forEach { pulse ->
            val radius = maxRadius * pulse.value
            val alpha = (1f - pulse.value) * 0.5f

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PARTICLE EXPLOSION — Success celebration
// ═══════════════════════════════════════════════════════════════════════

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val life: Float = 1f
)

@Composable
fun ParticleExplosion(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    particleCount: Int = 30,
    colors: List<Color> = listOf(NeonCyan, NeonGreen, PrimaryPurple, NeonPink)
) {
    if (!isActive) return

    val progress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(1500, easing = EaseOut),
        label = "explosion"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2

        val random = Random(42)
        repeat(particleCount) { i ->
            val angle = (i.toFloat() / particleCount) * 360f + random.nextFloat() * 30f
            val rad = Math.toRadians(angle.toDouble())
            val speed = 80f + random.nextFloat() * 120f
            val dist = speed * progress

            val px = cx + cos(rad).toFloat() * dist
            val py = cy + sin(rad).toFloat() * dist
            val pSize = (3f + random.nextFloat() * 4f) * (1f - progress * 0.7f)
            val alpha = (1f - progress).coerceIn(0f, 1f)

            drawCircle(
                color = colors[i % colors.size].copy(alpha = alpha),
                radius = pSize,
                center = Offset(px, py)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ANIMATED HEART WAVEFORM — Live ECG-style sine wave
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedHeartWaveform(
    modifier: Modifier = Modifier,
    color: Color = ErrorRed,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.height(40.dp)) {
        val w = size.width
        val h = size.height
        val midY = h / 2

        if (!isActive) {
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 2f
            )
            return@Canvas
        }

        val path = Path()
        path.moveTo(0f, midY)

        val points = 100
        for (i in 0..points) {
            val x = (i.toFloat() / points) * w
            val normalX = i.toFloat() / points

            // ECG-like waveform
            val y = midY + when {
                normalX in 0.15f..0.2f -> -h * 0.15f * sin((normalX - 0.15f) / 0.05f * PI.toFloat())
                normalX in 0.25f..0.3f -> -h * 0.4f * sin((normalX - 0.25f) / 0.05f * PI.toFloat())
                normalX in 0.35f..0.4f -> h * 0.12f * sin((normalX - 0.35f) / 0.05f * PI.toFloat())
                normalX in 0.55f..0.6f -> -h * 0.08f * sin((normalX - 0.55f) / 0.05f * PI.toFloat())
                normalX in 0.7f..0.75f -> -h * 0.35f * sin((normalX - 0.7f) / 0.05f * PI.toFloat())
                normalX in 0.8f..0.85f -> h * 0.1f * sin((normalX - 0.8f) / 0.05f * PI.toFloat())
                else -> sin(normalX * 20f + phase) * h * 0.02f
            }

            path.lineTo(x, y)
        }

        // Glow
        drawPath(
            path = path,
            color = color.copy(alpha = 0.2f),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Main line
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ANIMATED COUNTER — Number count-up animation
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun animateIntAsState(
    targetValue: Int,
    durationMillis: Int = 800
): State<Int> {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis, easing = EaseOutCubic)
        )
    }

    return remember {
        derivedStateOf { animatable.value.toInt() }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ANIMATED RADIAL CHART — Circular progress with gradient fill + glow
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedRadialChart(
    modifier: Modifier = Modifier,
    progress: Float,
    gradientColors: List<Color> = listOf(NeonCyan, NeonBlue),
    trackColor: Color = Color.White.copy(alpha = 0.06f),
    strokeWidth: Float = 10f,
    glowRadius: Float = 8f
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "radial"
    )

    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val radius = diameter / 2
        val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        // Track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress glow
        if (animatedProgress > 0f) {
            drawArc(
                brush = Brush.sweepGradient(gradientColors),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth + glowRadius, cap = StrokeCap.Round),
                alpha = 0.3f
            )

            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(gradientColors),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ANIMATED GRADIENT BORDER — Rotating gradient border for cards
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun animatedGradientBorderBrush(
    colors: List<Color> = listOf(NeonCyan, PrimaryPurple, NeonPink, NeonCyan),
    durationMillis: Int = 3000
): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "gradBorder")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderAngle"
    )

    return Brush.sweepGradient(
        colors = colors,
        center = Offset.Unspecified
    )
}

// ═══════════════════════════════════════════════════════════════════════
// CINEMATIC BACKGROUND — Animated gradient with light streaks
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun CinematicBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset"
    )

    val streakAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streak"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base ultra-dark gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF050508),
                    Color(0xFF08080E),
                    Color(0xFF050508)
                ),
                startY = h * gradientOffset * 0.3f,
                endY = h
            )
        )

        // Subtle cyan accent glow (top-right)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.03f),
                    Color.Transparent
                ),
                center = Offset(w * 0.8f, h * 0.15f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.8f, h * 0.15f)
        )

        // Purple accent glow (bottom-left)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PrimaryPurple.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(w * 0.2f, h * 0.85f),
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.2f, h * 0.85f)
        )

        // Light streak (diagonal)
        val streakY = h * (0.3f + gradientOffset * 0.4f)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = streakAlpha),
                    Color.Transparent
                )
            ),
            start = Offset(0f, streakY),
            end = Offset(w, streakY - h * 0.1f),
            strokeWidth = 1f
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════

@Preview(name = "3D Ring Connected", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun Ring3DConnectedPreview() {
    FitnessAppTheme(darkTheme = true) {
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            AnimatedRing3D(isConnected = true)
        }
    }
}

@Preview(name = "3D Ring Scanning", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun Ring3DScanningPreview() {
    FitnessAppTheme(darkTheme = true) {
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            AnimatedRing3D(isScanning = true)
        }
    }
}

@Preview(name = "Energy Pulse Wave", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun EnergyPulsePreview() {
    FitnessAppTheme(darkTheme = true) {
        Box(modifier = Modifier.size(200.dp)) {
            EnergyPulseWave(isActive = true)
        }
    }
}

@Preview(name = "Radial Chart", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun RadialChartPreview() {
    FitnessAppTheme(darkTheme = true) {
        AnimatedRadialChart(
            modifier = Modifier.size(80.dp),
            progress = 0.75f,
            gradientColors = listOf(NeonCyan, NeonBlue)
        )
    }
}

@Preview(name = "Heart Waveform", showBackground = true, backgroundColor = 0xFF050508)
@Composable
private fun HeartWaveformPreview() {
    FitnessAppTheme(darkTheme = true) {
        AnimatedHeartWaveform(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            isActive = true
        )
    }
}
