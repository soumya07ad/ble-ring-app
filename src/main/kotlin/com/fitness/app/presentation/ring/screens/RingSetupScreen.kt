package com.fitness.app.presentation.ring.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.SignalQuality
import com.fitness.app.presentation.ring.PermissionUiState
import com.fitness.app.presentation.ring.RingUiState
import com.fitness.app.presentation.ring.RingViewModel
import com.fitness.app.presentation.ring.components.*
import com.fitness.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM RING SETUP SCREEN - Modern Fitness App Design
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun RingSetupScreen(
    onSetupComplete: () -> Unit,
    onSkip: () -> Unit,
    viewModel: RingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        val shouldShowRationale = if (!allGranted && context is Activity) {
            viewModel.getRequiredPermissions().any { perm ->
                context.shouldShowRequestPermissionRationale(perm)
            }
        } else true
        viewModel.onPermissionResult(allGranted, shouldShowRationale)
    }
    
    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Animated background
        AnimatedBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            PremiumSetupHeader()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Content based on state
            when {
                !uiState.hasPermissions -> {
                    PermissionContent(
                        permissionState = uiState.permissionState,
                        onRequestPermission = {
                            permissionLauncher.launch(viewModel.getRequiredPermissions())
                        },
                        onOpenSettings = {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            })
                        }
                    )
                }
                uiState.isConnected -> {
                    ConnectedContent(
                        uiState = uiState,
                        onDisconnect = { viewModel.disconnect() },
                        onDone = onSetupComplete,
                        onMeasureHeartRate = { viewModel.startHeartRateMeasurement() },
                        onMeasureBloodPressure = { viewModel.startBloodPressureMeasurement() },
                        onMeasureSpO2 = { viewModel.startSpO2Measurement() },
                        onMeasureStress = { viewModel.startStressMeasurement() },
                        onRequestSleep = { viewModel.requestSleepHistory() }
                    )
                }
                uiState.isConnecting -> {
                    ConnectingContent()
                }
                uiState.showManualEntry -> {
                    ManualEntryContent(
                        macAddress = uiState.manualMacAddress,
                        onMacChange = { viewModel.updateManualMacAddress(it) },
                        onConnect = { viewModel.connectByMacAddress(uiState.manualMacAddress) },
                        onBack = { viewModel.toggleManualEntry() }
                    )
                }
                else -> {
                    ScanContent(
                        uiState = uiState,
                        onStartScan = { viewModel.startScan() },
                        onStopScan = { viewModel.stopScan() },
                        onDeviceSelected = { viewModel.connectToDevice(it) },
                        onManualEntry = { viewModel.toggleManualEntry() },
                        onSkip = onSkip
                    )
                }
            }
            
            // Error snackbar
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ErrorRed.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,  // Using Warning instead of Error
                            contentDescription = null,
                            tint = ErrorRed
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = AccentCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        PrimaryPurple.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(animatedOffset, 300f),
                    radius = 600f
                )
            )
    )
}

@Composable
private fun PremiumSetupHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated ring icon
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.3f),
                            PrimaryPurple.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(2.dp, PrimaryPurple.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,  // Device icon
                contentDescription = "Ring",
                tint = PrimaryPurple,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Pair Your Smart Ring",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Connect your fitness ring for real-time health data",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionContent(
    permissionState: PermissionUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(WarningAmber.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = when (permissionState) {
                PermissionUiState.PermanentlyDenied -> "Permissions Required"
                else -> "Allow Bluetooth Access"
            },
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = when (permissionState) {
                PermissionUiState.PermanentlyDenied -> 
                    "Please enable Bluetooth permissions in Settings to use this feature."
                else -> 
                    "We need Bluetooth permissions to scan for and connect to your ring."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        PremiumButton(
            text = if (permissionState == PermissionUiState.PermanentlyDenied) 
                "Open Settings" else "Grant Permission",
            onClick = if (permissionState == PermissionUiState.PermanentlyDenied) 
                onOpenSettings else onRequestPermission
        )
    }
}

@Composable
private fun ScanContent(
    uiState: RingUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceSelected: (Ring) -> Unit,
    onManualEntry: () -> Unit,
    onSkip: () -> Unit
) {
    var selectedDevice by remember { mutableStateOf<Ring?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Scan button
        PremiumButton(
            text = if (uiState.isScanning) "Stop Scanning" else "Scan for Devices",
            onClick = if (uiState.isScanning) onStopScan else onStartScan,
            isLoading = uiState.isScanning,
            icon = if (uiState.isScanning) null else Icons.Default.Search
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Device list or empty state
        if (uiState.scannedDevices.isNotEmpty()) {
            Text(
                text = "Found Devices",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.scannedDevices) { device ->
                    DeviceCard(
                        ring = device,
                        isSelected = selectedDevice?.macAddress == device.macAddress,
                        onClick = { selectedDevice = device }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PremiumButton(
                text = "Connect",
                onClick = { selectedDevice?.let { onDeviceSelected(it) } },
                enabled = selectedDevice != null
            )
        } else if (!uiState.isScanning) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap 'Scan for Devices' to find your ring",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Scanning animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = PrimaryPurple,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Scanning for devices...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Make sure your ring is nearby",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bottom buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryButton(
                text = "Enter MAC",
                onClick = onManualEntry,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Skip",
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConnectingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Pulsing animation
            val infiniteTransition = rememberInfiniteTransition(label = "connect")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AccentCyan.copy(alpha = 0.3f),
                                AccentCyan.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = AccentCyan,
                    strokeWidth = 4.dp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Connecting...",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Please wait while we pair your ring",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ConnectedContent(
    uiState: RingUiState,
    onDisconnect: () -> Unit,
    onDone: () -> Unit,
    onMeasureHeartRate: () -> Unit,
    onMeasureBloodPressure: () -> Unit = {},
    onMeasureSpO2: () -> Unit = {},
    onMeasureStress: () -> Unit = {},
    onRequestSleep: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Success card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SuccessGreen.copy(alpha = 0.2f),
                            SuccessGreen.copy(alpha = 0.05f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Ring Connected!",
                        style = MaterialTheme.typography.titleMedium,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                    uiState.connectedRing?.let { ring ->
                        Text(
                            text = "${ring.name} • ${ring.macAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Scrollable health data section
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Battery
            item {
                BatteryCard(batteryLevel = uiState.batteryLevel)
            }
            
            // Heart Rate
            item {
                HeartRateCard(
                    heartRate = uiState.heartRate,
                    isMeasuring = uiState.ringData?.heartRateMeasuring == true
                )
            }
            item {
                MeasurementButton(
                    text = if (uiState.ringData?.heartRateMeasuring == true) "Measuring HR..." else "Measure Heart Rate",
                    icon = Icons.Default.Favorite,
                    color = ErrorRed,
                    onClick = onMeasureHeartRate,
                    enabled = uiState.ringData?.heartRateMeasuring != true
                )
            }
            
            // Blood Pressure
            item {
                BloodPressureCard(
                    systolic = uiState.ringData?.bloodPressureSystolic ?: 0,
                    diastolic = uiState.ringData?.bloodPressureDiastolic ?: 0,
                    isMeasuring = uiState.ringData?.bloodPressureMeasuring == true
                )
            }
            item {
                MeasurementButton(
                    text = if (uiState.ringData?.bloodPressureMeasuring == true) "Measuring BP..." else "Measure Blood Pressure",
                    icon = Icons.Default.FavoriteBorder,
                    color = PrimaryPurple,
                    onClick = onMeasureBloodPressure,
                    enabled = uiState.ringData?.bloodPressureMeasuring != true
                )
            }
            
            // Blood Oxygen (SpO2)
            item {
                SpO2Card(
                    spO2 = uiState.ringData?.spO2 ?: 0f,
                    isMeasuring = uiState.ringData?.spO2Measuring == true
                )
            }
            item {
                MeasurementButton(
                    text = if (uiState.ringData?.spO2Measuring == true) "Measuring SpO2..." else "Measure Blood Oxygen",
                    icon = Icons.Default.ThumbUp,
                    color = AccentCyan,
                    onClick = onMeasureSpO2,
                    enabled = uiState.ringData?.spO2Measuring != true
                )
            }
            
            // Stress Level
            item {
                StressCard(
                    stress = uiState.ringData?.stress ?: 0,
                    onMeasureClick = onMeasureStress
                )
            }
            
            // Sleep Data
            item {
                SleepCard(
                    sleepData = uiState.ringData?.sleepData,
                    onRequestSleep = onRequestSleep
                )
            }
            
            // Steps
            item {
                StepsCard(steps = uiState.steps)
            }
            
            // Spacer for bottom buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryButton(
                text = "Disconnect",
                onClick = onDisconnect,
                modifier = Modifier.weight(1f)
            )
            PremiumButton(
                text = "Done",
                onClick = onDone,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MeasurementButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.8f),
            disabledContainerColor = color.copy(alpha = 0.3f)
        )
    ) {
        if (!enabled) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@Composable
private fun BloodPressureCard(systolic: Int, diastolic: Int, isMeasuring: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GlassWhite
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Blood Pressure",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = if (isMeasuring) "Measuring..." 
                           else if (systolic > 0) "$systolic/$diastolic mmHg" 
                           else "-- / -- mmHg",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isMeasuring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PrimaryPurple,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun SpO2Card(spO2: Float, isMeasuring: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GlassWhite
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Blood Oxygen (SpO2)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = if (isMeasuring) "Measuring..." 
                           else if (spO2 > 0) "$spO2%" 
                           else "--%",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isMeasuring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AccentCyan,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun StressCard(stress: Int, onMeasureClick: () -> Unit = {}) {
    val stressColor = when {
        stress <= 30 -> SuccessGreen
        stress <= 60 -> WarningAmber
        else -> ErrorRed
    }
    val stressLabel = when {
        stress <= 30 -> "Low"
        stress <= 60 -> "Medium"
        else -> "High"
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GlassWhite
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(stressColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = stressColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stress Level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (stress > 0) "$stress" else "--",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (stress > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stressLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = stressColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            // Measure Button
            FilledTonalIconButton(
                onClick = onMeasureClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = AccentCyan.copy(alpha = 0.15f),
                    contentColor = AccentCyan
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Measure Stress",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ManualEntryContent(
    macAddress: String,
    onMacChange: (String) -> Unit,
    onConnect: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Manual MAC Entry",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter the MAC address of your ring (found in Settings)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = macAddress,
            onValueChange = onMacChange,
            label = { Text("MAC Address") },
            placeholder = { Text("XX:XX:XX:XX:XX:XX") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryPurple
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )
            PremiumButton(
                text = "Connect",
                onClick = onConnect,
                modifier = Modifier.weight(1f),
                enabled = macAddress.length >= 17
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREMIUM BUTTONS
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryPurple,
            disabledContainerColor = PrimaryPurple.copy(alpha = 0.3f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TextPrimary
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEWS - Only using default RingUiState constructor
// ═══════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400, heightDp = 800)
@Composable
private fun SetupHeaderPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(20.dp)) {
            PremiumSetupHeader()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400)
@Composable
private fun ButtonsPreview() {
    FitnessAppTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumButton(text = "Primary Button", onClick = {})
            PremiumButton(text = "With Icon", onClick = {}, icon = Icons.Default.Settings)
            PremiumButton(text = "Loading", onClick = {}, isLoading = true)
            PremiumButton(text = "Disabled", onClick = {}, enabled = false)
            SecondaryButton(text = "Secondary", onClick = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 400)
@Composable
private fun ConnectingPreview() {
    FitnessAppTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            ConnectingContent()
        }
    }
}
