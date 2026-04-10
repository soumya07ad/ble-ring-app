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
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showProfileSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
                    icon = "☁️",
                    title = "Sync Now",
                    iconColor = PrimaryPurple,
                    onClick = { 
                        viewModel.triggerManualSync(context)
                        Toast.makeText(context, "Manual sync triggered...", Toast.LENGTH_SHORT).show()
                    }
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
                    onClick = onNavigateToSupport
                )
                SettingsActionRow(
                    icon = "🛡️",
                    title = "Privacy Policy",
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onNavigateToPrivacy
                )
                Spacer(modifier = Modifier.height(36.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(alpha = 0.1f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { showLogoutDialog = true }
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

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log Out", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.logout(context) {
                                showLogoutDialog = false
                                onLogout()
                            }
                        }
                    ) {
                        Text("Log Out", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = NeonCyan,
                    selectedYearContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedYearContainerColor = PrimaryPurple,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContainerColor = PrimaryPurple,
                    todayContentColor = NeonCyan,
                    todayDateBorderColor = NeonCyan,
                    navigationContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(24.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = AppColors.dividerColor,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = PrimaryPurple
                )
            )
            Spacer(Modifier.height(16.dp))

            // DOB field — read-only, opens DatePicker on click
            OutlinedTextField(
                value = displayDob,
                onValueChange = { /* read-only */ },
                label = { Text("Date of Birth", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("dd/MM/yyyy", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                singleLine = true,
                readOnly = true,
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = AppColors.dividerColor,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.height(20.dp))

            // Gender selection
            Text("Gender", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
                            .background(if (selected) PrimaryPurple.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, if (selected) PrimaryPurple else AppColors.dividerColor, RoundedCornerShape(12.dp))
                            .clickable { gender = option }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                checkedTrackColor = PrimaryPurple,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = AppColors.dividerColor
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
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
