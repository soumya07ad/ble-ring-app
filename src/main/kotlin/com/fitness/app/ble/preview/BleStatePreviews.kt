package com.fitness.app.ble.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitness.app.ble.BleDevice
import com.fitness.app.ble.RingPairingUiState

/**
 * Compose Previews for all BLE Connection States
 * View these in Android Studio's Design/Split view
 */

// ============================================
// IDLE STATE
// ============================================
@Preview(name = "1. Idle State", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewIdleState() {
    SmartRingSetupPreview {
        IdleStateContent(
            onScanClick = {},
            onSkip = {}
        )
    }
}

@Composable
private fun IdleStateContent(onScanClick: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Turn on your smart ring and keep it nearby to connect.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFA855F7)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan & Connect", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onSkip) {
            Text("Skip for now", color = Color(0xFF6B7280))
        }
    }
}

// ============================================
// SCANNING STATE
// ============================================
@Preview(name = "2. Scanning State", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewScanningState() {
    SmartRingSetupPreview {
        ScanningStateContent()
    }
}

@Composable
private fun ScanningStateContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = Color(0xFFA855F7),
            strokeWidth = 4.dp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Scanning for nearby rings…",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Make sure your ring is powered on and in pairing mode.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

// ============================================
// DEVICES FOUND STATE
// ============================================
@Preview(name = "3. Devices Found", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewDevicesFoundState() {
    SmartRingSetupPreview {
        DevicesFoundStateContent(
            devices = listOf(
                BleDevice("YC Smart Ring", "AA:BB:CC:DD:EE:01", -45),
                BleDevice("Fitness Ring Pro", "AA:BB:CC:DD:EE:02", -60),
                BleDevice("Health Band", "AA:BB:CC:DD:EE:03", -75)
            ),
            onDeviceSelected = {}
        )
    }
}

@Composable
private fun DevicesFoundStateContent(
    devices: List<BleDevice>,
    onDeviceSelected: (BleDevice) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Found ${devices.size} device${if (devices.size != 1) "s" else ""}",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        devices.forEach { device ->
            DeviceCard(device = device, onClick = { onDeviceSelected(device) })
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DeviceCard(device: BleDevice, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = Color(0xFFA855F7),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.deviceName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
                Text(
                    device.deviceMac,
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
            Text(
                "${device.rssi} dBm",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

// ============================================
// NO DEVICE FOUND STATE
// ============================================
@Preview(name = "4. No Device Found", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewNoDeviceFoundState() {
    SmartRingSetupPreview {
        NoDeviceFoundStateContent(onScanAgain = {})
    }
}

@Composable
private fun NoDeviceFoundStateContent(onScanAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFF59E0B)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "No devices found",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Make sure your ring is nearby and in pairing mode, then try again.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onScanAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFA855F7)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Again", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ============================================
// CONNECTING STATE
// ============================================
@Preview(name = "5. Connecting State", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewConnectingState() {
    SmartRingSetupPreview {
        ConnectingStateContent(deviceName = "YC Smart Ring")
    }
}

@Composable
private fun ConnectingStateContent(deviceName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = Color(0xFFA855F7),
            strokeWidth = 4.dp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Connecting to ring…",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Please wait, this may take a few seconds.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Device: $deviceName",
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}

// ============================================
// CONNECTED STATE
// ============================================
@Preview(name = "6. Connected State", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewConnectedState() {
    SmartRingSetupPreview {
        ConnectedStateContent(
            deviceName = "YC Smart Ring",
            onDisconnectClick = {},
            onSetupComplete = {}
        )
    }
}

@Composable
private fun ConnectedStateContent(
    deviceName: String,
    onDisconnectClick: () -> Unit,
    onSetupComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF22C55E)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Ring Connected",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF166534)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Your smart ring is now connected and ready to use.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF0FDF4),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Device: $deviceName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF166534)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onSetupComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22C55E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onDisconnectClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Disconnect", color = Color(0xFF6B7280))
        }
    }
}

// ============================================
// DISCONNECTED STATE
// ============================================
@Preview(name = "7. Disconnected State", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewDisconnectedState() {
    SmartRingSetupPreview {
        DisconnectedStateContent(onReconnect = {})
    }
}

@Composable
private fun DisconnectedStateContent(onReconnect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFEF4444)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Ring Disconnected",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Tap below to reconnect your ring.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onReconnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFA855F7)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Scan & Connect", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ============================================
// ERROR STATE
// ============================================
@Preview(name = "8. Error State", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewErrorState() {
    SmartRingSetupPreview {
        ErrorStateContent(
            message = "Connection timeout. Please try again.",
            onRetry = {}
        )
    }
}

@Composable
private fun ErrorStateContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFEF4444)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Connection Error",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            message,
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ============================================
// PERMISSION REQUIRED STATE
// ============================================
@Preview(name = "9. Permission Required", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewPermissionRequiredState() {
    SmartRingSetupPreview {
        PermissionRequiredStateContent(
            isPermanentlyDenied = false,
            onRequestPermission = {}
        )
    }
}

@Composable
private fun PermissionRequiredStateContent(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFF59E0B)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            if (isPermanentlyDenied) "Permissions Denied" else "Permissions Required",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            if (isPermanentlyDenied) {
                "Please enable Bluetooth permissions in Settings to scan for devices."
            } else {
                "Bluetooth permissions are required to scan for nearby devices."
            },
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPermanentlyDenied) Color(0xFFEF4444) else Color(0xFFA855F7)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (isPermanentlyDenied) "Open Settings" else "Grant Permissions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ============================================
// PREVIEW WRAPPER
// ============================================
@Composable
private fun SmartRingSetupPreview(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            "Pair Your Smart Ring",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Dynamic content
        content()
    }
}
