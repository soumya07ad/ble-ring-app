package com.fitness.app.presentation.settings.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.presentation.settings.SettingsViewModel
import com.fitness.app.ui.components.GlowDivider
import com.fitness.app.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showProfileSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    navigationIconContentColor = TextPrimary,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Profile Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ProfileHeader(
                    name = uiState.userName,
                    dob = uiState.userDob,
                    gender = uiState.userGender,
                    onEditClick = { showProfileSheet = true }
                )
                Spacer(modifier = Modifier.height(32.dp))
                GlowDivider(color = PrimaryPurple.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                SettingsSectionHeader("Preferences")
                SettingsSwitchRow(
                    icon = "🔔",
                    title = "Push Notifications",
                    subtitle = "Get reminders and updates",
                    iconColor = NeonOrange,
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
                SettingsSwitchRow(
                    icon = "📏",
                    title = "Use Metric Units",
                    subtitle = "Kilometres and Kilograms",
                    iconColor = NeonCyan,
                    checked = uiState.metricUnitsEnabled,
                    onCheckedChange = { viewModel.toggleMetricUnits(it) }
                )
                SettingsSwitchRow(
                    icon = "😴",
                    title = "Bedtime Reminders",
                    subtitle = "Wind down notifications",
                    iconColor = PrimaryPurple,
                    checked = uiState.bedtimeReminderEnabled,
                    onCheckedChange = { viewModel.toggleBedtimeReminder(it) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SettingsSectionHeader("Device & Sync")
                SettingsSwitchRow(
                    icon = "🔄",
                    title = "Background Data Sync",
                    subtitle = "Keep ring data up to date",
                    iconColor = NeonGreen,
                    checked = uiState.dataSyncEnabled,
                    onCheckedChange = { viewModel.toggleDataSync(it) }
                )
                SettingsActionRow(
                    icon = "💍",
                    title = "Paired Ring Configuration",
                    iconColor = NeonCyan,
                    onClick = { Toast.makeText(context, "Ring settings opened", Toast.LENGTH_SHORT).show() }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SettingsSectionHeader("Support & About")
                SettingsActionRow(
                    icon = "❓",
                    title = "Help Center",
                    iconColor = NeonBlue,
                    onClick = { Toast.makeText(context, "Help Center opened", Toast.LENGTH_SHORT).show() }
                )
                SettingsActionRow(
                    icon = "🛡️",
                    title = "Privacy Policy",
                    iconColor = TextSecondary,
                    onClick = { Toast.makeText(context, "Privacy Policy opened", Toast.LENGTH_SHORT).show() }
                )
                Spacer(modifier = Modifier.height(36.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(alpha = 0.1f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log Out",
                        color = ErrorRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showProfileSheet) {
            ProfileEditBottomSheet(
                currentName = uiState.userName,
                currentDob = uiState.userDob,
                currentGender = uiState.userGender,
                onDismiss = { showProfileSheet = false },
                onSave = { name, dob, gender ->
                    viewModel.saveProfile(name, dob, gender)
                    showProfileSheet = false
                }
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    dob: String,
    gender: String,
    onEditClick: () -> Unit
) {
    val displayName = name.ifBlank { "Add your name" }
    val age = calculateAge(dob)
    // Convert stored yyyy-MM-dd to dd/MM/yyyy for display
    val displayDob = formatDobForDisplay(dob)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .border(2.dp, PrimaryPurple.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (name.isNotBlank()) {
                Text(
                    text = name.first().uppercase(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (name.isBlank()) TextSecondary else TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (gender.isNotBlank()) {
                    InfoChip(gender, NeonCyan)
                }
                if (age != null) {
                    InfoChip("Age $age", NeonGreen)
                }
            }
        }

        IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PrimaryPurple)
        }
    }
}

@Composable
private fun InfoChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private fun calculateAge(dob: String): Int? {
    if (dob.isBlank()) return null
    return try {
        val birth = LocalDate.parse(dob, DateTimeFormatter.ISO_LOCAL_DATE)
        Period.between(birth, LocalDate.now()).years.takeIf { it >= 0 }
    } catch (e: DateTimeParseException) {
        null
    }
}

/**
 * Convert stored yyyy-MM-dd to dd/MM/yyyy for UI display.
 * Returns empty string if input is blank or unparseable.
 */
private fun formatDobForDisplay(storedDob: String): String {
    if (storedDob.isBlank()) return ""
    return try {
        val date = LocalDate.parse(storedDob, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (e: DateTimeParseException) {
        storedDob  // fallback: show as-is
    }
}

/**
 * Convert display dd/MM/yyyy to yyyy-MM-dd for storage.
 * Returns the input as-is if blank or unparseable.
 */
private fun formatDobForStorage(displayDob: String): String {
    if (displayDob.isBlank()) return ""
    return try {
        val date = LocalDate.parse(displayDob, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeParseException) {
        displayDob  // fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditBottomSheet(
    currentName: String,
    currentDob: String,
    currentGender: String,
    onDismiss: () -> Unit,
    onSave: (name: String, dob: String, gender: String) -> Unit
) {
    var name   by remember { mutableStateOf(currentName) }
    // Display DOB in dd/MM/yyyy; currentDob comes as yyyy-MM-dd from storage
    var displayDob by remember { mutableStateOf(formatDobForDisplay(currentDob)) }
    var gender by remember { mutableStateOf(currentGender) }
    var showDatePicker by remember { mutableStateOf(false) }

    val genders = listOf("Male", "Female", "Other")

    // Initialise DatePickerState with existing DOB if available
    val initialMillis = remember {
        if (currentDob.isNotBlank()) {
            try {
                LocalDate.parse(currentDob, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            } catch (e: DateTimeParseException) { null }
        } else null
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    fun validateAndSave() {
        // Convert display format to storage format
        val storageDob = formatDobForStorage(displayDob)
        onSave(name, storageDob, gender)
    }

    // DatePicker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            displayDob = selectedDate.format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = PrimaryPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = DarkCard
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = DarkCard,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = PrimaryPurple,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = PrimaryPurple,
                    dayContentColor = TextPrimary,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = PrimaryPurple,
                    todayContentColor = NeonCyan,
                    todayDateBorderColor = NeonCyan,
                    navigationContentColor = TextPrimary
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                "Edit Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(24.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name", color = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = PrimaryPurple
                )
            )
            Spacer(Modifier.height(16.dp))

            // DOB field — read-only, opens DatePicker on click
            OutlinedTextField(
                value = displayDob,
                onValueChange = { /* read-only */ },
                label = { Text("Date of Birth", color = TextSecondary) },
                placeholder = { Text("dd/MM/yyyy", color = TextMuted) },
                singleLine = true,
                readOnly = true,
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = GlassBorder,
                    disabledTextColor = TextPrimary,
                    disabledLabelColor = TextSecondary,
                    disabledPlaceholderColor = TextMuted
                )
            )
            Spacer(Modifier.height(20.dp))

            // Gender selection
            Text("Gender", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                genders.forEach { option ->
                    val selected = gender == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) PrimaryPurple.copy(alpha = 0.25f) else DarkSurfaceVariant)
                            .border(1.5.dp, if (selected) PrimaryPurple else GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { gender = option }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            color = if (selected) Color.White else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { validateAndSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Reusable row components ────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: String,
    title: String,
    subtitle: String,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryPurple,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurfaceVariant,
                uncheckedBorderColor = GlassBorder
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: String,
    title: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary
        )
    }
}
