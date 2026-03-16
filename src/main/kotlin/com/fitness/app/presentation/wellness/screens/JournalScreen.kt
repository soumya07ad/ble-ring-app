package com.fitness.app.presentation.wellness.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.data.local.entity.JournalEntry
import com.fitness.app.presentation.wellness.WellnessUiState
import com.fitness.app.presentation.wellness.WellnessViewModel
import com.fitness.app.ui.components.GlowDivider
import com.fitness.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: WellnessViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val entries by viewModel.journalEntries.collectAsState()

    Scaffold(
        containerColor = Color(0xFFFDFBF7) // Diary theme background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Header
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
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "My Mood Diary",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "Reflect on your journey",
                            fontSize = 13.sp,
                            color = Color(0xFF6B6B6B)
                        )
                    }
                }
                GlowDivider(color = NeonOrange.copy(alpha = 0.4f))
            }

            // Journal Entries
            item {
                Spacer(modifier = Modifier.height(24.dp))
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White, Color(0xFFF8F9FA))
                                )
                            )
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No journal entries yet.\nSelect an emotion on the Wellness page to write your first entry.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            items(entries, key = { it.id }) { entry ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    JournalEntryCard(
                        entry = entry,
                        uiState = uiState,
                        onPlayAudio = { viewModel.playJournalAudio(it) },
                        onPauseAudio = { viewModel.pausePlayback() },
                        onResumeAudio = { viewModel.resumePlayback() },
                        onSeekAudio = { viewModel.seekTo(it) }
                    )
                }
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
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha=0.05f), spotColor = Color.Black.copy(alpha=0.05f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
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
                            color = Color(0xFF475569),
                            maxLines = 4,
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
                                .background(NeonOrange),
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
                        .background(Color(0xFFF8FAFC))
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
                            color = Color(0xFF64748B)
                        )
                        Text(
                            "%d:%02d".format(durSec / 60, durSec % 60),
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Slider(
                        value = if (uiState.audioDurationMs > 0) uiState.playbackPositionMs.toFloat() / uiState.audioDurationMs else 0f,
                        onValueChange = { frac ->
                            onSeekAudio((frac * uiState.audioDurationMs).toInt())
                        },
                        modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonOrange,
                            activeTrackColor = NeonOrange,
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // Timestamp
            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(8.dp))
            Text(
                "$dateStr  •  $timeStr",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
