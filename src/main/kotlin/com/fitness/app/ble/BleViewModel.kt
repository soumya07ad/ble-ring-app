package com.fitness.app.ble

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for BLE operations
 * Handles permission logic and exposes BLE states to UI
 */
class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = BleManager.getInstance(application)

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    val scanState: StateFlow<ScanState> = bleManager.scanState
    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState

    // Unified UI state for simplified UI observation
    private val _uiState = MutableStateFlow<RingPairingUiState>(RingPairingUiState.Idle)
    val uiState: StateFlow<RingPairingUiState> = _uiState.asStateFlow()

    private var selectedDeviceName: String? = null
    private var selectedDeviceMac: String? = null

    init {
        bleManager.initialize()
        
        // Combine states into unified UI state
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                _permissionState,
                scanState,
                connectionState
            ) { permission, scan, connection ->
                computeUiState(permission, scan, connection)
            }.collect { _uiState.value = it }
        }
    }

    private fun computeUiState(
        permission: PermissionState,
        scan: ScanState,
        connection: ConnectionState
    ): RingPairingUiState {
        when (connection) {
            is ConnectionState.Connecting -> return RingPairingUiState.Connecting(
                selectedDeviceName ?: "Smart Ring", selectedDeviceMac ?: ""
            )
            is ConnectionState.Connected -> return RingPairingUiState.Connected(
                selectedDeviceName ?: "Smart Ring", selectedDeviceMac ?: ""
            )
            is ConnectionState.Disconnected -> if (selectedDeviceMac != null) {
                return RingPairingUiState.Disconnected
            }
            is ConnectionState.Error -> return RingPairingUiState.Error(connection.message, true)
            else -> {}
        }
        when (scan) {
            is ScanState.Scanning -> return RingPairingUiState.Scanning
            is ScanState.DevicesFound -> return if (scan.devices.isEmpty()) {
                RingPairingUiState.NoDeviceFound
            } else {
                RingPairingUiState.DevicesFound(scan.devices)
            }
            is ScanState.Error -> return if (scan.message.contains("No devices", true)) {
                RingPairingUiState.NoDeviceFound
            } else {
                RingPairingUiState.Error(scan.message, true)
            }
            else -> {}
        }
        when (permission) {
            is PermissionState.Denied, is PermissionState.NotRequested -> {
                return RingPairingUiState.PermissionRequired(false)
            }
            is PermissionState.PermanentlyDenied -> {
                return RingPairingUiState.PermissionRequired(true)
            }
            else -> {}
        }
        return RingPairingUiState.Idle
    }

    /**
     * Get required Bluetooth permissions based on Android version
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires new runtime permissions
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // Pre-Android 12
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * Check if all required permissions are granted
     */
    fun checkPermissions(context: Context): Boolean {
        val permissions = getRequiredPermissions()
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        _permissionState.value = if (allGranted) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }

        return allGranted
    }

    /**
     * Handle permission result from Activity
     * @param granted Whether permissions were granted
     * @param shouldShowRationale Whether to show rationale (if false after denial = permanently denied)
     */
    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean = true) {
        _permissionState.value = when {
            granted -> PermissionState.Granted
            !shouldShowRationale -> PermissionState.PermanentlyDenied
            else -> PermissionState.Denied
        }
    }

    /**
     * Start BLE scan - Only if permissions are granted
     * Call this AFTER permission check
     */
    fun startScan() {
        viewModelScope.launch {
            if (_permissionState.value != PermissionState.Granted) {
                // Permission not granted, do not scan
                return@launch
            }
            bleManager.startScan()
        }
    }

    /**
     * Connect to selected device from scan results
     */
    fun connectToDevice(device: BleDevice) {
        viewModelScope.launch {
            if (_permissionState.value != PermissionState.Granted) {
                return@launch
            }
            selectedDeviceName = device.deviceName
            selectedDeviceMac = device.deviceMac
            // Pass device name for NULL validation
            bleManager.connectDevice(device.deviceMac, device.rssi, device.deviceName)
        }
    }
    
    /**
     * Connect using manual MAC address entry
     * WHY: Fallback when scan is unreliable or ring already paired elsewhere
     * @param deviceName User-defined name for display
     * @param macAddress BLE MAC address (validated format)
     */
    fun connectByMacAddress(deviceName: String, macAddress: String) {
        viewModelScope.launch {
            if (_permissionState.value != PermissionState.Granted) {
                return@launch
            }
            
            // Validate MAC format
            if (!MacAddressValidator.isValidMacAddress(macAddress)) {
                android.util.Log.e("BleViewModel", "Invalid MAC address format: $macAddress")
                return@launch
            }
            
            selectedDeviceName = deviceName
            selectedDeviceMac = macAddress
            
            android.util.Log.i("BleViewModel", "Manual MAC connection initiated")
            android.util.Log.i("BleViewModel", "  Device Name: $deviceName")
            android.util.Log.i("BleViewModel", "  MAC Address: $macAddress")
            
            // Connect with manual entry (no RSSI, no device name validation)
            // WHY: Manual entry bypasses scan, so we don't have RSSI or device name from SDK
            bleManager.connectDevice(
                macAddress = macAddress,
                rssi = -50, // Assume good signal for manual entry
                deviceName = deviceName // Use user-provided name
            )
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        viewModelScope.launch {
            bleManager.disconnect()
        }
    }

    /**
     * Stop ongoing scan
     */
    fun stopScan() {
        bleManager.stopScan()
    }

    /**
     * Check if device is currently connected
     */
    fun isConnected(): Boolean {
        return bleManager.isConnected()
    }
}
