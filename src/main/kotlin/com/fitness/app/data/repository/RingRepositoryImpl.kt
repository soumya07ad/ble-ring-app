package com.fitness.app.data.repository

import android.content.Context
import com.fitness.app.ble.BleDevice
import com.fitness.app.ble.BleManager
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
 * Wraps the existing BleManager to provide a clean interface
 * 
 * This preserves the existing BLE functionality while providing
 * a clean MVVM-compatible interface
 */
class RingRepositoryImpl(
    private val context: Context
) : IRingRepository {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Use existing BleManager singleton
    private val bleManager: BleManager by lazy { 
        BleManager.getInstance(context)
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
        observeBleManagerStates()
    }
    
    /**
     * Observe BleManager states and map to domain states
     */
    private fun observeBleManagerStates() {
        scope.launch {
            // Observe connection state
            bleManager.connectionState.collect { state ->
                _connectionStatus.value = mapConnectionState(state)
            }
        }
        
        scope.launch {
            // Observe scan state
            bleManager.scanState.collect { state ->
                _scanStatus.value = mapScanState(state)
            }
        }
        
        scope.launch {
            // Observe ring data
            bleManager.ringData.collect { data ->
                _ringData.value = mapRingData(data)
            }
        }
    }
    
    /**
     * Map BleManager ConnectionState to domain ConnectionStatus
     */
    private fun mapConnectionState(state: ConnectionState): ConnectionStatus {
        return when (state) {
            is ConnectionState.Disconnected -> ConnectionStatus.Disconnected
            is ConnectionState.Connecting -> ConnectionStatus.Connecting
            is ConnectionState.Connected -> {
                connectedRing?.let { ConnectionStatus.Connected(it) } 
                    ?: ConnectionStatus.Connected(
                        Ring(
                            macAddress = bleManager.connectedMacAddress,
                            name = bleManager.connectedDeviceName,
                            isConnected = true
                        )
                    )
            }
            is ConnectionState.Error -> ConnectionStatus.Error(state.message)
            is ConnectionState.Timeout -> ConnectionStatus.Timeout
        }
    }
    
    /**
     * Map BleManager ScanState to domain ScanStatus
     */
    private fun mapScanState(state: ScanState): ScanStatus {
        return when (state) {
            is ScanState.Idle -> ScanStatus.Idle
            is ScanState.Scanning -> ScanStatus.Scanning
            is ScanState.DevicesFound -> ScanStatus.DevicesFound(
                state.devices.map { it.toDomain() }
            )
            is ScanState.Error -> ScanStatus.Error(state.message)
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
        // BleManager.initialize() internally calls registerConnectionListener()
        bleManager.initialize()
    }
    
    override suspend fun startScan(durationSeconds: Int): Result<List<Ring>> {
        return try {
            _scanStatus.value = ScanStatus.Scanning
            bleManager.startScan()
            
            // Wait for scan duration
            delay(durationSeconds * 1000L)
            
            // Stop scan and get results
            bleManager.stopScan()
            
            val devices = when (val state = bleManager.scanState.value) {
                is ScanState.DevicesFound -> state.devices.map { it.toDomain() }
                else -> emptyList()
            }
            
            _scanStatus.value = ScanStatus.DevicesFound(devices)
            Result.success(devices)
        } catch (e: Exception) {
            _scanStatus.value = ScanStatus.Error(e.message ?: "Scan failed")
            Result.error("Scan failed: ${e.message}", e)
        }
    }
    
    override fun stopScan() {
        bleManager.stopScan()
        _scanStatus.value = ScanStatus.Idle
    }
    
    override suspend fun connect(macAddress: String, deviceName: String?): Result<Ring> {
        return try {
            val ring = Ring(
                macAddress = macAddress,
                name = deviceName ?: "Ring",
                isConnected = false
            )
            
            connectedRing = ring
            _connectionStatus.value = ConnectionStatus.Connecting
            
            bleManager.connectDevice(macAddress = macAddress, deviceName = deviceName)
            
            // Wait for connection (max 15 seconds)
            val connected = withTimeoutOrNull(15000L) {
                while (bleManager.connectionState.value !is ConnectionState.Connected) {
                    if (bleManager.connectionState.value is ConnectionState.Error ||
                        bleManager.connectionState.value is ConnectionState.Timeout) {
                        return@withTimeoutOrNull false
                    }
                    delay(100)
                }
                true
            } ?: false
            
            if (connected) {
                connectedRing = ring.copy(isConnected = true)
                Result.success(connectedRing!!)
            } else {
                connectedRing = null
                val errorMsg = when (val state = bleManager.connectionState.value) {
                    is ConnectionState.Error -> state.message
                    is ConnectionState.Timeout -> "Connection timed out"
                    else -> "Connection failed"
                }
                Result.error(errorMsg)
            }
        } catch (e: Exception) {
            connectedRing = null
            Result.error("Connection failed: ${e.message}", e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> {
        return try {
            bleManager.disconnect()
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
        return bleManager.connectionState.value is ConnectionState.Connected
    }
    
    override fun getConnectedRing(): Ring? = connectedRing
    
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
