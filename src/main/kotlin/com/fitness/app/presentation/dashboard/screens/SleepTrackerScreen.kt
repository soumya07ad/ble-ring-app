package com.fitness.app.presentation.dashboard.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.ui.theme.*
import com.fitness.app.ui.components.GlowDivider
import com.fitness.app.presentation.dashboard.SleepTrackerViewModel
import com.fitness.app.domain.repository.SleepDayUiModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════
// UI DATA HELPERS
// ═══════════════════════════════════════════════════════════════════════

enum class SleepQuality(val label: String, val color: Color) {
    POOR("Poor", Color(0xFFFF453A)),
    FAIR("Fair", Color(0xFFFF9F0A)),
    GOOD("Good", Color(0xFF30D158)),
    EXCELLENT("Excellent", Color(0xFF00F0FF))
}

fun hoursToQuality(hours: Double): SleepQuality = when {
    hours < 5.0  -> SleepQuality.POOR
    hours < 6.5 -> SleepQuality.FAIR
    hours < 8.0  -> SleepQuality.GOOD
    else        -> SleepQuality.EXCELLENT
}

// ═══════════════════════════════════════════════════════════════════════
// SLEEP TRACKER SCREEN
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTrackerScreen(
    viewModel: SleepTrackerViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Form state
    var bedHour   by remember { mutableStateOf(22) }
    var bedMin    by remember { mutableStateOf(30) }
    var wakeHour  by remember { mutableStateOf(6) }
    var wakeMin   by remember { mutableStateOf(30) }
    
    // Always default to today
    val todayIso = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    var selectedIsoDate by remember { mutableStateOf(todayIso) }
    
    var showDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle Messages (Errors or Success)
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { errorMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(message = errorMsg)
                viewModel.clearMessages()
            }
        }
        uiState.successMessage?.let { successMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(message = successMsg)
                viewModel.clearMessages()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Header ────────────────────────────────────────────────
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
                            text = "😴  Sleep Tracker",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track your rest, improve your life",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                GlowDivider(color = PrimaryPurple.copy(alpha = 0.4f))

                Spacer(modifier = Modifier.height(20.dp))

                // ── Quick Stats Row ───────────────────────────────────────
                QuickSleepStats(uiState.average, uiState.bestNight, uiState.last7Days.lastOrNull()?.hours ?: 0.0)

                Spacer(modifier = Modifier.height(20.dp))

                // ── 7-Day Sleep Graph ─────────────────────────────────────
                SleepWeekChart(uiState.last7Days)

                Spacer(modifier = Modifier.height(20.dp))

                // ── Log Sleep Button ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = PrimaryPurple.copy(alpha = 0.4f), spotColor = PrimaryPurple.copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(PrimaryPurple, NeonPink.copy(alpha = 0.8f))))
                        .clickable { showDialog = true }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🛏️", fontSize = 20.sp)
                        Text(
                            text = "Log Sleep",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── This Week's Sleep Log ─────────────────────────────────
                SleepLogList(uiState.last7Days)

                Spacer(modifier = Modifier.height(20.dp))

                // ── Sleep Statistics ──────────────────────────────────────
                SleepStatisticsCard(
                    avg = uiState.average,
                    best = uiState.bestNight,
                    worst = uiState.worstNight,
                    total = uiState.weeklyTotal,
                    streak = uiState.consistencyCount
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Sleep Tips ────────────────────────────────────────────
                SleepTipsCard()

                Spacer(modifier = Modifier.height(40.dp))
            }

            // ── Log Sleep Dialog ─────────────────────────────────────────
            if (showDialog) {
                LogSleepDialog(
                    selectedIsoDate = selectedIsoDate,
                    bedHour = bedHour, bedMin = bedMin,
                    wakeHour = wakeHour, wakeMin = wakeMin,
                    onDateChange = { selectedIsoDate = it },
                    onBedHourChange = { bedHour = it },
                    onBedMinChange = { bedMin = it },
                    onWakeHourChange = { wakeHour = it },
                    onWakeMinChange = { wakeMin = it },
                    onSave = {
                        val totalMins = ((wakeHour * 60 + wakeMin) - (bedHour * 60 + bedMin)).let {
                            if (it < 0) it + 24 * 60 else it
                        }
                        val hours = totalMins / 60.0
                        viewModel.logSleep(selectedIsoDate, hours)
                        showDialog = false
                    },
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// QUICK STATS ROW
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun QuickSleepStats(avg: Double, best: Double, last: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatTile(modifier = Modifier.weight(1f), emoji = "📊", label = "Avg", value = "%.1fh".format(avg), color = NeonCyan)
        QuickStatTile(modifier = Modifier.weight(1f), emoji = "🏆", label = "Best", value = "%.1fh".format(best), color = NeonGreen)
        QuickStatTile(modifier = Modifier.weight(1f), emoji = "🌙", label = "Last", value = "%.1fh".format(last), color = PrimaryPurple)
    }
}

@Composable
private fun QuickStatTile(modifier: Modifier, emoji: String, label: String, value: String, color: Color) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = color.copy(alpha = 0.3f), spotColor = color.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, fontSize = 22.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// 7-DAY SLEEP BAR CHART (Normalized to 16h cap visually)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun SleepWeekChart(data: List<SleepDayUiModel>) {
    // Normalization fixed at 16h visually. If hours > 16, graph clips to 16h height.
    val maxVisualHours = 16f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = PrimaryPurple.copy(alpha = 0.2f), spotColor = PrimaryPurple.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.sectionGradient(PrimaryPurple))
            .border(1.5.dp, AppColors.sectionBorder(PrimaryPurple), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(PrimaryPurple))
                Spacer(Modifier.width(10.dp))
                Text("LAST 7 DAYS GRAPH", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text("Recommended: 7-9h", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }

            Spacer(Modifier.height(20.dp))

            val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val w = size.width
                val h = size.height
                val padBottom = 32f
                val padTop = 10f
                val chartH = h - padBottom - padTop
                val slots = 7
                val slotW = w / slots
                val barW = slotW * 0.48f

                // Reference line at 8h
                val refY = padTop + chartH * (1f - 8f / maxVisualHours)
                drawLine(NeonGreen.copy(alpha = 0.3f), Offset(0f, refY), Offset(w, refY), strokeWidth = 1.5f)

                // Grid lines (y-axis) up to 16h
                for (g in 0..4) {
                    val gy = padTop + chartH * (g / 4f)
                    drawLine(gridLineColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                }

                data.forEachIndexed { i, entry ->
                    val hrs = entry.hours.toFloat()
                    val color = hoursToQuality(entry.hours).color
                    val x = slotW * i + slotW / 2f
                    
                    // Clamp visual height to 16h max, animation is smooth
                    val cappedHrs = hrs.coerceAtMost(maxVisualHours)
                    val barH = chartH * (cappedHrs / maxVisualHours)
                    val top = padTop + chartH - barH
                    val bottom = padTop + chartH

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.3f)),
                            startY = top, endY = bottom
                        ),
                        topLeft = Offset(x - barW / 2f, top),
                        size = androidx.compose.ui.geometry.Size(barW, barH.coerceAtLeast(0f))
                    )

                    // Top cap
                    if (barH > 4f) {
                        drawRect(
                            color = color,
                            topLeft = Offset(x - barW / 2f, top),
                            size = androidx.compose.ui.geometry.Size(barW, 3f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day + exact hour labels (showing real raw value even if > 16h)
            Row(Modifier.fillMaxWidth()) {
                data.forEach { entry ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(entry.dayName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (entry.hours > 0.0) "%.1fh".format(entry.hours) else "-",
                            fontSize = 10.sp,
                            color = hoursToQuality(entry.hours).color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Quality legend
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SleepQuality.values().forEach { q ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(q.color))
                        Text(q.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// LOG SLEEP DIALOG (Supports any date)
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun LogSleepDialog(
    selectedIsoDate: String,
    bedHour: Int, bedMin: Int,
    wakeHour: Int, wakeMin: Int,
    onDateChange: (String) -> Unit,
    onBedHourChange: (Int) -> Unit,
    onBedMinChange: (Int) -> Unit,
    onWakeHourChange: (Int) -> Unit,
    onWakeMinChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    // Generate simple list of past 7 dates to pick from, ending with today.
    // In a real app we could use a DatePicker, but here we provide a quick 7-day scroll format.
    val recentDates = remember {
        val today = LocalDate.now()
        (6 downTo 0).map { i -> today.minusDays(i.toLong()) }
    }

    val totalMins = ((wakeHour * 60 + wakeMin) - (bedHour * 60 + bedMin)).let {
        if (it < 0) it + 24 * 60 else it
    }
    val calcHours = totalMins / 60
    val calcMins  = totalMins % 60

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🛏️", fontSize = 22.sp)
                Text("Log Sleep", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {

                // Calculated duration preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryPurple.copy(alpha = 0.15f))
                        .border(1.dp, PrimaryPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Duration: ${calcHours}h ${calcMins}m",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = hoursToQuality(totalMins / 60.0).color
                    )
                }

                // Date Picker Strip
                Text("Select Date (Today/Past)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recentDates.forEach { ld ->
                        val isoStr = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val isSelected = isoStr == selectedIsoDate
                        val label = if (ld == LocalDate.now()) "Today" else ld.dayOfWeek.name.take(3)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .border(1.dp, if (isSelected) PrimaryPurple else AppColors.dividerColor, RoundedCornerShape(10.dp))
                                .clickable { onDateChange(isoStr) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text("${ld.dayOfMonth}/${ld.monthValue}", fontSize = 9.sp, color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Bed time
                Text("Bedtime", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                TimePickerRow(
                    hour = bedHour, minute = bedMin,
                    onHourChange = onBedHourChange,
                    onMinChange = onBedMinChange,
                    accentColor = NeonPink
                )

                // Wake time
                Text("Wake Time", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                TimePickerRow(
                    hour = wakeHour, minute = wakeMin,
                    onHourChange = onWakeHourChange,
                    onMinChange = onWakeMinChange,
                    accentColor = NeonCyan
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun TimePickerRow(
    hour: Int, minute: Int,
    onHourChange: (Int) -> Unit,
    onMinChange: (Int) -> Unit,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hour
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { onHourChange((hour + 1) % 24) }) {
                Icon(Icons.Default.KeyboardArrowUp, null, tint = accentColor)
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("%02d".format(hour), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
            IconButton(onClick = { onHourChange((hour - 1 + 24) % 24) }) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = accentColor)
            }
        }
        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Minute
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { onMinChange((minute + 15) % 60) }) {
                Icon(Icons.Default.KeyboardArrowUp, null, tint = accentColor)
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("%02d".format(minute), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
            IconButton(onClick = { onMinChange((minute - 15 + 60) % 60) }) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = accentColor)
            }
        }
        Text(
            text = "%02d:%02d".format(hour, minute),
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SLEEP LOG LIST
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SleepLogList(data: List<SleepDayUiModel>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = NeonCyan.copy(alpha = 0.15f), spotColor = NeonCyan.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.sectionGradient(NeonCyan))
            .border(1.5.dp, AppColors.sectionBorder(NeonCyan), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(NeonGreen))
                Spacer(Modifier.width(10.dp))
                Text("THIS WEEK'S LOG", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(14.dp))
            
            data.forEachIndexed { idx, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.dayName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(38.dp))
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                    ) {
                        val pct = (entry.hours / 16.0).coerceIn(0.0, 1.0).toFloat()
                        val color = hoursToQuality(entry.hours).color
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(pct)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(color, color.copy(alpha = 0.5f))
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (entry.hours > 0) "%.1fh".format(entry.hours) else "—",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = hoursToQuality(entry.hours).color,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }
                if (idx < data.lastIndex) HorizontalDivider(color = AppColors.dividerColor, thickness = 0.5.dp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SLEEP STATISTICS CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SleepStatisticsCard(avg: Double, best: Double, worst: Double, total: Double, streak: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = NeonGreen.copy(alpha = 0.15f), spotColor = NeonGreen.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.sectionGradient(NeonGreen))
            .border(1.5.dp, AppColors.sectionBorder(NeonGreen), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(NeonGreen))
                Spacer(Modifier.width(10.dp))
                Text("SLEEP STATISTICS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(Modifier.weight(1f), "📊", "Average", "%.1fh".format(avg), NeonCyan)
                StatBox(Modifier.weight(1f), "🏆", "Best Night", "%.1fh".format(best), NeonGreen)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(Modifier.weight(1f), "📉", "Worst Night", "%.1fh".format(worst), NeonOrange)
                StatBox(Modifier.weight(1f), "✅", "7h+ Days", "${streak} days", PrimaryPurple)
            }
            Spacer(Modifier.height(12.dp))
            // Weekly total
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonGreen.copy(alpha = 0.1f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("🌙", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("Total sleep this week", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("%.1fh".format(total), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = NeonGreen)
                }
            }
        }
    }
}

@Composable
private fun StatBox(modifier: Modifier, emoji: String, label: String, value: String, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, fontSize = 18.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SLEEP TIPS CARD
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SleepTipsCard() {
    val tips = listOf(
        Triple("📵", "Screen-Free Wind Down", "Avoid screens 1 hour before bed. Blue light suppresses melatonin production, delaying sleep by up to 3 hours."),
        Triple("🌡️", "Cool Your Room", "Keep your bedroom between 18–20°C (65–68°F). Core body temperature must drop to initiate sleep."),
        Triple("⏰", "Be Consistent", "Go to bed and wake up at the same time every day — even weekends. This anchors your circadian rhythm."),
        Triple("☕", "Limit Caffeine", "Avoid caffeine after 2 PM. Its half-life is ~6 hours, so a 3PM coffee still has half its stimulant effect at 9PM."),
        Triple("🧘", "Try Relaxation Techniques", "4-7-8 breathing or progressive muscle relaxation can reduce time to fall asleep by up to 50%."),
        Triple("🌙", "Optimize Your Environment", "Use blackout curtains, a white noise machine, or earplugs. Darkness signals your brain to release melatonin.")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = NeonOrange.copy(alpha = 0.15f), spotColor = NeonOrange.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.sectionGradient(NeonOrange))
            .border(1.5.dp, AppColors.sectionBorder(NeonOrange), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(NeonOrange))
                Spacer(Modifier.width(10.dp))
                Text("TIPS FOR BETTER SLEEP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(16.dp))
            tips.forEachIndexed { idx, (emoji, title, desc) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonOrange.copy(alpha = 0.12f))
                            .border(1.dp, NeonOrange.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                    }
                }
                if (idx < tips.lastIndex) HorizontalDivider(color = AppColors.dividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}
