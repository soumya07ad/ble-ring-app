package com.fitness.app.data.repository

import android.content.Context
import com.fitness.app.ble.BleDevice
import com.fitness.app.ble.NativeGattManager
import com.fitness.app.ble.ConnectionState
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
    
    // Use NativeGattManager (pure native approach)
    private val nativeManager: NativeGattManager by lazy { 
        NativeGattManager.getInstance(context)
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
        nativeManager.initialize()
        observeNativeManagerStates()
    }
    
    /**
     * Observe NativeGattManager states and map to domain states
     */
    private fun observeNativeManagerStates() {
        scope.launch {
            // Observe connection state
            nativeManager.connectionState.collect { state ->
                _connectionStatus.value = mapConnectionState(state)
            }
        }
        
        scope.launch {
            // Observe ring data
            nativeManager.ringData.collect { data ->
                _ringData.value = mapRingData(data)
            }
        }
        
        // Observe scan results
        scope.launch {
            nativeManager.scanState.collect { state ->
                 when (state) {
                     is ScanState.Idle -> _scanStatus.value = ScanStatus.Idle
                     is ScanState.Scanning -> _scanStatus.value = ScanStatus.Scanning
                     is ScanState.DevicesFound -> {
                         // Convert BleDevice -> Ring
                         val rings = state.devices.map { it.toDomain() }
                         _scanStatus.value = ScanStatus.DevicesFound(rings)
                     }
                     is ScanState.Error -> _scanStatus.value = ScanStatus.Error(state.message)
                 }
            }
        }
    }
    
    /**
     * Map Native Connection State to domain ConnectionStatus
     */
    private fun mapConnectionState(state: com.fitness.app.ble.ConnectionState): ConnectionStatus {
        return when (state) {
            is com.fitness.app.ble.ConnectionState.Disconnected -> ConnectionStatus.Disconnected
            is com.fitness.app.ble.ConnectionState.Connecting -> ConnectionStatus.Connecting
            is com.fitness.app.ble.ConnectionState.Connected -> {
                // If we know the ring details, pass them
                val ring = Ring(
                    macAddress = nativeManager.connectedMacAddress,
                    name = nativeManager.connectedDeviceName,
                    isConnected = true
                )
                connectedRing = ring
                ConnectionStatus.Connected(ring)
            }
            is com.fitness.app.ble.ConnectionState.Error -> ConnectionStatus.Error(state.message)
            is com.fitness.app.ble.ConnectionState.Timeout -> ConnectionStatus.Error("Connection timed out")
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
        nativeManager.initialize()
    }
    
    override suspend fun startScan(durationSeconds: Int): Result<List<Ring>> {
        return try {
            nativeManager.startScan(durationSeconds)
            Result.success(emptyList()) // Results come via flow
        } catch (e: Exception) {
            _scanStatus.value = ScanStatus.Error(e.message ?: "Scan failed")
            Result.error("Scan failed: ${e.message}", e)
        }
    }
    
    override fun stopScan() {
        nativeManager.stopScan()
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
            
            nativeManager.connectDevice(macAddress, deviceName)
            
            // Wait for connection (max 15 seconds)
            val connected = withTimeoutOrNull(15000L) {
                while (nativeManager.connectionState.value !is com.fitness.app.ble.ConnectionState.Connected) {
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
        // Native manager doesn't have explicit disconnect method public yet, 
        // effectively we stop via connectDevice or need to expose disconnect
        // For now, we just reset state
        _connectionStatus.value = ConnectionStatus.Disconnected
        return Result.success(Unit)
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
        return nativeManager.connectionState.value is com.fitness.app.ble.ConnectionState.Connected
    }
    
    override fun getConnectedRing(): Ring? = connectedRing
    
    /**
     * Start heart rate measurement
     * (Currently disabled in Native mode)
     */
    override fun startHeartRateMeasurement() {
        // nativeManager.startHeartRateMeasurement() // Not implemented in native yet
    }
    
    /**
     * Stop heart rate measurement
     */
    override fun stopHeartRateMeasurement() {
        // nativeManager.stopHeartRateMeasurement() // Not implemented in native yet
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
