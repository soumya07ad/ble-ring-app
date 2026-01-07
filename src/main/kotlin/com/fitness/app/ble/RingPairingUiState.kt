package com.fitness.app.ble

/**
 * Unified UI state for Smart Ring pairing screen
 * Combines permission, scan, and connection states into a single observable state
 * 
 * WHY: Single state simplifies UI logic - one observer instead of three
 * WHY: Sealed class ensures exhaustive when statements (compile-time safety)
 * WHY: Each state contains exactly what UI needs to display
 */
sealed class RingPairingUiState {
    
    /**
     * Initial state - no action taken yet
     * UI shows: Welcome message with "Scan & Connect" button
     */
    object Idle : RingPairingUiState()
    
    /**
     * Permissions not granted - user needs to approve
     * UI shows: Permission rationale or settings prompt
     */
    data class PermissionRequired(
        val isPermanentlyDenied: Boolean = false
    ) : RingPairingUiState()
    
    /**
     * BLE scan in progress
     * UI shows: Loading indicator, "Scanning..." text, disabled button
     * WHY separate from Idle: Prevents double-scan clicks
     */
    object Scanning : RingPairingUiState()
    
    /**
     * Devices found during scan
     * UI shows: List of discovered devices
     * WHY list included: UI needs device names/MACs to display
     */
    data class DevicesFound(
        val devices: List<BleDevice>
    ) : RingPairingUiState()
    
    /**
     * No devices found after scan timeout
     * UI shows: "No devices found" message with "Scan Again" button
     * WHY separate from Error: Different user action required
     */
    object NoDeviceFound : RingPairingUiState()
    
    /**
     * Connection attempt in progress
     * UI shows: "Connecting..." with loading indicator
     * WHY deviceName included: Shows which device is being connected
     */
    data class Connecting(
        val deviceName: String,
        val deviceMac: String
    ) : RingPairingUiState()
    
    /**
     * Successfully connected to ring
     * UI shows: Success message, device info, "Disconnect" option
     * WHY deviceName included: Confirms which device is connected
     */
    data class Connected(
        val deviceName: String,
        val deviceMac: String
    ) : RingPairingUiState()
    
    /**
     * Ring was connected but now disconnected
     * UI shows: "Ring Disconnected" message with reconnect option
     * WHY separate from Idle: User knows connection was lost (not first time)
     */
    object Disconnected : RingPairingUiState()
    
    /**
     * Error occurred during scan or connection
     * UI shows: Error message with retry option
     * WHY message included: Helps debugging (permission denied, timeout, etc.)
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : RingPairingUiState()
}
