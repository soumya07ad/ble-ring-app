package com.fitness.app.presentation.wellness.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.domain.model.MeditationData
import com.fitness.app.domain.model.MeditationExercise
import com.fitness.app.presentation.wellness.MeditationTimerState
import com.fitness.app.presentation.wellness.MeditationViewModel
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// MEDITATION LIST SCREEN — Shows exercises for a given category
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun MeditationListScreen(
    category: String,
    viewModel: MeditationViewModel,
    onExerciseClick: (MeditationExercise) -> Unit,
    onBack: () -> Unit
) {
    val exercises = remember(category) { MeditationData.getByCategory(category) }
    val title = MeditationData.categoryTitle(category)
    val emoji = MeditationData.categoryEmoji(category)
    val description = MeditationData.categoryDescription(category)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    SkyBlue.copy(alpha = 0.35f),
                                    SkyBlue.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(Modifier.height(20.dp))
            }

            // Exercise cards
            items(exercises, key = { it.id }) { exercise ->
                ExerciseGlassCard(
                    exercise = exercise,
                    onClick = { onExerciseClick(exercise) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// EXERCISE GLASS CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ExerciseGlassCard(
    exercise: MeditationExercise,
    onClick: () -> Unit
) {
    val iconBgColor = when {
        exercise.category == "morning_calm" -> Color(0xFFFFF3E0)  // soft orange
        exercise.category == "breathing" -> Color(0xFFE0F7FA)     // soft cyan
        exercise.category == "sleep" -> Color(0xFFEDE7F6)          // soft purple
        else -> SkyBlue.copy(alpha = 0.1f)
    }
    val iconBgColorDark = when {
        exercise.category == "morning_calm" -> NeonOrange.copy(alpha = 0.15f)
        exercise.category == "breathing" -> NeonCyan.copy(alpha = 0.15f)
        exercise.category == "sleep" -> PrimaryPurple.copy(alpha = 0.15f)
        else -> SkyBlue.copy(alpha = 0.15f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                8.dp, RoundedCornerShape(20.dp),
                ambientColor = if (AppColors.isDark) Color.Transparent else LightGlassShadow
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (AppColors.isDark) CardGlassBrush
                else Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x0DFFFFFF)))
            )
            .border(
                1.dp,
                if (AppColors.isDark) GlassBorder else LightGlassBorderStrong,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with soft colored background
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (AppColors.isDark) iconBgColorDark else iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(exercise.emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    exercise.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${exercise.durationMinutes} min",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SkyBlue
                )
            }

            Spacer(Modifier.width(8.dp))

            // Start button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.accentGradient)
                    .clickable { onClick() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    "Start",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// MEDITATION TIMER SCREEN
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun MeditationTimerScreen(
    exerciseId: String,
    category: String,
    viewModel: MeditationViewModel,
    onBack: () -> Unit
) {
    val timerState by viewModel.timerState.collectAsState()
    val isDark = AppColors.isDark
    val trackColor = if (isDark) Color(0xFF1A1A2E) else SkyBlue.copy(alpha = 0.1f)

    // Load exercise on first composition
    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    val exercise = timerState.exercise

    // Breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    // Progress ring animation
    val animatedProgress by animateFloatAsState(
        targetValue = timerState.progress,
        animationSpec = tween(300),
        label = "progress"
    )

    // Completion color pulse
    val completionColor by animateColorAsState(
        targetValue = if (timerState.isCompleted) HighlighterGreen else SkyBlue,
        animationSpec = tween(500),
        label = "completionColor"
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        exercise?.name ?: "Meditation",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        exercise?.description ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Breathing circle + timer
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow
                if (timerState.isRunning) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size((260 * breatheScale).dp)
                    ) {
                        drawCircle(
                            color = completionColor.copy(alpha = 0.08f),
                            radius = size.minDimension / 2
                        )
                    }
                }

                // Progress ring
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(240.dp)
                ) {
                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                    // Progress arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(completionColor, completionColor.copy(alpha = 0.6f))
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                }

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        exercise?.emoji ?: "🧘",
                        fontSize = 40.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        timerState.formattedTime,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = completionColor,
                        letterSpacing = 2.sp
                    )
                    if (timerState.isCompleted) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "✅ Complete!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighlighterGreen
                        )
                    } else if (!timerState.isRunning && !timerState.isPaused) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ready to begin",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (timerState.isPaused) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Paused",
                            fontSize = 13.sp,
                            color = NeonOrange
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress percentage
            Text(
                "${(timerState.progress * 100).toInt()}% complete",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))

            // Duration Selection Grid (2×3)
            DurationSelectionGrid(
                selectedMinutes = timerState.selectedDurationMinutes,
                enabled = !timerState.isRunning && !timerState.isPaused && !timerState.isCompleted,
                onSelect = { viewModel.setDuration(it) }
            )

            Spacer(Modifier.height(28.dp))

            // Control buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (timerState.isRunning || timerState.isPaused) {
                    // Stop button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ErrorRed.copy(alpha = 0.15f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { viewModel.stopTimer() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stop, "Stop", tint = ErrorRed, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Stop", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                        }
                    }

                    // Pause / Resume
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.accentGradient)
                            .clickable {
                                if (timerState.isRunning) viewModel.pauseTimer()
                                else viewModel.resumeTimer()
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (timerState.isRunning) "Pause" else "Resume",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (timerState.isRunning) "Pause" else "Resume",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else if (timerState.isCompleted) {
                    // Done — go back
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.accentGradient)
                            .clickable { onBack() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Done",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // Start
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.accentGradient)
                            .clickable { viewModel.startTimer() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Start Meditation",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DURATION SELECTION GRID
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DurationSelectionGrid(
    selectedMinutes: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    val durations = listOf(5, 10, 15, 20, 25, 30)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2×3 grid: 3 rows of 2 buttons
        for (row in durations.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (minutes in row) {
                    DurationButton(
                        minutes = minutes,
                        isSelected = selectedMinutes == minutes,
                        enabled = enabled,
                        onClick = { onSelect(minutes) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationButton(
    minutes: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = AppColors.isDark

    val background = if (isSelected) {
        AppColors.accentGradient
    } else {
        if (isDark) CardGlassBrush
        else Brush.verticalGradient(listOf(LightGlassCard, LightGlassCard.copy(alpha = 0.4f)))
    }

    val borderColor = if (isSelected) {
        Color.Transparent
    } else {
        if (isDark) GlassBorder else LightGlassBorderStrong
    }

    val textColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .shadow(
                if (isSelected) 8.dp else 4.dp,
                RoundedCornerShape(16.dp),
                ambientColor = if (isSelected) SkyBlue.copy(alpha = 0.3f) else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .then(
                if (!isSelected) Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$minutes min",
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}
