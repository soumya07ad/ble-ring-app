package com.fitness.app.ble

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Example Activity/Fragment integration showing:
 * 1. Permission request on "Scan & Connect" button click
 * 2. Scan start after permission grant
 * 3. Device list rendering
 * 4. Device connection
 */
@Composable
fun BleIntegrationExample(
    viewModel: BleViewModel = viewModel(),
    onDeviceConnected: (BleDevice) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Observe states
    val permissionState by viewModel.permissionState.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var selectedDevice by remember { mutableStateOf<BleDevice?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        
        // Check if we should show rationale (if user can still be asked)
        val shouldShowRationale = if (activity != null) {
            viewModel.getRequiredPermissions().any { permission ->
                activity.shouldShowRequestPermissionRationale(permission)
            }
        } else {
            true
        }

        viewModel.onPermissionResult(allGranted, shouldShowRationale)

        // Auto-start scan if granted
        if (allGranted) {
            viewModel.startScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7FF))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            "BLE Device Scanner",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )

        // Permission Status Card
        PermissionStatusCard(permissionState)

        // Scan Button - ONLY triggers permission request
        Button(
            onClick = {
                // Check permissions first
                if (viewModel.checkPermissions(context)) {
                    // Already granted, start scan
                    viewModel.startScan()
                } else {
                    // Request permissions
                    permissionLauncher.launch(viewModel.getRequiredPermissions())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFA855F7)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = permissionState != PermissionState.PermanentlyDenied
        ) {
            Icon(
                imageVector = if (scanState is ScanState.Scanning) Icons.Filled.Refresh else Icons.Filled.Search,
                contentDescription = "Scan",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                when (scanState) {
                    is ScanState.Scanning -> "Scanning..."
                    else -> "Scan & Connect"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Permanently Denied - Show Settings Button
        if (permissionState == PermissionState.PermanentlyDenied) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                )
            ) {
                Text("Open Settings to Grant Permissions")
            }
        }

        // Scan State Display
        when (val state = scanState) {
            is ScanState.Scanning -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            is ScanState.DevicesFound -> {
                Text(
                    "Found ${state.devices.size} device(s)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.devices) { device ->
                        BleDeviceCard(
                            device = device,
                            isSelected = selectedDevice?.deviceMac == device.deviceMac,
                            isConnected = connectionState is ConnectionState.Connected && selectedDevice?.deviceMac == device.deviceMac,
                            onSelect = { selectedDevice = device },
                            onConnect = {
                                viewModel.connectToDevice(device)
                            }
                        )
                    }
                }
            }
            is ScanState.Error -> {
                ErrorCard(state.message)
            }
            else -> {
                // Idle state
            }
        }

        // Connection Status
        ConnectionStatusCard(connectionState, selectedDevice)
    }
}

@Composable
private fun PermissionStatusCard(permissionState: PermissionState) {
    val (color, icon, text) = when (permissionState) {
        PermissionState.Granted -> Triple(Color(0xFF22C55E), Icons.Filled.Check, "Permissions Granted")
        PermissionState.Denied -> Triple(Color(0xFFF59E0B), Icons.Filled.Warning, "Permissions Denied")
        PermissionState.PermanentlyDenied -> Triple(Color(0xFFEF4444), Icons.Filled.Warning, "Permissions Permanently Denied")
        PermissionState.NotRequested -> Triple(Color(0xFF6B7280), Icons.Filled.Search, "Tap 'Scan & Connect' to begin")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun BleDeviceCard(
    device: BleDevice,
    isSelected: Boolean,
    isConnected: Boolean,
    onSelect: () -> Unit,
    onConnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) Color(0xFFEDE9FE) else Color.White,
        shape = RoundedCornerShape(12.dp),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.deviceName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    device.deviceMac,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "RSSI: ${device.rssi} dBm • ${device.signalQuality}",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            if (isSelected && !isConnected) {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA855F7)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Connect", fontSize = 12.sp)
                }
            }

            if (isConnected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Connected",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(connectionState: ConnectionState, device: BleDevice?) {
    if (connectionState !is ConnectionState.Disconnected && device != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = when (connectionState) {
                is ConnectionState.Connected -> Color(0xFFF0FDF4)
                is ConnectionState.Connecting -> Color(0xFFFEF3C7)
                is ConnectionState.Error -> Color(0xFFFEE2E2)
                else -> Color(0xFFF3F4F6)
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (connectionState) {
                    is ConnectionState.Connecting -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    is ConnectionState.Connected -> Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(24.dp)
                    )
                    else -> {}
                }

                Column {
                    Text(
                        when (connectionState) {
                            is ConnectionState.Connecting -> "Connecting to ${device.deviceName}..."
                            is ConnectionState.Connected -> "Connected to ${device.deviceName}"
                            is ConnectionState.Error -> "Connection Error: ${connectionState.message}"
                            else -> ""
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFEE2E2),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(24.dp)
            )
            Text(
                message,
                fontSize = 14.sp,
                color = Color(0xFFEF4444)
            )
        }
    }
}
