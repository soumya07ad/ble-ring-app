package com.fitness.app.presentation.ring

import com.fitness.app.domain.model.ConnectionStatus
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.RingHealthData
import com.fitness.app.domain.model.ScanStatus

/**
 * UI State for Ring feature
 * Single source of truth for all Ring-related UI data
 */
data class RingUiState(
    // Permission state
    val permissionState: PermissionUiState = PermissionUiState.NotRequested,
    
    // Scanning
    val scanStatus: ScanStatus = ScanStatus.Idle,
    val scannedDevices: List<Ring> = emptyList(),
    
    // Connection
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val connectedRing: Ring? = null,
    
    // Ring data
    val ringData: RingHealthData = RingHealthData(),
    
    // UI state
    val showManualEntry: Boolean = false,
    val manualMacAddress: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false
) {
    /**
     * Derived properties for UI convenience
     */
    
    val isScanning: Boolean 
        get() = scanStatus is ScanStatus.Scanning
    
    val isConnecting: Boolean 
        get() = connectionStatus is ConnectionStatus.Connecting
    
    val isConnected: Boolean 
        get() = connectionStatus is ConnectionStatus.Connected
    
    val hasPermissions: Boolean 
        get() = permissionState == PermissionUiState.Granted
    
    val canStartScan: Boolean 
        get() = hasPermissions && !isScanning && !isConnecting && !isConnected
    
    val shouldShowDeviceList: Boolean 
        get() = scannedDevices.isNotEmpty() || isScanning
    
    val batteryLevel: Int? 
        get() = ringData.battery
    
    val heartRate: Int 
        get() = ringData.heartRate
    
    val steps: Int 
        get() = ringData.steps
    
    /**
     * Create a copy with error message cleared
     */
    fun clearError(): RingUiState = copy(errorMessage = null)
}

/**
 * Permission UI states
 */
sealed class PermissionUiState {
    object NotRequested : PermissionUiState()
    object Granted : PermissionUiState()
    object Denied : PermissionUiState()
    object PermanentlyDenied : PermissionUiState()
}
