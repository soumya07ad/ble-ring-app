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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.presentation.wellness.*
import com.fitness.app.domain.model.Emotion
import com.fitness.app.domain.model.MeditationItem
import com.fitness.app.domain.model.ActiveTimer
import com.fitness.app.ui.theme.*
import com.fitness.app.ui.components.GlowDivider

// ═══════════════════════════════════════════════════════════════════════
// WELLNESS SCREEN
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessScreen(
    viewModel: WellnessViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Header ────────────────────────────────────────────
            item {
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
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "💎  Wellness",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track your mind, body & spirit",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                GlowDivider(color = NeonCyan.copy(alpha = 0.4f))
            }

            // ── Section 1: Mental Wellness ────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "Mental Wellness",
                        dotColor = NeonPink
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "How are you feeling?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select an emotion that best describes your current state",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                EmotionGrid(
                    emotions = uiState.emotions,
                    selectedEmotion = uiState.selectedEmotion,
                    onSelect = { viewModel.selectEmotion(it) }
                )
            }

            // ── Section 2: Wellness Insights ──────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "Wellness Insights",
                        dotColor = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Two side-by-side cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InsightCard(
                            modifier = Modifier.weight(1f),
                            emoji = "😊",
                            label = "Avg Mood",
                            value = "${uiState.avgMood}%",
                            color = NeonGreen,
                            progress = uiState.avgMood / 100f
                        )
                        InsightCard(
                            modifier = Modifier.weight(1f),
                            emoji = "🧠",
                            label = "Stress Level",
                            value = "${uiState.stressLevel}%",
                            color = NeonOrange,
                            progress = uiState.stressLevel / 100f
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full-width Wellness Score
                    WellnessScoreCard(score = uiState.wellnessScore)
                }
            }

            // ── Section 3: Meditation ─────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "Meditation",
                        dotColor = PrimaryPurple
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Active Timer Card
            val activeTimer = uiState.activeTimer
            if (activeTimer != null) {
                item(key = "active_timer") {
                    ActiveTimerCard(
                        timer = activeTimer,
                        formattedTime = viewModel.formatTime(activeTimer.remainingSeconds),
                        onPause = { viewModel.pauseTimer() },
                        onResume = { viewModel.resumeTimer() },
                        onStop = { viewModel.stopTimer() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            items(uiState.meditations, key = { it.id }) { meditation ->
                val isActive = uiState.activeTimer?.meditationId == meditation.id
                MeditationCard(
                    meditation = meditation,
                    isActive = isActive,
                    onStart = { viewModel.startMeditation(meditation) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SECTION HEADER
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// EMOTION GRID
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmotionGrid(
    emotions: List<Emotion>,
    selectedEmotion: String?,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        emotions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { emotion ->
                    EmotionCard(
                        modifier = Modifier.weight(1f),
                        emotion = emotion,
                        isSelected = emotion.name == selectedEmotion,
                        onSelect = { onSelect(emotion.name) }
                    )
                }
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EmotionCard(
    modifier: Modifier = Modifier,
    emotion: Emotion,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else AppColors.dividerColor,
        animationSpec = tween(300),
        label = "emotionBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "emotionBg"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color.Transparent,
                spotColor = if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emotion.emoji, fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                emotion.name,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// INSIGHT CARDS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    color: Color,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "insightProgress"
    )

    Box(
        modifier = modifier
            .shadow(
                8.dp, RoundedCornerShape(16.dp),
                ambientColor = color.copy(alpha = 0.2f),
                spotColor = color.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.5f))))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// WELLNESS SCORE CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun WellnessScoreCard(score: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "wellnessScore"
    )

    val scoreColor = when {
        score >= 80 -> NeonGreen
        score >= 60 -> NeonCyan
        score >= 40 -> NeonOrange
        else -> ErrorRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                12.dp, RoundedCornerShape(20.dp),
                ambientColor = PrimaryPurple.copy(alpha = 0.2f),
                spotColor = PrimaryPurple.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                AppColors.sectionGradient(PrimaryPurple)
            )
            .border(
                1.5.dp,
                AppColors.sectionBorder(PrimaryPurple),
                RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Wellness Score",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "$score/100",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(PrimaryPurple, scoreColor)
                            )
                        )
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = when {
                    score >= 80 -> "Excellent! You're doing amazing."
                    score >= 60 -> "Good progress. Keep it up!"
                    score >= 40 -> "Room for improvement. Stay consistent."
                    else -> "Let's work on building better habits."
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ACTIVE TIMER CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ActiveTimerCard(
    timer: ActiveTimer,
    formattedTime: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = timer.progress,
        animationSpec = tween(300),
        label = "timerProgress"
    )

    val accentColor = if (timer.isCompleted) NeonGreen else PrimaryPurple

    // Pulsing glow when running
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                16.dp, RoundedCornerShape(24.dp),
                ambientColor = accentColor.copy(alpha = if (timer.isRunning) pulseAlpha else 0.2f),
                spotColor = accentColor.copy(alpha = if (timer.isRunning) pulseAlpha else 0.3f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                AppColors.sectionGradient(accentColor)
            )
            .border(
                1.5.dp,
                AppColors.sectionBorder(accentColor),
                RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Title
            Text(
                text = if (timer.isCompleted) "✅ Session Complete!" else "🧘 ${timer.title}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (timer.isCompleted) NeonGreen else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(20.dp))

            // Big Timer Display
            Text(
                text = formattedTime,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(16.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(listOf(accentColor, NeonCyan))
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${(timer.progress * 100).toInt()}% complete",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!timer.isCompleted) {
                Spacer(Modifier.height(20.dp))

                // Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ErrorRed.copy(alpha = 0.15f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable { onStop() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Stop",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        }
                    }

                    // Pause / Resume Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                AppColors.accentGradient
                            )
                            .clickable {
                                if (timer.isRunning) onPause() else onResume()
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (timer.isRunning) "Pause" else "Resume",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (timer.isRunning) "Pause" else "Resume",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// MEDITATION CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun MeditationCard(
    meditation: MeditationItem,
    isActive: Boolean,
    onStart: () -> Unit
) {
    val borderColor = if (isActive) PrimaryPurple.copy(alpha = 0.5f) else AppColors.dividerColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                8.dp, RoundedCornerShape(16.dp),
                ambientColor = if (isActive) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent,
                spotColor = if (isActive) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryPurple.copy(alpha = 0.12f))
                    .border(1.dp, PrimaryPurple.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(meditation.emoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(14.dp))

            // Title + Duration
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    meditation.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    meditation.duration,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Start Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) Brush.horizontalGradient(listOf(NeonGreen, NeonCyan))
                        else AppColors.accentGradient
                    )
                    .clickable { onStart() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    if (isActive) "Active" else "Start",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
