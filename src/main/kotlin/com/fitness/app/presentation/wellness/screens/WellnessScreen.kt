package com.fitness.app.presentation.wellness.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import com.fitness.app.data.repository.MoodDayAggregate
import com.fitness.app.data.local.entity.JournalEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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
    onBack: () -> Unit = {},
    onMeditationClick: (String) -> Unit = {},
    onJournalClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Runtime RECORD_AUDIO permission
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

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
                EmotionDropdownSelector(
                    emotions = uiState.emotions,
                    selectedEmotion = uiState.selectedEmotion,
                    onSelect = { viewModel.selectEmotion(it) }
                )
            }

            // Journal Input Dialog
            if (uiState.showJournalDialog) {
                item {
                    JournalInputDialog(
                        uiState = uiState,
                        onDismiss = { viewModel.dismissDialog() },
                        onSave = { msg -> viewModel.saveJournalEntry(msg) },
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { viewModel.stopRecording() },
                        onPlayPreview = { viewModel.playPreview() },
                        onPausePlayback = { viewModel.pausePlayback() },
                        onResumePlayback = { viewModel.resumePlayback() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onStopPlayback = { viewModel.stopPlayback() },
                        onDiscardRecording = { viewModel.discardRecording() },
                        hasAudioPermission = hasAudioPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }

            // ── Section 2: Mood Meter ─────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                MoodMeterSection(uiState = uiState, viewModel = viewModel)
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
                    onStart = {
                        val category = when (meditation.id) {
                            "1" -> "morning_calm"
                            "2" -> "breathing"
                            "3" -> "sleep"
                            else -> "morning_calm"
                        }
                        onMeditationClick(category)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Section 4: Journal ──────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "Journal",
                        dotColor = NeonOrange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                8.dp, RoundedCornerShape(20.dp),
                                ambientColor = NeonOrange.copy(alpha = 0.2f),
                                spotColor = NeonOrange.copy(alpha = 0.3f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .border(
                                1.5.dp,
                                AppColors.sectionBorder(NeonOrange),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onJournalClick() }
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NeonOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📔", fontSize = 24.sp)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open My Journal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "View and reflect on past entries",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Open Journal",
                                tint = NeonOrange
                            )
                        }
                    }
                }
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
    val isDark = AppColors.isDark
    val shape = RoundedCornerShape(16.dp)

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) NeonCyan else SkyBlue
        } else {
            if (isDark) AppColors.dividerColor else PremiumGlassBorder
        },
        animationSpec = tween(300),
        label = "emotionBorder"
    )

    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(listOf(SkyBlue, HighlighterGreen))
    } else {
        if (isDark) Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
        else Brush.verticalGradient(
            listOf(
                PremiumGlassHighlight,
                PremiumGlassWhite
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 10.dp else 6.dp,
                shape = shape,
                ambientColor = if (isSelected) SkyBlue.copy(alpha = 0.35f) else PremiumShadowColor,
                spotColor = if (isSelected) SkyBlue.copy(alpha = 0.35f) else PremiumShadowColor
            )
            .clip(shape)
            .background(bgBrush)
            .then(
                if (!isSelected) Modifier.border(1.dp, borderColor, shape)
                else Modifier
            )
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
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White
                    else if (isDark) MaterialTheme.colorScheme.onSurfaceVariant
                    else DarkGrayText
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DROPDOWN EMOTION SELECTOR
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmotionDropdownSelector(
    emotions: List<Emotion>,
    selectedEmotion: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (selectedEmotion != null) {
        val em = emotions.find { it.name == selectedEmotion }
        "${em?.emoji ?: ""} ${em?.name ?: selectedEmotion}"
    } else {
        "Select Emotion"
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = SkyBlue.copy(alpha = 0.15f), spotColor = SkyBlue.copy(alpha = 0.15f))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PremiumGlassHighlight,
                            PremiumGlassWhite
                        )
                    )
                )
                .border(1.dp, PremiumGlassBorder, RoundedCornerShape(16.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dropdown panel
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                PremiumGlassHighlight,
                                PremiumGlassWhite
                            )
                        )
                    )
                    .border(1.dp, PremiumGlassBorder, RoundedCornerShape(16.dp))
            ) {
                emotions.forEachIndexed { index, emotion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expanded = false
                                onSelect(emotion.name)
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(emotion.emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            emotion.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index < emotions.lastIndex) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// JOURNAL INPUT DIALOG
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun JournalInputDialog(
    uiState: WellnessUiState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayPreview: () -> Unit,
    onPausePlayback: () -> Unit,
    onResumePlayback: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onStopPlayback: () -> Unit,
    onDiscardRecording: () -> Unit,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Pulse animation for recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF0F8FF), Color.White)
                    )
                )
                .border(1.dp, PremiumGlassBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                // Title
                Text(
                    text = "${uiState.dialogEmoji} What made you feel ${uiState.dialogEmotion.lowercase()}?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(Modifier.height(20.dp))

                // Text Input
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = {
                        Text(
                            "Write about how you feel...",
                            color = Color(0xFF94A3B8)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
                Spacer(Modifier.height(16.dp))

                // Audio section label
                Text(
                    "Or record a voice note",
                    fontSize = 13.sp,
                    color = Color(0xFF6B6B6B),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))

                // --- Recording indicator ---
                if (uiState.isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ErrorRed.copy(alpha = 0.08f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "\uD83D\uDD34",
                            fontSize = 16.sp,
                            modifier = Modifier.graphicsLayer(alpha = pulseAlpha)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Recording...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ErrorRed
                        )
                        Spacer(Modifier.width(12.dp))
                        val m = uiState.recordingSeconds / 60
                        val s = uiState.recordingSeconds % 60
                        Text(
                            "%02d:%02d".format(m, s),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // --- Unified Record Button ---
                if (!uiState.hasRecording) {
                    val buttonColor by animateColorAsState(
                        targetValue = if (uiState.isRecording) ErrorRed else SkyBlue,
                        label = "buttonColor"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(buttonColor)
                            .pointerInput(hasAudioPermission) {
                                detectTapGestures(
                                    onPress = {
                                        if (!hasAudioPermission) {
                                            onRequestPermission()
                                        } else {
                                            onStartRecording()
                                            tryAwaitRelease()
                                            onStopRecording()
                                        }
                                    }
                                )
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = if (uiState.isRecording) "Recording" else "Record",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !hasAudioPermission -> "Tap to grant permission"
                                    uiState.isRecording -> "Release to stop"
                                    else -> "Hold to record"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // --- Playback controls ---
                if (uiState.hasRecording) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Play/Pause toggle
                            IconButton(
                                onClick = {
                                    if (uiState.isPlayingPreview) onPausePlayback() else {
                                        if (uiState.playbackPositionMs > 0 && uiState.playbackPositionMs < uiState.audioDurationMs)
                                            onResumePlayback()
                                        else
                                            onPlayPreview()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SkyBlue)
                            ) {
                                Icon(
                                    if (uiState.isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // Position label
                            val posSec = uiState.playbackPositionMs / 1000
                            val durSec = uiState.audioDurationMs / 1000
                            Text(
                                "%d:%02d / %d:%02d".format(posSec / 60, posSec % 60, durSec / 60, durSec % 60),
                                fontSize = 12.sp,
                                color = Color(0xFF6B6B6B),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Seek slider
                        Slider(
                            value = if (uiState.audioDurationMs > 0) uiState.playbackPositionMs.toFloat() / uiState.audioDurationMs else 0f,
                            onValueChange = { frac ->
                                onSeekTo((frac * uiState.audioDurationMs).toInt())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = SkyBlue,
                                activeTrackColor = SkyBlue,
                                inactiveTrackColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // Record Again button
                        OutlinedButton(
                            onClick = onDiscardRecording,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Record Again",
                                tint = ErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Record Again",
                                color = ErrorRed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(message) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyBlue
                        )
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// JOURNAL SECTION
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun JournalSection(
    entries: List<JournalEntry>,
    uiState: WellnessUiState,
    onPlayAudio: (String) -> Unit,
    onPauseAudio: () -> Unit,
    onResumeAudio: () -> Unit,
    onSeekAudio: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        SectionHeader(title = "Journal", dotColor = NeonOrange)
        Spacer(Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(PremiumGlassHighlight, PremiumGlassWhite)
                        )
                    )
                    .border(1.dp, PremiumGlassBorder, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No journal entries yet.\nSelect an emotion to write your first entry.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            entries.forEach { entry ->
                JournalEntryCard(
                    entry = entry,
                    uiState = uiState,
                    onPlayAudio = onPlayAudio,
                    onPauseAudio = onPauseAudio,
                    onResumeAudio = onResumeAudio,
                    onSeekAudio = onSeekAudio
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun JournalEntryCard(
    entry: JournalEntry,
    uiState: WellnessUiState,
    onPlayAudio: (String) -> Unit,
    onPauseAudio: () -> Unit,
    onResumeAudio: () -> Unit,
    onSeekAudio: (Int) -> Unit
) {
    val isPlayingThis = entry.audioPath != null && entry.audioPath == uiState.playingAudioPath
    
    val emoji = when (entry.emotion) {
        "Happy" -> "😊"
        "Calm" -> "😌"
        "Excited" -> "🤩"
        "Grateful" -> "🙏"
        "Anxious" -> "😰"
        "Sad" -> "😢"
        "Frustrated" -> "😤"
        "Peaceful" -> "🕊️"
        else -> "😶"
    }

    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val date = Date(entry.createdAt)
    val timeStr = timeFormatter.format(date)
    val dateStr = dateFormatter.format(date)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = PremiumShadowColor, spotColor = PremiumShadowColor)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(PremiumGlassHighlight, PremiumGlassWhite)
                )
            )
            .border(1.dp, PremiumGlassBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji
                Text(emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.emotion,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    if (!entry.message.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "\"${entry.message}\"",
                            fontSize = 13.sp,
                            color = Color(0xFF6B6B6B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Audio Play/Pause button
                if (!entry.audioPath.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            if (isPlayingThis) {
                                if (uiState.isPlayingPreview) onPauseAudio() else onResumeAudio()
                            } else {
                                onPlayAudio(entry.audioPath)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SkyBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlayingThis && uiState.isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Seeker for the active entry
            if (isPlayingThis) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(8.dp)
                ) {
                    val posSec = uiState.playbackPositionMs / 1000
                    val durSec = uiState.audioDurationMs / 1000
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "%d:%02d".format(posSec / 60, posSec % 60),
                            fontSize = 10.sp,
                            color = Color(0xFF6B6B6B)
                        )
                        Text(
                            "%d:%02d".format(durSec / 60, durSec % 60),
                            fontSize = 10.sp,
                            color = Color(0xFF6B6B6B)
                        )
                    }
                    
                    Slider(
                        value = if (uiState.audioDurationMs > 0) uiState.playbackPositionMs.toFloat() / uiState.audioDurationMs else 0f,
                        onValueChange = { frac ->
                            onSeekAudio((frac * uiState.audioDurationMs).toInt())
                        },
                        modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = SkyBlue,
                            activeTrackColor = SkyBlue,
                            inactiveTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            // Timestamp
            Spacer(Modifier.height(8.dp))
            Text(
                "$dateStr  •  $timeStr",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// MOOD METER SECTION
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun MoodMeterSection(uiState: WellnessUiState, viewModel: WellnessViewModel) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        SectionHeader(title = "Mood Meter", dotColor = NeonCyan)
        Spacer(Modifier.height(16.dp))

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.cardBackground)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("Daily", "Weekly", "Monthly")
            tabs.forEachIndexed { index, title ->
                val isSelected = uiState.selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isSelected) Modifier.background(AppColors.sectionGradient(NeonCyan))
                            else Modifier.background(Color.Transparent)
                        )
                        .clickable { viewModel.setTab(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Stacked Chart
        StackedMoodBarChart(uiState.chartData)
        
        Spacer(Modifier.height(16.dp))
        
        // Metrics Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val avgStr = if (uiState.averageMoodScore > 0) "+${String.format("%.1f", uiState.averageMoodScore)}" else String.format("%.1f", uiState.averageMoodScore)
            MoodMetricCard(Modifier.weight(1f), "Average Mood", avgStr)
            MoodMetricCard(Modifier.weight(1f), "Best Day", uiState.bestDay?.dateLabel ?: "-")
            MoodMetricCard(Modifier.weight(1f), "Worst Day", uiState.worstDay?.dateLabel ?: "-")
        }

        Spacer(Modifier.height(24.dp))
        WellnessScoreCard(score = uiState.wellnessScore)
    }
}

@Composable
private fun MoodMetricCard(modifier: Modifier = Modifier, title: String, value: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)))
            .border(1.dp, AppColors.dividerColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun getEmotionColor(emotion: String): Color = when (emotion) {
    "Happy" -> Color(0xFFFFD60A) // Yellow
    "Calm" -> NeonBlue
    "Excited" -> NeonOrange
    "Grateful" -> NeonPurple
    "Peaceful" -> NeonGreen
    "Anxious" -> Color(0xFF64D2FF) // Light Blue
    "Sad" -> Color(0xFF0040DD) // Dark Blue
    "Frustrated" -> ErrorRed
    else -> Color.Gray
}

@Composable
fun StackedMoodBarChart(data: List<MoodDayAggregate>) {
    val dividerColor = AppColors.dividerColor
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = NeonCyan.copy(alpha=0.2f), spotColor = NeonCyan.copy(alpha=0.3f))
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.sectionGradient(NeonCyan))
            .border(1.5.dp, AppColors.sectionBorder(NeonCyan), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val w = size.width
                val h = size.height
                val padBottom = 30f
                val chartH = h - padBottom
                
                if (data.isEmpty()) return@Canvas
                
                val slotW = w / data.size
                val barW = (slotW * 0.6f).coerceAtMost(40f)
                
                for (i in 0..4) {
                    val y = chartH * i / 4f
                    drawLine(dividerColor.copy(alpha = 0.5f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                val maxEntries = data.maxOfOrNull { it.entries.size } ?: 1
                val maxVisible = maxEntries.coerceAtLeast(5)

                data.forEachIndexed { i, day ->
                    val cx = slotW * i + slotW / 2f
                    var currentBottom = chartH
                    
                    if (day.entries.isNotEmpty()) {
                        val unitH = chartH / maxVisible
                        
                        day.entries.forEach { entry ->
                            val color = getEmotionColor(entry.emotion)
                            val top = currentBottom - unitH
                            
                            drawRect(
                                color = color,
                                topLeft = Offset(cx - barW / 2f, top),
                                size = Size(barW, unitH)
                            )
                            currentBottom = top
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val showFilter = data.size <= 7
                data.forEachIndexed { index, day ->
                    val show = showFilter || (index % 5 == 0) || index == data.lastIndex
                    if (show) {
                        Text(
                            text = day.dateLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B6B6B),
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    } else if (data.size > 7) {
                        // Keep spacing for hidden elements so alignment is retained
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
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
                    .shadow(
                        8.dp,
                        RoundedCornerShape(28.dp),
                        ambientColor = SkyBlue.copy(alpha = 0.3f),
                        spotColor = SkyBlue.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(listOf(SkyBlue, SoftHighlighterGreen))
                    )
                    .clickable { onStart() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    if (isActive) "Active" else "Start",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
