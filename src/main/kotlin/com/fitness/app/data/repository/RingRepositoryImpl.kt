package com.fitness.app.data.repository

import android.content.Context
import android.util.Log
import com.fitness.app.ble.BleConnectionState
import com.fitness.app.ble.RingData
import com.fitness.app.ble.MrdBleManager
import com.fitness.app.core.util.Result
import com.fitness.app.domain.model.ConnectionStatus
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.RingHealthData
import com.fitness.app.domain.model.ScanStatus
import com.fitness.app.domain.repository.IRingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Implementation of IRingRepository
 * Uses MrdBleManager (Manridy MRD SDK approach)
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
    
    // Use MrdBleManager (Manridy MRD SDK approach)
    private val mrdManager: MrdBleManager by lazy { 
        MrdBleManager.getInstance(context)
    }
    
    // Domain state flows
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    override val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()
    
    private val _ringData = MutableStateFlow(RingHealthData())
    override val ringData: StateFlow<RingHealthData> = _ringData.asStateFlow()
    
    // Track connected ring
    
    override val measurementTimer: StateFlow<MeasurementTimer> = mrdManager.measurementTimer
    private var connectedRing: Ring? = null
    
    init {
        observeManagerStates()
    }
    
    /**
     * Observe MrdBleManager states and map to domain states
     */
    private fun observeManagerStates() {
        // Observe connection state
        scope.launch {
            mrdManager.connectionState.collect { state ->
                _connectionStatus.value = mapConnectionState(state)
            }
        }
        
        // Observe ring data
        scope.launch {
            mrdManager.ringData.collect { data ->
                _ringData.value = mapRingData(data)
            }
        }
        
        // Observe scan results
        scope.launch {
            mrdManager.scanResults.collect { rings ->
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
        // MRD SDK initialized in FitnessApplication
    }
    
    override suspend fun startScan(durationSeconds: Int): Result<List<Ring>> {
        return try {
            _scanStatus.value = ScanStatus.Scanning
            mrdManager.startScan(durationSeconds)
            Result.success(emptyList()) // Results come via flow
        } catch (e: Exception) {
            _scanStatus.value = ScanStatus.Error(e.message ?: "Scan failed")
            Result.error("Scan failed: ${e.message}", e)
        }
    }
    
    override fun stopScan() {
        mrdManager.stopScan()
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
            
            // SDK-DRIVEN CONNECTION (like RealSil in Chinese app)
            // - Trigger connect() ONCE
            // - SDK owns the connection lifecycle
            // - SDK handles retries and reconnection internally
            // - App only observes state via callbacks
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "🔗 Triggering SDK connect: $macAddress")
            Log.i(TAG, "   SDK owns: connection, retries, reconnect")
            Log.i(TAG, "═══════════════════════════════════")
            
            mrdManager.connectToDevice(macAddress, deviceName)
            
            // Return immediately - connection state will be updated via SDK callbacks
            // NO app-level timeout - SDK manages this
            Result.success(ring)
            
        } catch (e: Exception) {
            connectedRing = null
            _connectionStatus.value = ConnectionStatus.Disconnected
            Result.error("Connection failed: ${e.message}", e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> {
        mrdManager.disconnect()
        connectedRing = null
        _connectionStatus.value = ConnectionStatus.Disconnected
        return Result.success(Unit)
    }
    
    override suspend fun getBattery(): Result<Int> {
        // Request fresh battery data
        mrdManager.requestBattery()
        
        val battery = _ringData.value.battery
        return if (battery != null && battery > 0) {
            Result.success(battery)
        } else {
            Result.error("Battery data not available")
        }
    }
    
    override fun isConnected(): Boolean {
        return mrdManager.connectionState.value is BleConnectionState.Connected
    }
    
    override fun getConnectedRing(): Ring? = connectedRing
    
    /**
     * Start heart rate measurement via SDK
     */
    override fun startHeartRateMeasurement() {
        Log.i(TAG, "Starting HR measurement via MRD SDK")
        mrdManager.requestHeartRate()
    }
    
    /**
     * Stop heart rate measurement
     */
    override fun stopHeartRateMeasurement() {
        // MRD SDK auto-stops after measurement
    }
    
    /**
     * Start blood pressure measurement
     * Note: SDK may not support this directly
     */
    override fun startBloodPressureMeasurement() {
        Log.i(TAG, "Starting 30-second blood pressure measurement")
        mrdManager.startBloodPressureMeasurement()
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
        Log.i(TAG, "Starting 30-second SpO2 measurement")
        mrdManager.startSpO2Measurement()
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
        mrdManager.requestBattery()
    }
    
    /**
     * Refresh steps data
     */
    fun refreshStepsData() {
        mrdManager.requestSteps()
    }
    
    /**
     * Refresh blood pressure data
     */
    fun refreshBloodPressure() {
        mrdManager.requestBloodPressure()
    }
    
    /**
     * Refresh stress/HRV data
     */
    fun refreshStress() {
        mrdManager.requestStress()
    }
    
    /**
     * Refresh all health data from ring
     */
    fun refreshAllData() {
        mrdManager.requestBattery()
        mrdManager.requestHeartRate()
        mrdManager.requestSteps()
        mrdManager.requestSpO2()
        mrdManager.requestBloodPressure()
        mrdManager.requestStress()
    }
    
    override fun stopMeasurement() {
        mrdManager.stopMeasurement()
    }
}
