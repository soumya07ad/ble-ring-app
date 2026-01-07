package com.fitness.app

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import com.fitness.app.ble.*
import kotlin.random.Random

@Composable
fun SmartRingSetupScreen(
    onSetupComplete: () -> Unit,
    onSkip: () -> Unit,
    bleViewModel: BleViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Observe BLE states
    val permissionState by bleViewModel.permissionState.collectAsState()
    val bleScanState by bleViewModel.scanState.collectAsState()
    val bleConnectionState by bleViewModel.connectionState.collectAsState()

    // Existing state
    var discoveredDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var isPairing by remember { mutableStateOf(false) }
    var pairingSuccess by remember { mutableStateOf(false) }
    var pairingMethod by remember { mutableStateOf<PairingMethod?>(null) }
    var showAdvancedOptions by remember { mutableStateOf(false) }

    // Permission launcher for BLE
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        val shouldShowRationale = activity?.let { act ->
            bleViewModel.getRequiredPermissions().any { 
                act.shouldShowRequestPermissionRationale(it) 
            }
        } ?: true
        
        bleViewModel.onPermissionResult(allGranted, shouldShowRationale)
        
        if (allGranted) {
            bleViewModel.startScan()
            pairingMethod = PairingMethod.SCAN
        }
    }

    // Sync BLE scan results to local state for existing UI
    LaunchedEffect(bleScanState) {
        when (val state = bleScanState) {
            is ScanState.DevicesFound -> {
                discoveredDevices = state.devices.map { bleDevice ->
                    BluetoothDevice(
                        bleDevice.deviceName,
                        bleDevice.deviceMac,
                        bleDevice.rssi
                    )
                }
            }
            is ScanState.Error -> {
                discoveredDevices = emptyList()
            }
            else -> {}
        }
    }

    // Handle connection success
    LaunchedEffect(bleConnectionState) {
        if (bleConnectionState is ConnectionState.Connected && selectedDevice != null) {
            pairingSuccess = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Smart Ring",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFA855F7)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Pair Your Smart Ring",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Connect your fitness tracker for real-time health data",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }

            // Content
            if (pairingSuccess) {
                // Success State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Success",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF22C55E)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Ring Paired Successfully!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${selectedDevice?.name} • ${selectedDevice?.address}",
                        fontSize = 14.sp,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Battery: 85% • Last sync: Just now",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            } else if (pairingMethod == null) {
                // Pairing Method Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Choose Pairing Method",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    PairingMethodCard(
                        icon = Icons.Filled.Search,
                        title = "Scan & Connect",
                        description = "Auto-scan for nearby devices",
                        onClick = {
                            // Check permissions first
                            if (bleViewModel.checkPermissions(context)) {
                                bleViewModel.startScan()
                                pairingMethod = PairingMethod.SCAN
                            } else {
                                // Request permissions
                                permissionLauncher.launch(bleViewModel.getRequiredPermissions())
                            }
                        },
                        color = Color(0xFFA855F7)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PairingMethodCard(
                        icon = Icons.Filled.Search,
                        title = "QR Code",
                        description = "Scan QR code from device",
                        onClick = { pairingMethod = PairingMethod.QR_CODE },
                        color = Color(0xFF3B82F6)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PairingMethodCard(
                        icon = Icons.Filled.Info,
                        title = "Manual Entry",
                        description = "Enter device details manually",
                        onClick = { pairingMethod = PairingMethod.MANUAL },
                        color = Color(0xFF10B981)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PairingMethodCard(
                        icon = Icons.Filled.Close,
                        title = "NFC Tap",
                        description = "Tap device to phone NFC",
                        onClick = { pairingMethod = PairingMethod.NFC },
                        color = Color(0xFFF59E0B)
                    )
                }
            } else if (pairingMethod == PairingMethod.SCAN) {
                // Scan Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (bleScanState is ScanState.Scanning) {
                                bleViewModel.stopScan()
                            } else {
                                bleViewModel.startScan()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA855F7)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Scan",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (bleScanState is ScanState.Scanning) "Scanning..." else "Scan for Devices",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Permission status messages
                    if (permissionState == PermissionState.Denied) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Bluetooth permissions required to scan",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    if (permissionState == PermissionState.PermanentlyDenied) {
                        Spacer(modifier = Modifier.height(8.dp))
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
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Settings to Grant Permissions", fontSize = 12.sp)
                        }
                    }

                    if (discoveredDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Available Devices",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(discoveredDevices) { device ->
                                DeviceCard(
                                    device = device,
                                    isSelected = selectedDevice?.address == device.address,
                                    onSelect = { selectedDevice = device }
                                )
                            }
                        }
                    }
                }
            } else if (pairingMethod == PairingMethod.QR_CODE) {
                // QR Code Pairing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF3B82F6)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Position QR Code in viewfinder",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Scanning will begin automatically",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        color = Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "[QR Code Preview]",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "QR code will be scanned from your smart ring",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }
            } else if (pairingMethod == PairingMethod.MANUAL) {
                // Manual Entry
                var deviceName by remember { mutableStateOf("") }
                var macAddress by remember { mutableStateOf("") }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Enter Device Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("Device Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = macAddress,
                        onValueChange = { macAddress = it },
                        label = { Text("MAC Address (XX:XX:XX:XX:XX:XX)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (deviceName.isNotEmpty() && macAddress.isNotEmpty()) {
                                selectedDevice = BluetoothDevice(deviceName, macAddress, -50)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = deviceName.isNotEmpty() && macAddress.isNotEmpty()
                    ) {
                        Text("Confirm Device", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (pairingMethod == PairingMethod.NFC) {
                // NFC Pairing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "NFC",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Ready for NFC Tap",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Hold your smart ring near the back of your phone",
                        fontSize = 14.sp,
                        color = Color(0xFF92400E),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Waiting for device...",
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (pairingMethod != null) {
                            pairingMethod = null
                            selectedDevice = null
                            discoveredDevices = emptyList()
                           // isScanning = false
                        } else {
                            onSkip()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE5E7EB)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (pairingMethod != null) "Back" else "Skip",
                        color = Color(0xFF4B5563),
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (selectedDevice != null && bleConnectionState !is ConnectionState.Connected) {
                            // Connect to device using BLE
                            bleViewModel.connectToDevice(
                                BleDevice(
                                    selectedDevice!!.name,
                                    selectedDevice!!.address,
                                    selectedDevice!!.signalStrength
                                )
                            )
                        } else if (bleConnectionState is ConnectionState.Connected) {
                            // Connection successful, save and proceed
                            val ring = SmartRing(
                                name = selectedDevice!!.name,
                                macAddress = selectedDevice!!.address,
                                batteryLevel = 85, // Will be updated from real device later
                                isConnected = true
                            )
                            AppState.setPairedRing(ring)
                            AppState.setSetupComplete(true)
                            onSetupComplete()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = selectedDevice != null || bleConnectionState is ConnectionState.Connected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA855F7),
                        disabledContainerColor = Color(0xFFD1D5DB)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    when (bleConnectionState) {
                        is ConnectionState.Connecting -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        is ConnectionState.Connected -> {
                            Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Text("Pair Device", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: BluetoothDevice,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        color = if (isSelected) Color(0xFFEDE9FE) else Color.White,
        shape = RoundedCornerShape(12.dp),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    device.address,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Signal",
                    fontSize = 10.sp,
                    color = Color(0xFF9CA3AF)
                )
                Text(
                    "${device.signalStrength} dBm",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (device.signalStrength > -60) Color(0xFF22C55E) else Color(0xFFF97316)
                )
            }
        }
    }
}

@Composable
private fun PairingMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(28.dp),
                        tint = color
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

enum class PairingMethod {
    SCAN,
    QR_CODE,
    MANUAL,
    NFC
}
