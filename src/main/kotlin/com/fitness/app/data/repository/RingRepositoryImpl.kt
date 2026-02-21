package com.fitness.app.data.repository

import android.content.Context
import android.util.Log
import com.fitness.app.ble.BleConnectionState
import com.fitness.app.ble.RingData
import com.fitness.app.ble.NativeGattManager
import com.fitness.app.ble.MeasurementTimer
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
 * Uses NativeGattManager (Pure Native BLE — No SDK)
 * 
 * All BLE operations use raw byte-array commands and native GATT.
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
    
    // Use NativeGattManager (Pure Native BLE)
    private val bleManager: NativeGattManager by lazy { 
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
    override val measurementTimer: StateFlow<MeasurementTimer> = bleManager.measurementTimer
    private var connectedRing: Ring? = null
    
    init {
        observeManagerStates()
    }
    
    /**
     * Observe NativeGattManager states and map to domain states
     */
    private fun observeManagerStates() {
        // Observe connection state
        scope.launch {
            bleManager.connectionState.collect { state ->
                _connectionStatus.value = mapConnectionState(state)
            }
        }
        
        // Observe ring data
        scope.launch {
            bleManager.ringData.collect { data ->
                _ringData.value = mapRingData(data)
            }
        }
        
        // Observe scan results
        scope.launch {
            bleManager.scanResults.collect { rings ->
                if (rings.isNotEmpty()) {
                    _scanStatus.value = ScanStatus.DevicesFound(rings)
                }
            }
        }
    }
    
    /**
     * Map BLE Connection State to domain ConnectionStatus
     */
    private fun mapConnectionState(state: BleConnectionState): ConnectionStatus {
        return when (state) {
            is BleConnectionState.Disconnected -> {
                connectedRing = null
                ConnectionStatus.Disconnected
            }
            is BleConnectionState.Connecting -> ConnectionStatus.Connecting
            is BleConnectionState.Connected -> {
                connectedRing = state.ring.copy(isConnected = true)
                ConnectionStatus.Connected(connectedRing!!)
            }
            is BleConnectionState.Error -> {
                connectedRing = null
                ConnectionStatus.Disconnected
            }
        }
    }
    
    /**
     * Map BLE RingData to domain RingHealthData
     */
    private fun mapRingData(data: RingData): RingHealthData {
        return RingHealthData(
            battery = data.battery,
            isCharging = data.isCharging,
            heartRate = data.heartRate,
            heartRateMeasuring = data.heartRateMeasuring,
            bloodPressureSystolic = data.bloodPressureSystolic,
            bloodPressureDiastolic = data.bloodPressureDiastolic,
            bloodPressureHeartRate = data.bloodPressureHeartRate,
            bloodPressureMeasuring = data.bloodPressureMeasuring,
            spO2 = data.spO2,
            spO2Measuring = data.spO2Measuring,
            stress = data.stress,
            steps = data.steps,
            distance = data.distance,
            calories = data.calories,
            sleepData = data.sleepData,
            firmwareInfo = data.firmwareInfo,
            lastUpdate = data.lastUpdate
        )
    }
    
    override fun initialize() {
        // No SDK initialization needed — NativeGattManager is self-contained
    }
    
    override suspend fun startScan(durationSeconds: Int): Result<List<Ring>> {
        return try {
            _scanStatus.value = ScanStatus.Scanning
            bleManager.startScan(durationSeconds)
            Result.success(emptyList()) // Results come via flow
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
                name = deviceName ?: "R9 Ring",
                isConnected = false
            )
            
            connectedRing = ring
            _connectionStatus.value = ConnectionStatus.Connecting
            
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "🔗 Native BLE connect: $macAddress")
            Log.i(TAG, "   Pure byte-array protocol")
            Log.i(TAG, "═══════════════════════════════════")
            
            bleManager.connectToDevice(macAddress, deviceName)
            
            Result.success(ring)
            
        } catch (e: Exception) {
            connectedRing = null
            _connectionStatus.value = ConnectionStatus.Disconnected
            Result.error("Connection failed: ${e.message}", e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> {
        bleManager.disconnect()
        connectedRing = null
        _connectionStatus.value = ConnectionStatus.Disconnected
        return Result.success(Unit)
    }
    
    override suspend fun getBattery(): Result<Int> {
        bleManager.requestBattery()
        
        val battery = _ringData.value.battery
        return if (battery != null && battery > 0) {
            Result.success(battery)
        } else {
            Result.error("Battery data not available")
        }
    }
    
    override fun isConnected(): Boolean {
        return bleManager.connectionState.value is BleConnectionState.Connected
    }
    
    override fun getConnectedRing(): Ring? = connectedRing
    
    // ═══════════════════════════════════
    // Measurements (delegate to manager)
    // ═══════════════════════════════════
    
    override fun startHeartRateMeasurement() {
        Log.i(TAG, "Starting HR measurement (native)")
        bleManager.requestHeartRate()
    }
    
    override fun stopHeartRateMeasurement() {
        // Auto-stops after data received
    }
    
    override fun startBloodPressureMeasurement() {
        Log.i(TAG, "Starting BP measurement (native)")
        bleManager.startBloodPressureMeasurement()
    }
    
    override fun stopBloodPressureMeasurement() {
        bleManager.stopMeasurement()
    }
    
    override fun startSpO2Measurement() {
        Log.i(TAG, "Starting SpO2 measurement (native)")
        bleManager.startSpO2Measurement()
    }
    
    override fun stopSpO2Measurement() {
        bleManager.stopMeasurement()
    }
    
    override fun startStressMeasurement() {
        Log.i(TAG, "Starting stress measurement (native)")
        bleManager.startStressMeasurement()
    }
    
    override fun stopStressMeasurement() {
        bleManager.stopMeasurement()
    }
    
    override fun requestSleepHistory() {
        Log.i(TAG, "Requesting sleep history (native)")
        bleManager.requestSleepHistory()
    }
    
    fun refreshDeviceInfo() {
        bleManager.requestBattery()
    }
    
    fun refreshStepsData() {
        bleManager.requestSteps()
    }
    
    fun refreshBloodPressure() {
        bleManager.requestBloodPressure()
    }
    
    fun refreshStress() {
        bleManager.requestStress()
    }
    
    fun refreshAllData() {
        bleManager.requestBattery()
        bleManager.requestHeartRate()
        bleManager.requestSteps()
        bleManager.requestSpO2()
        bleManager.requestBloodPressure()
        bleManager.requestStress()
    }
    
    override fun stopMeasurement() {
        bleManager.stopMeasurement()
    }
}
