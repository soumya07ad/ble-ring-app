package com.fitness.app.presentation.streaks.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.domain.model.ActivityStreakData
import com.fitness.app.domain.model.MilestoneData
import com.fitness.app.presentation.streaks.StreakViewModel
import com.fitness.app.ui.components.GlowDivider
import com.fitness.app.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreaksScreen(
    viewModel: StreakViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showManualEntry by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Error Snackbar
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = NeonCyan
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualEntry = true },
                containerColor = PrimaryPurple,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.activityStreaks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
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
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🔥 Streaks",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Consistency is key",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    GlowDivider(color = NeonOrange.copy(alpha = 0.4f))
                }

                // Section A: Current Longest Streak
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    LongestStreakCard(
                        activityName = uiState.longestStreakActivity ?: "No Streaks Yet",
                        streakCount = uiState.longestStreakCount
                    )
                }

                // Section B: Active Streaks
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    SectionHeader("Active Streaks", NeonCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(uiState.activityStreaks, key = { it.activityType }) { streakData ->
                    ActiveStreakCard(streakData)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Section C: Milestones
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader("Milestones", PrimaryPurple)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(uiState.milestoneProgress, key = { it.label }) { milestone ->
                    MilestoneCard(milestone)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        if (showManualEntry) {
            ManualEntryBottomSheet(
                onDismiss = { showManualEntry = false },
                onConfirm = { activity, date ->
                    viewModel.logActivity(activity, date)
                    showManualEntry = false
                    scope.launch {
                        snackbarHostState.showSnackbar("✅ $activity logged for ${if (date == java.time.LocalDate.now()) "Today" else date.toString()}")
                    }
                }
            )
        }
    }
}


@Composable
private fun SectionHeader(title: String, dotColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
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

@Composable
private fun LongestStreakCard(activityName: String, streakCount: Int) {
    val animatedCount by animateIntAsState(
        targetValue = streakCount,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "longestStreakCount"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                16.dp, RoundedCornerShape(24.dp),
                ambientColor = NeonOrange.copy(alpha = 0.3f),
                spotColor = NeonOrange.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                AppColors.sectionGradient(NeonOrange)
            )
            .border(
                1.5.dp,
                AppColors.sectionBorder(NeonOrange),
                RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "LONGEST STREAK",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonOrange.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$animatedCount",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    " days",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    activityName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonOrange
                )
            }
        }
    }
}

@Composable
private fun ActiveStreakCard(data: ActivityStreakData) {
    val icon = getActivityIcon(data.activityType)
    val color = getActivityColor(data.activityType)
    
    // Progress relative to next multiple of 7 (or 7 if 0)
    val target = ((data.currentStreak / 7) + 1) * 7
    val progress = data.currentStreak.toFloat() / target

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "activeStreakProgress"
    )
    val animatedCount by animateIntAsState(
        targetValue = data.currentStreak,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "activeStreakCount"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                8.dp, RoundedCornerShape(16.dp),
                ambientColor = color.copy(alpha = 0.15f),
                spotColor = color.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)))
            .border(1.dp, AppColors.dividerColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    data.activityType,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), color)))
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$animatedCount",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    "days",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun MilestoneCard(milestone: MilestoneData) {
    val animatedProgress by animateFloatAsState(
        targetValue = milestone.progress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "milestoneProgress"
    )

    val isCompleted = milestone.current >= milestone.target
    val color = if (isCompleted) NeonGreen else PrimaryPurple

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                6.dp, RoundedCornerShape(16.dp),
                ambientColor = color.copy(alpha = 0.1f),
                spotColor = color.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (isCompleted) NeonGreen.copy(alpha = 0.5f) else AppColors.dividerColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isCompleted) "🏆" else "🎯",
                    fontSize = 20.sp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    milestone.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) NeonGreen else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${milestone.current} / ${milestone.target}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(color.copy(alpha = 0.6f), color)
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualEntryBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate) -> Unit
) {
    val activities = listOf("Running", "Water Intake", "Meditation", "Gym", "Sleep", "Mood")
    var selectedActivity by remember { mutableStateOf(activities[0]) }
    
    // Simple state for date selection (yesterday or today for simplicity, but we'll show a basic picker)
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "Log Activity",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(24.dp))
            
            Text("Activity", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            // Activity Dropdown/Row selection
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activities.forEach { activity ->
                    val isSelected = activity == selectedActivity
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) NeonPurple else AppColors.dividerColor, RoundedCornerShape(12.dp))
                            .clickable { selectedActivity = activity }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            activity,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Date", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dates = listOf(
                    LocalDate.now().minusDays(2) to "2 Days Ago",
                    LocalDate.now().minusDays(1) to "Yesterday",
                    LocalDate.now() to "Today"
                )
                dates.forEach { (date, label) ->
                    val isSelected = date == selectedDate
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) NeonCyan else AppColors.dividerColor, RoundedCornerShape(12.dp))
                            .clickable { selectedDate = date }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onConfirm(selectedActivity, selectedDate) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Log Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// Helpers for icons and colors
private fun getActivityIcon(type: String): String = when (type) {
    "Running" -> "🏃"
    "Water Intake" -> "💧"
    "Meditation" -> "🧘"
    "Gym" -> "🏋️"
    "Sleep" -> "😴"
    "Mood" -> "✨"
    else -> "📈"
}

private fun getActivityColor(type: String): Color = when (type) {
    "Running" -> NeonOrange
    "Water Intake" -> NeonCyan
    "Meditation" -> PrimaryPurple
    "Gym" -> NeonPink
    "Sleep" -> NeonBlue
    "Mood" -> NeonGreen
    else -> TextSecondary
}
