package com.fitness.app.presentation.dashboard.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitness.app.domain.model.FitnessHistoryEntry
import com.fitness.app.presentation.dashboard.FitnessHistoryViewModel
import com.fitness.app.ui.components.*
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// FITNESS HISTORY SCREEN — Past 30 days steps & distance with 7-day graph
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun FitnessHistoryRoute(
    viewModel: FitnessHistoryViewModel = viewModel<FitnessHistoryViewModel>(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    FitnessHistoryScreen(
        history = state.history,
        last7Days = state.last7Days,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        onBack = onBack,
        onRetrySync = { viewModel.retrySync() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessHistoryScreen(
    history: List<FitnessHistoryEntry>,
    last7Days: List<FitnessHistoryEntry>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRetrySync: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Top Bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Fitness History",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetrySync, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                            Text("Retry Sync", color = Color.Black)
                        }
                    }
                }
            } else if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "No fitness history found.\nWalk a few steps or sync with Health Connect!", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onRetrySync, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                            Text("Sync with Health Connect", color = Color.Black)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // ── 7-Day Steps Graph ────────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        StepsBarChart(
                            data = last7Days,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    // ── 7-Day Distance Graph ─────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        DistanceLineChart(
                            data = last7Days,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    // ── 30-Day History List Header ───────────────────
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "PAST 30 DAYS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ── History Items ─────────────────────────────────
                    itemsIndexed(history) { index, entry ->
                        HistoryDayCard(
                            entry = entry,
                            index = index,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        if (index < history.lastIndex) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// 7-DAY STEPS BAR CHART — Animated bars with gradient fill
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun StepsBarChart(
    data: List<FitnessHistoryEntry>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
    }

    val maxSteps = (data.maxOfOrNull { it.steps } ?: 1).coerceAtLeast(1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = NeonCyan.copy(alpha = 0.25f),
                spotColor = NeonCyan.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.sectionGradient(NeonCyan))
            .border(
                1.5.dp,
                AppColors.sectionBorder(NeonCyan),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "7-DAY STEPS OVERVIEW",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bar Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val w = size.width
                val h = size.height
                val padBottom = 36f
                val padTop = 16f
                val chartH = h - padBottom - padTop
                val slotW = w / data.size
                val barW = slotW * 0.5f

                // Grid lines
                for (i in 0..4) {
                    val y = padTop + chartH * i / 4f
                    drawLine(
                        NeonCyan.copy(alpha = 0.1f),
                        Offset(0f, y),
                        Offset(w, y),
                        strokeWidth = 1.5f
                    )
                }

                data.forEachIndexed { i, entry ->
                    val x = slotW * i + slotW / 2f
                    val barH = chartH * (entry.steps.toFloat() / maxSteps) * animProgress.value
                    val top = padTop + chartH - barH
                    val bottom = padTop + chartH

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.8f),
                                PrimaryPurple.copy(alpha = 0.4f)
                            ),
                            startY = top,
                            endY = bottom
                        ),
                        topLeft = Offset(x - barW / 2f, top),
                        size = androidx.compose.ui.geometry.Size(barW, barH)
                    )

                    // Dot on top of bar
                    drawCircle(NeonCyan, radius = 5f, center = Offset(x, top))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day labels
            Row(modifier = Modifier.fillMaxWidth()) {
                data.forEach { entry ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = entry.date.take(2), // day number
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(entry.steps / 1000f).let { String.format("%.1f", it) }}k",
                            fontSize = 10.sp,
                            color = NeonCyan.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// 7-DAY DISTANCE LINE CHART — Smooth curve with gradient area fill
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun DistanceLineChart(
    data: List<FitnessHistoryEntry>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1400, easing = FastOutSlowInEasing)
        )
    }

    val maxDistance = (data.maxOfOrNull { it.distance } ?: 1).coerceAtLeast(1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = NeonOrange.copy(alpha = 0.25f),
                spotColor = NeonOrange.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.sectionGradient(NeonOrange))
            .border(
                1.5.dp,
                AppColors.sectionBorder(NeonOrange),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonOrange)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "7-DAY DISTANCE TREND",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Line Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val w = size.width
                val h = size.height
                val padBottom = 36f
                val padTop = 16f
                val chartH = h - padBottom - padTop
                val slotW = w / data.size

                // Grid lines
                for (i in 0..4) {
                    val y = padTop + chartH * i / 4f
                    drawLine(
                        NeonOrange.copy(alpha = 0.1f),
                        Offset(0f, y),
                        Offset(w, y),
                        strokeWidth = 1.5f
                    )
                }

                val points = data.mapIndexed { i, entry ->
                    val x = slotW * i + slotW / 2f
                    val y = padTop + chartH * (1f - entry.distance.toFloat() / maxDistance * animProgress.value)
                    Offset(x, y)
                }

                // Area fill under curve
                if (points.size > 1) {
                    val areaPath = Path().apply {
                        moveTo(points.first().x, padTop + chartH)
                        lineTo(points.first().x, points.first().y)
                        for (j in 1 until points.size) {
                            val cx = (points[j - 1].x + points[j].x) / 2f
                            cubicTo(cx, points[j - 1].y, cx, points[j].y, points[j].x, points[j].y)
                        }
                        lineTo(points.last().x, padTop + chartH)
                        close()
                    }
                    drawPath(
                        areaPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                NeonOrange.copy(alpha = 0.3f),
                                NeonOrange.copy(alpha = 0.02f)
                            ),
                            startY = padTop,
                            endY = padTop + chartH
                        )
                    )

                    // Line
                    val linePath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (j in 1 until points.size) {
                            val cx = (points[j - 1].x + points[j].x) / 2f
                            cubicTo(cx, points[j - 1].y, cx, points[j].y, points[j].x, points[j].y)
                        }
                    }
                    drawPath(linePath, NeonOrange, style = Stroke(width = 3f, cap = StrokeCap.Round))
                }

                // Dots
                points.forEach { pt ->
                    drawCircle(NeonOrange, radius = 6f, center = pt)
                    drawCircle(Color(0xFF000000), radius = 3f, center = pt)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day labels
            Row(modifier = Modifier.fillMaxWidth()) {
                data.forEach { entry ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = entry.date.take(2),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${entry.distance}m",
                            fontSize = 9.sp,
                            color = NeonOrange.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(24.dp)
                            .background(
                                Brush.horizontalGradient(listOf(NeonOrange, WarningAmber))
                            )
                    )
                    Text(
                        text = "Distance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// HISTORY DAY CARD — Single day entry in the 30-day list
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun HistoryDayCard(
    entry: FitnessHistoryEntry,
    index: Int,
    modifier: Modifier = Modifier
) {
    val isDark = AppColors.isDark
    val shape = RoundedCornerShape(18.dp)

    // Determine if today (index 0)
    val isToday = index == 0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isToday) 12.dp else 4.dp,
                shape = shape,
                ambientColor = if (isDark) Color.Transparent else PremiumShadowColor,
                spotColor = if (isDark) Color.Transparent else PremiumShadowColor
            )
            .clip(shape)
            .then(
                if (isDark) {
                    if (isToday) {
                        Modifier.border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(NeonCyan.copy(alpha = 0.4f), PrimaryPurple.copy(alpha = 0.2f))
                            ),
                            shape
                        )
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                    }
                } else {
                    Modifier.border(1.dp, MetricCardBorder, shape)
                }
            ),
        shape = shape,
        color = if (isDark) MaterialTheme.colorScheme.surface else MetricCardGlass,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .background(
                    if (isDark) CardGlassBrush else Brush.verticalGradient(
                        listOf(PremiumGlassHighlight, MetricCardGlass)
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(56.dp)
            ) {
                Text(
                    text = entry.date.take(2),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isToday) NeonCyan else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.date.drop(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (isToday) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Divider line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(
                        if (isDark) MaterialTheme.colorScheme.outline
                        else MetricCardBorder
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Steps
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Steps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%,d".format(entry.steps),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            // Distance
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = NeonOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Distance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (entry.distance >= 1000) {
                        "%.1f km".format(entry.distance / 1000f)
                    } else {
                        "${entry.distance} m"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            // Calories
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entry.calories}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
