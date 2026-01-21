package com.fitness.app.data.repository

import android.content.Context
import com.fitness.app.ble.BleDevice
import com.fitness.app.ble.SdkBleManager
import com.fitness.app.ble.BleConnectionState
import com.fitness.app.ble.RingData
import com.fitness.app.ble.ScanState
import com.fitness.app.core.util.Result
import com.fitness.app.domain.model.ConnectionStatus
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.RingHealthData
import com.fitness.app.domain.model.ScanStatus
import com.fitness.app.domain.repository.IRingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Implementation of IRingRepository
 * Now uses SdkBleManager (full YC SDK approach)
 * 
 * This uses the SDK for all operations: connection, battery, steps, and heart rate
 */
class RingRepositoryImpl(
    private val context: Context
) : IRingRepository {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Use SdkBleManager singleton (full SDK approach)
    private val sdkManager: SdkBleManager by lazy { 
        SdkBleManager.getInstance(context)
    }
    
    // Domain state flows
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    override val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()
    
    private val _ringData = MutableStateFlow(RingHealthData())
    override val ringData: StateFlow<RingHealthData> = _ringData.asStateFlow()
    
    // Track connected ring
    private var connectedRing: Ring? = null
    
    init {
        sdkManager.initialize()
        observeSdkManagerStates()
    }
    
    /**
     * Observe SdkBleManager states and map to domain states
     */
    private fun observeSdkManagerStates() {
        scope.launch {
            // Observe connection state
            sdkManager.connectionState.collect { state ->
                _connectionStatus.value = mapConnectionState(state)
            }
        }
        
        scope.launch {
            // Observe ring data
            sdkManager.ringData.collect { data ->
                _ringData.value = mapRingData(data)
            }
        }
        
        // Observe scan results
        scope.launch {
            sdkManager.scanResults.collect { devices ->
                if (_scanStatus.value is ScanStatus.Scanning && devices.isNotEmpty()) {
                    _scanStatus.value = ScanStatus.DevicesFound(devices)
                }
            }
        }
    }
    
    /**
     * Map SDK BleConnectionState to domain ConnectionStatus
     */
    private fun mapConnectionState(state: BleConnectionState): ConnectionStatus {
        return when (state) {
            is BleConnectionState.Disconnected -> ConnectionStatus.Disconnected
            is BleConnectionState.Connecting -> ConnectionStatus.Connecting
            is BleConnectionState.Connected -> {
                connectedRing = state.ring
                ConnectionStatus.Connected(state.ring)
            }
        }
    }
    

    
    /**
     * Map BleManager RingData to domain RingHealthData
     */
    private fun mapRingData(data: RingData): RingHealthData {
        return RingHealthData(
            battery = data.battery,
            heartRate = data.heartRate,
            steps = data.steps,
            distance = data.distance,
            calories = data.calories,
            spO2 = data.spO2,
            deepSleep = data.deepSleep,
            lightSleep = data.lightSleep,
            lastUpdate = data.lastUpdate
        )
    }
    
    override fun initialize() {
        // Initialize SDK manager (already done in init block)
    }
    
    override suspend fun startScan(durationSeconds: Int): Result<List<Ring>> {
        return try {
            _scanStatus.value = ScanStatus.Scanning
            
            // Start SDK scan
            sdkManager.startScan(durationSeconds)
            
            // Wait for duration or results
            delay(durationSeconds * 1000L)
            
            // Get final results
            val devices = sdkManager.scanResults.value
            _scanStatus.value = if (devices.isNotEmpty()) {
                ScanStatus.DevicesFound(devices)
            } else {
                ScanStatus.Idle
            }
            
            Result.success(devices)
        } catch (e: Exception) {
            _scanStatus.value = ScanStatus.Error(e.message ?: "Scan failed")
            Result.error("Scan failed: ${e.message}", e)
        }
    }
    
    override fun stopScan() {
        sdkManager.stopScan()
        _scanStatus.value = ScanStatus.Idle
    }
    
    override suspend fun connect(macAddress: String, deviceName: String?): Result<Ring> {
        return try {
            val ring = Ring(
                macAddress = macAddress,
                name = deviceName ?: "R9 Ring",
                isConnected = false
            )
            
            connectedRing = ring
            _connectionStatus.value = ConnectionStatus.Connecting
            
            sdkManager.connectToDevice(macAddress)
            
            // Wait for connection (max 15 seconds)
            val connected = withTimeoutOrNull(15000L) {
                while (sdkManager.connectionState.value !is BleConnectionState.Connected) {
                    delay(100)
                }
                true
            } ?: false
            
            if (connected) {
                connectedRing = ring.copy(isConnected = true)
                Result.success(connectedRing!!)
            } else {
                connectedRing = null
                Result.error("Connection timed out")
            }
        } catch (e: Exception) {
            connectedRing = null
            Result.error("Connection failed: ${e.message}", e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> {
        return try {
            sdkManager.disconnect()
            connectedRing = null
            _connectionStatus.value = ConnectionStatus.Disconnected
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error("Disconnect failed: ${e.message}", e)
        }
    }
    
    override suspend fun getBattery(): Result<Int> {
        val battery = _ringData.value.battery
        return if (battery != null && battery > 0) {
            Result.success(battery)
        } else {
            Result.error("Battery data not available")
        }
    }
    
    override fun isConnected(): Boolean {
        return sdkManager.connectionState.value is BleConnectionState.Connected
    }
    
    override fun getConnectedRing(): Ring? = connectedRing
    
    /**
     * Start heart rate measurement via SDK
     */
    override fun startHeartRateMeasurement() {
        sdkManager.startHeartRateMeasurement()
    }
    
    /**
     * Stop heart rate measurement via SDK
     */
    override fun stopHeartRateMeasurement() {
        sdkManager.stopHeartRateMeasurement()
    }
    
    companion object {
        @Volatile
        private var INSTANCE: RingRepositoryImpl? = null
        
        fun getInstance(context: Context): RingRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RingRepositoryImpl(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

/**
 * Extension function to convert BleDevice to domain Ring
 */
private fun BleDevice.toDomain(): Ring {
    return Ring(
        macAddress = deviceMac,
        name = deviceName,
        rssi = rssi,
        isConnected = false
    )
}
