package com.fitness.app.presentation.ring

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.core.di.AppContainer
import com.fitness.app.core.util.Result
import com.fitness.app.domain.model.ConnectionStatus
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.ScanStatus
import com.fitness.app.domain.usecase.ConnectRingUseCase
import com.fitness.app.domain.usecase.DisconnectRingUseCase
import com.fitness.app.domain.usecase.GetRingDataUseCase
import com.fitness.app.domain.usecase.ScanDevicesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Ring feature following MVVM pattern
 * 
 * Uses Use Cases for business logic and exposes a single UI state
 */
class RingViewModel(application: Application) : AndroidViewModel(application) {
    
    // Get dependencies from DI container
    private val container = AppContainer.getInstance(application)
    private val scanDevicesUseCase: ScanDevicesUseCase = container.scanDevicesUseCase
    private val connectRingUseCase: ConnectRingUseCase = container.connectRingUseCase
    private val disconnectRingUseCase: DisconnectRingUseCase = container.disconnectRingUseCase
    private val getRingDataUseCase: GetRingDataUseCase = container.getRingDataUseCase
    
    // UI State
    private val _uiState = MutableStateFlow(RingUiState())
    val uiState: StateFlow<RingUiState> = _uiState.asStateFlow()
    
    init {
        // Initialize repository
        container.ringRepository.initialize()
        
        // Observe repository states
        observeRepositoryStates()
    }
    
    /**
     * Observe repository state flows and update UI state
     */
    private fun observeRepositoryStates() {
        viewModelScope.launch {
            container.ringRepository.connectionStatus.collect { status ->
                _uiState.update { it.copy(
                    connectionStatus = status,
                    connectedRing = (status as? ConnectionStatus.Connected)?.ring
                )}
            }
        }
        
        viewModelScope.launch {
            container.ringRepository.scanStatus.collect { status ->
                _uiState.update { it.copy(
                    scanStatus = status,
                    scannedDevices = status.getDevicesOrEmpty()
                )}
            }
        }
        
        viewModelScope.launch {
            getRingDataUseCase().collect { data ->
                _uiState.update { it.copy(ringData = data) }
            }
        }
        

    
    // ==================== Permission Handling ====================
    
    /**
     * Get required Bluetooth permissions based on Android version
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
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
        val allGranted = getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        _uiState.update { 
            it.copy(permissionState = if (allGranted) PermissionUiState.Granted else PermissionUiState.NotRequested)
        }
        
        return allGranted
    }
    
    /**
     * Handle permission result
     */
    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean = true) {
        val newState = when {
            granted -> PermissionUiState.Granted
            !shouldShowRationale -> PermissionUiState.PermanentlyDenied
            else -> PermissionUiState.Denied
        }
        _uiState.update { it.copy(permissionState = newState) }
    }
    
    // ==================== Scanning ====================
    
    /**
     * Start scanning for devices
     */
    fun startScan() {
        if (!_uiState.value.hasPermissions) {
            _uiState.update { it.copy(errorMessage = "Permissions required") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = scanDevicesUseCase(durationSeconds = 6)) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        scannedDevices = result.data,
                        isLoading = false
                    )}
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        errorMessage = result.message,
                        isLoading = false
                    )}
                }
                is Result.Loading -> {
                    // Already handled by scanStatus flow
                }
            }
        }
    }
    
    /**
     * Stop ongoing scan
     */
    fun stopScan() {
        scanDevicesUseCase.stopScan()
        _uiState.update { it.copy(scanStatus = ScanStatus.Idle) }
    }
    
    // ==================== Connection ====================
    
    /**
     * Connect to a scanned device
     */
    fun connectToDevice(ring: Ring) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = connectRingUseCase.connect(ring)) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        connectedRing = result.data,
                        isLoading = false
                    )}
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        errorMessage = result.message,
                        isLoading = false
                    )}
                }
                is Result.Loading -> {
                    // Handled by connectionStatus flow
                }
            }
        }
    }
    
    /**
     * Connect using manual MAC address entry
     */
    fun connectByMacAddress(macAddress: String, deviceName: String = "Ring") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, showManualEntry = false) }
            
            when (val result = connectRingUseCase(macAddress, deviceName)) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        connectedRing = result.data,
                        isLoading = false
                    )}
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        errorMessage = result.message,
                        isLoading = false
                    )}
                }
                is Result.Loading -> {
                    // Handled by connectionStatus flow
                }
            }
        }
    }
    
    /**
     * Disconnect from current device
     */
    fun disconnect() {
        viewModelScope.launch {
            disconnectRingUseCase()
            _uiState.update { it.copy(
                connectedRing = null,
                connectionStatus = ConnectionStatus.Disconnected
            )}
        }
    }
    
    /**
     * Start heart rate measurement using SDK
     */
    fun startHeartRateMeasurement() {
        // Use SDK for HR measurement (via repository)
        container.ringRepository.startHeartRateMeasurement()
    }
    
    /**
     * Stop heart rate measurement
     */
    fun stopHeartRateMeasurement() {
        container.ringRepository.stopHeartRateMeasurement()
    }
    
    // ==================== UI State Updates ====================
    
    /**
     * Show/hide manual MAC entry
     */
    fun toggleManualEntry() {
        _uiState.update { it.copy(showManualEntry = !it.showManualEntry) }
    }
    
    /**
     * Update manual MAC address
     */
    fun updateManualMacAddress(mac: String) {
        _uiState.update { it.copy(manualMacAddress = mac) }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.clearError() }
    }
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean = _uiState.value.isConnected
}
