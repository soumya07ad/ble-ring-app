package com.fitness.app.data.repository

import android.content.Context
import android.util.Log
import com.fitness.app.ble.BleConnectionState
import com.fitness.app.ble.RingData
import com.fitness.app.ble.SdkBleManager
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
 * Uses SdkBleManager (YCBTClient SDK approach)
 * 
 * SDK handles all BLE operations: scanning, connection, data retrieval
 */
class RingRepositoryImpl(
    private val context: Context
) : IRingRepository {
    
    companion object {
        private const val TAG = "RingRepositoryImpl"
        
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
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Use SdkBleManager (YCBTClient SDK approach)
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
        // Observe connection state
        scope.launch {
            sdkManager.connectionState.collect { state ->
                _connectionStatus.value = mapConnectionState(state)
            }
        }
        
        // Observe ring data
        scope.launch {
            sdkManager.ringData.collect { data ->
                _ringData.value = mapRingData(data)
            }
        }
        
        // Observe scan results
        scope.launch {
            sdkManager.scanResults.collect { rings ->
                if (rings.isNotEmpty()) {
                    _scanStatus.value = ScanStatus.DevicesFound(rings)
                }
            }
        }
    }
    
    /**
     * Map SDK Connection State to domain ConnectionStatus
     */
    private fun mapConnectionState(state: BleConnectionState): ConnectionStatus {
        return when (state) {
            is BleConnectionState.Disconnected -> ConnectionStatus.Disconnected
            is BleConnectionState.Connecting -> ConnectionStatus.Connecting
            is BleConnectionState.Connected -> {
                connectedRing = state.ring.copy(isConnected = true)
                ConnectionStatus.Connected(connectedRing!!)
            }
        }
    }
    
    /**
     * Map SDK RingData to domain RingHealthData
     */
    private fun mapRingData(data: RingData): RingHealthData {
        return RingHealthData(
            battery = data.battery,
            heartRate = data.heartRate,
            heartRateMeasuring = data.heartRateMeasuring,
            bloodPressureSystolic = data.bloodPressureSystolic,
            bloodPressureDiastolic = data.bloodPressureDiastolic,
            bloodPressureMeasuring = data.bloodPressureMeasuring,
            spO2 = data.spO2,
            spO2Measuring = data.spO2Measuring,
            stress = data.stress,
            steps = data.steps,
            distance = data.distance,
            calories = data.calories,
            deepSleep = data.deepSleep,
            lightSleep = data.lightSleep,
            lastUpdate = data.lastUpdate
        )
    }
    
    override fun initialize() {
        sdkManager.initialize()
    }
    
    override suspend fun startScan(durationSeconds: Int): Result<List<Ring>> {
        return try {
            _scanStatus.value = ScanStatus.Scanning
            sdkManager.startScan(durationSeconds)
            Result.success(emptyList()) // Results come via flow
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
            
            Log.i(TAG, "Connecting via SDK to: $macAddress")
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
                Log.i(TAG, "✓ SDK Connection successful!")
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
        sdkManager.disconnect()
        connectedRing = null
        _connectionStatus.value = ConnectionStatus.Disconnected
        return Result.success(Unit)
    }
    
    override suspend fun getBattery(): Result<Int> {
        // Request fresh battery data
        sdkManager.refreshDeviceInfo()
        
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
        Log.i(TAG, "Starting HR measurement via SDK")
        sdkManager.startHeartRateMeasurement()
    }
    
    /**
     * Stop heart rate measurement
     */
    override fun stopHeartRateMeasurement() {
        sdkManager.stopHeartRateMeasurement()
    }
    
    /**
     * Start blood pressure measurement
     * Note: SDK may not support this directly
     */
    override fun startBloodPressureMeasurement() {
        Log.w(TAG, "Blood pressure measurement not yet implemented in SDK")
        // TODO: Implement when SDK method is available
    }
    
    /**
     * Stop blood pressure measurement
     */
    override fun stopBloodPressureMeasurement() {
        Log.w(TAG, "Blood pressure measurement not yet implemented in SDK")
    }
    
    /**
     * Start SpO2 measurement
     * Note: SDK may not support this directly
     */
    override fun startSpO2Measurement() {
        Log.w(TAG, "SpO2 measurement not yet implemented in SDK")
        // TODO: Implement when SDK method is available
    }
    
    /**
     * Stop SpO2 measurement
     */
    override fun stopSpO2Measurement() {
        Log.w(TAG, "SpO2 measurement not yet implemented in SDK")
    }
    
    /**
     * Refresh device info (battery, device info)
     */
    fun refreshDeviceInfo() {
        sdkManager.refreshDeviceInfo()
    }
    
    /**
     * Refresh steps data
     */
    fun refreshStepsData() {
        sdkManager.refreshStepsData()
    }
}
