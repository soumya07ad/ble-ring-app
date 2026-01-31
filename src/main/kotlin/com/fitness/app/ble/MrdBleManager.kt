package com.fitness.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fitness.app.domain.model.Ring
import com.manridy.lib.mrd.Manridy
import com.manridy.lib.mrd.bean.RawData
import com.manridy.lib.mrd.bean.RawDataType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * MRD SDK-based BLE Manager for R9 Ring
 * 
 * WORKING SDK that detects fresh R9 rings!
 * Uses Manridy MRD SDK for:
 * - BLE standard operations (scan/connect)
 * - Data parsing (battery, HR, steps)
 * - Commands (via Manridy.getMrdSend())
 */
class MrdBleManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MrdBleManager"
        

/**
 * Bluetooth hardware/software state
 */
enum class BluetoothState {
    NOT_AVAILABLE,  // No Bluetooth hardware
    DISABLED,       // Bluetooth is turned off
    ENABLED         // Bluetooth is on and ready
}
        // UUID for R9 Ring (MRD SDK)
        private val SERVICE_UUID = UUID.fromString("f000efe0-0451-4000-0000-00000000b000")
        private val NOTIFY_CHAR_UUID = UUID.fromString("f000efe3-0451-4000-0000-00000000b000")
        private val WRITE_CHAR_UUID = UUID.fromString("f000efe1-0451-4000-0000-00000000b000")
        private val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        
        @Volatile
        private var INSTANCE: MrdBleManager? = null
        
        fun getInstance(context: Context): MrdBleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MrdBleManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    // StateFlows
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private val _ringData = MutableStateFlow(RingData())
    val ringData: StateFlow<RingData> = _ringData.asStateFlow()
    
    private val _scanResults = MutableStateFlow<List<Ring>>(emptyList())
    val scanResults: StateFlow<List<Ring>> = _scanResults.asStateFlow()
    
    
    private val _measurementTimer = MutableStateFlow(MeasurementTimer())
    val measurementTimer: StateFlow<MeasurementTimer> = _measurementTimer.asStateFlow()
    
    // Coroutine scope for background tasks
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Jobs
    private var measurementJob: Job? = null
    private var connectionTimeoutJob: Job? = null
    private var keepAliveJob: Job? = null
    
    // BLE objects
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Scan state
    private var isScanning = false
    
    init {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "MRD BLE Manager initialized!")
        Log.i(TAG, "Ready to detect R9 rings")
        Log.i(TAG, "═══════════════════════════════════")
    }
    
    // Connection timeout
    private var connectionTimeoutJob: Job? = null
    
    // ==================== Bluetooth State Checks ====================
    
    /**
     * Check if Bluetooth hardware is available
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }
    
    /**
     * Check if Bluetooth is currently enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Get current Bluetooth state
     */
    fun getBluetoothState(): BluetoothState {
        return when {
            bluetoothAdapter == null -> BluetoothState.NOT_AVAILABLE
            !bluetoothAdapter.isEnabled -> BluetoothState.DISABLED
            else -> BluetoothState.ENABLED
        }
    }
    
    // ==================== Scanning ====================
    
    @SuppressLint("MissingPermission")
    fun startScan(durationSeconds: Int = 6) {
        // CHECK BLUETOOTH STATE FIRST
        when (getBluetoothState()) {
            BluetoothState.NOT_AVAILABLE -> {
                Log.e(TAG, "❌ Bluetooth not available on this device")
                handler.post {
                    _connectionState.value = BleConnectionState.Error("Bluetooth not available on this device")
                }
                return
            }
            BluetoothState.DISABLED -> {
                Log.e(TAG, "❌ Bluetooth is disabled - Please enable Bluetooth")
                handler.post {
                    _connectionState.value = BleConnectionState.Error("Please enable Bluetooth to scan for devices")
                }
                return
            }
            BluetoothState.ENABLED -> {
                // Continue with scan
            }
        }
        
        if (isScanning) {
            Log.w(TAG, "⚠️ Already scanning")
            return
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔍 MRD Starting Scan ($durationSeconds seconds)")
        Log.i(TAG, "NO FILTER - Will find fresh R9 rings!")
        Log.i(TAG, "═══════════════════════════════════")
        
        _scanResults.value = emptyList()
        isScanning = true
        
        try {
            bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
            
            // Stop scan after duration
            handler.postDelayed({
                stopScan()
            }, durationSeconds * 1000L)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Scan failed: ${e.message}", e)
            isScanning = false
        }
    }
    
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        
        Log.i(TAG, "🛑 MRD Stop Scan")
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}")
        }
        isScanning = false
    }
    
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Ring"
            val mac = device.address
            val rssi = result.rssi
            
            // Check if already in list
            val currentResults = _scanResults.value.toMutableList()
            if (currentResults.none { it.macAddress == mac }) {
                Log.d(TAG, "📍 Found: $name ($mac) RSSI: $rssi")
                
                val ring = Ring(
                    name = name,
                    macAddress = mac,
                    rssi = rssi,
                    isConnected = false
                )
                currentResults.add(ring)
                _scanResults.value = currentResults
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            val errorMsg = when (errorCode) {
                android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                android.bluetooth.le.ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                android.bluetooth.le.ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scan not supported on this device"
                android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal Bluetooth error"
                else -> "Unknown error: $errorCode"
            }
            
            Log.e(TAG, "❌ Scan failed: $errorMsg (code=$errorCode)")
            isScanning = false
            
            // Update state so UI knows about the error
            handler.post {
                _connectionState.value = BleConnectionState.Error("Scan failed: $errorMsg")
            }
        }
    }
    
    // ==================== Connection ====================
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(macAddress: String, deviceName: String? = null) {
        // CHECK BLUETOOTH STATE FIRST
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "❌ Cannot connect: Bluetooth is not enabled")
            handler.post {
                _connectionState.value = BleConnectionState.Error("Bluetooth is not enabled")
            }
            return
        }
        
        val currentState = _connectionState.value
        if (currentState is BleConnectionState.Connected) {
            Log.w(TAG, "⚠️ Already connected, disconnecting first...")
            disconnect()
        }
        
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        if (device == null) {
            Log.e(TAG, "❌ Device not found: $macAddress")
            handler.post {
                _connectionState.value = BleConnectionState.Error("Device not found")
            }
            return
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔗 MRD Connecting to: $macAddress")
        Log.i(TAG, "═══════════════════════════════════")
        
        _connectionState.value = BleConnectionState.Connecting
        connectedDevice = device
        
        // START 30-SECOND TIMEOUT
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(30000) // 30 seconds
            
            if (_connectionState.value is BleConnectionState.Connecting) {
                Log.e(TAG, "❌ Connection timeout after 30 seconds")
                disconnect()
                _connectionState.value = BleConnectionState.Error("Connection timeout")
            }
        }
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        MrdPushCore.getInstance().init(bluetoothGatt)
    }
    
    @SuppressLint("MissingPermission")
    fun disconnect() {
        Log.i(TAG, "🔌 MRD Disconnecting")
        
        bluetoothGatt?.let {
            it.disconnect()
            it.close()
        }
        bluetoothGatt = null
        connectedDevice = null
        _connectionState.value = BleConnectionState.Disconnected
        _ringData.value = RingData()
    }
    
    // ==================== GATT Callback ====================
    
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✓ Connected, discovering services...")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "❌ Disconnected")
            stopKeepAlive()  // Stop keep-alive when disconnected
            handler.post {
                        _connectionState.value = BleConnectionState.Disconnected
                    }
                }
            }
        }
        
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            // Cancel connection timeout - we're successfully connected!
            connectionTimeoutJob?.cancel()
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ Services discovered")
                
                // Enable notifications
                val service = gatt.getService(SERVICE_UUID)
                val notifyChar = service?.getCharacteristic(NOTIFY_CHAR_UUID)
                
                if (notifyChar != null) {
                    gatt.setCharacteristicNotification(notifyChar, true)
                    
                    val descriptor = notifyChar.getDescriptor(CLIENT_CONFIG_UUID)
                    descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    gatt.writeDescriptor(descriptor)
                    
                    handler.post {
                        connectedDevice?.let { device ->
                            val ring = Ring(
                                name = device.name ?: "R9 Ring",
                                macAddress = device.address,
                                isConnected = true
                            )
                            _connectionState.value = BleConnectionState.Connected(ring)
                        }
                        
                        // Request initial data
                        Log.i(TAG, "📊 Requesting initial data...")
                        handler.postDelayed({
                            requestBattery()
                            requestSteps()
                            requestStress()
                            
                            // Start periodic keep-alive (every 5 seconds)
                            startKeepAlive()
                        }, 500)
                    }
                } else {
                    Log.e(TAG, "❌ Notify characteristic not found!")
                }
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            if (data != null) {
                Log.d(TAG, "📥 Data received: ${bytesToHex(data)}")
                
                // Parse data using MRD SDK
                MrdPushCore.getInstance().readData(data)
                parseDataFromDevice(data)
            }
        }
        
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✓ Write success")
                MrdPushCore.getInstance().onCharacteristicWrite(status, characteristic)
            } else {
                Log.w(TAG, "⚠️ Write failed: status=$status")
            }
        }
        
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ MTU changed: $mtu")
                MrdPushCore.getInstance().onMtuChanged(mtu, status)
            }
        }
    }
    
    // ==================== Data Operations ====================
    
    /**
     * Request battery level from ring
     */
    fun requestBattery() {
        val command = Manridy.getMrdSend().getSystem(com.manridy.sdk_mrd2019.bean.send.SystemEnum.battery).datas
        writeData(command)
    }
    
    /**
     * Request heart rate from ring
     */
    fun requestHeartRate() {
        val command = Manridy.getMrdSend().getHrData(0).datas
        writeData(command)
    }
    
    /**
     * Request steps from ring
     */
    fun requestSteps() {
        Log.i(TAG, "📤 Requesting step data from ring...")
        val command = Manridy.getMrdSend().getStep(3).datas
        writeData(command)
    }
    
    /**
     * Request SpO2 (blood oxygen) from ring
     */
    fun requestSpO2() {
        val command = Manridy.getMrdSend().getBoData(0).datas
        writeData(command)
    }
    
    /**
     * Request blood pressure from ring
     */
    fun requestBloodPressure() {
        val command = Manridy.getMrdSend().getBpData(0).datas
        writeData(command)
    }
    
    /**
     * Request stress/HRV data from ring
     */
    fun requestStress() {
        Log.i(TAG, "📤 Requesting stress/HRV data from ring...")
        val command = Manridy.getMrdSend().getHRVHistory(2).datas
        writeData(command)
    }
    
    /**
     * Write data to ring
     */
    
    // ==================== Timed Measurements ====================
    
    /**
     * Start 30-second heart rate measurement
     */
    fun startHeartRateMeasurement() {
        startTimedMeasurement(
            type = MeasurementType.HEART_RATE,
            requestFunc = { requestHeartRate() }
        )
    }
    
    /**
     * Start 30-second blood pressure measurement
     */
    fun startBloodPressureMeasurement() {
        startTimedMeasurement(
            type = MeasurementType.BLOOD_PRESSURE,
            requestFunc = { requestBloodPressure() }
        )
    }
    
    /**
     * Start 30-second SpO2 measurement
     */
    fun startSpO2Measurement() {
        startTimedMeasurement(
            type = MeasurementType.SPO2,
            requestFunc = { requestSpO2() }
        )
    }
    
    /**
     * Start 30-second stress measurement
     */
    fun startStressMeasurement() {
        startTimedMeasurement(
            type = MeasurementType.STRESS,
            requestFunc = { requestStress() }
        )
    }
    
    /**
     * Generic timed measurement function
     * Requests data every 2 seconds for the specified duration
     */
    private fun startTimedMeasurement(
        type: MeasurementType,
        durationSeconds: Int = 30,
        requestFunc: () -> Unit
    ) {
        // Cancel any existing measurement
        stopMeasurement()
        
        Log.i(TAG, " Starting ${type.name} measurement for $durationSeconds seconds")
        
        measurementJob = CoroutineScope(Dispatchers.Main).launch {
            _measurementTimer.value = MeasurementTimer(
                isActive = true,
                measurementType = type,
                remainingSeconds = durationSeconds,
                totalSeconds = durationSeconds
            )
            
            // Request data every 2 seconds during the measurement
            for (second in durationSeconds downTo 0) {
                _measurementTimer.value = _measurementTimer.value.copy(
                    remainingSeconds = second
                )
                
                // Request every 2 seconds
                if (second % 2 == 0) {
                    requestFunc()
                }
                
                if (second > 0) {
                    delay(1000)  // Wait 1 second
                }
            }
            
            Log.i(TAG, " ${type.name} measurement complete")
            _measurementTimer.value = MeasurementTimer()  // Reset
        }
    }
    
    /**
     * Stop current measurement
     */
    fun stopMeasurement() {
        measurementJob?.cancel()
        measurementJob = null
        _measurementTimer.value = MeasurementTimer()
        if (_measurementTimer.value.isActive) {
            Log.i(TAG, " Measurement stopped")
        }
    }
    @SuppressLint("MissingPermission")
    private fun writeData(data: ByteArray) {
        val gatt = bluetoothGatt
        if (gatt == null) {
            Log.e(TAG, "❌ Not connected")
            return
        }
        
        val service = gatt.getService(SERVICE_UUID)
        val writeChar = service?.getCharacteristic(WRITE_CHAR_UUID)
        
        if (writeChar != null) {
            writeChar.value = data
            writeChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            gatt.writeCharacteristic(writeChar)
            Log.d(TAG, "📤 Wrote: ${bytesToHex(data)}")
        } else {
            Log.e(TAG, "❌ Write characteristic not found")
        }
    }
    
    /**
     * Parse incoming data from ring using MRD SDK
     */
    private fun parseDataFromDevice(data: ByteArray) {
        try {
            val readRequest: MrdReadRequest = Manridy.getMrdRead().read(data)
            
            if (readRequest.status == 0 || readRequest.mrdReadEnum == MrdReadEnum.Failure) {
                Log.w(TAG, "⚠️ Parse failed or unsupported command")
                return
            }
            
            val json = readRequest.json

            Log.i(TAG, "📦 MRD Data: ${readRequest.mrdReadEnum.name} = $json")

            if (!json.isNullOrEmpty()) {
                val battery = parseJsonInt(json, "battery")
                if (battery != null && battery in 1..100) {
                    Log.i(TAG, "🔋 Battery: $battery%")
                    handler.post {
                        _ringData.value = _ringData.value.copy(battery = battery, lastUpdate = System.currentTimeMillis())
                    }
                }

                val hr = parseJsonInt(json, "heartRate") ?: parseJsonInt(json, "heart")
                if (hr != null && hr in 40..220) {
                    Log.i(TAG, "❤️ Heart Rate: $hr bpm")
                    handler.post {
                        _ringData.value = _ringData.value.copy(heartRate = hr, lastUpdate = System.currentTimeMillis())
                    }
                }

                // Parse Steps with enhanced data (calories, distance)
                val stepNum1 = parseJsonInt(json, "stepNum")
                val stepNum2 = parseJsonInt(json, "step")
                val stepNum3 = parseJsonInt(json, "steps")
                val steps = stepNum1 ?: stepNum2 ?: stepNum3
                val calories = parseJsonInt(json, "stepCalorie") ?: parseJsonInt(json, "calories")
                val distanceMeters = parseJsonInt(json, "stepMileage") ?: parseJsonInt(json, "stepDistance") ?: parseJsonInt(json, "distance")
                
                // DEBUG LOGGING
                Log.d(TAG, "🔍 Step Parse: stepNum=$stepNum1, step=$stepNum2, steps=$stepNum3 → final=$steps, cal=$calories, dist=$distanceMeters")
                
                if (steps != null && steps >= 0) {
                    val distanceKm = (distanceMeters ?: 0) / 1000.0
                    Log.i(TAG, "👟 Steps: $steps (${calories ?: 0} cal, ${String.format("%.2f", distanceKm)} km)")
                    handler.post {
                        _ringData.value = _ringData.value.copy(
                            steps = steps,
                            calories = calories ?: 0,
                            distance = distanceMeters ?: 0,
                            lastUpdate = System.currentTimeMillis()
                        )
                        Log.d(TAG, "✅ Step data updated in ringData state")
                    }
                } else {
                    Log.w(TAG, "⚠️ Step data null or invalid: $steps")
                }
                
                // Parse SpO2 (Blood Oxygen) - Float for decimals
                // boRate comes as STRING "99.5", need to handle both string and number
                val spo2 = parseJsonFloat(json, "boRate") ?: parseJsonFloat(json, "bo") ?: parseJsonFloat(json, "spo2")
                
                // DEBUG LOGGING
                Log.d(TAG, "🔍 SpO2 Parse Debug: spo2=$spo2 from JSON: $json")
                
                if (spo2 != null && spo2 in 80f..100f) {
                    Log.i(TAG, "🫁 SpO2: $spo2%")
                    handler.post {
                        _ringData.value = _ringData.value.copy(spO2 = spo2, lastUpdate = System.currentTimeMillis())
                        Log.d(TAG, "✅ SpO2 data updated in ringData state")
                    }
                } else {
                    Log.w(TAG, "⚠️ SpO2 out of range or null: $spo2")
                }
                
               // Parse Blood Pressure - ALL 3 VALUES
                val systolic = parseJsonInt(json, "bpHp") ?: parseJsonInt(json, "bphp") ?: parseJsonInt(json, "hightBp") ?: parseJsonInt(json, "systolic")
                val diastolic = parseJsonInt(json, "bpLp") ?: parseJsonInt(json, "bplp") ?: parseJsonInt(json, "lowBp") ?: parseJsonInt(json, "diastolic")
                val bpHR = parseJsonInt(json, "bpHr") ?: parseJsonInt(json, "bphr") ?: parseJsonInt(json, "bpHeartRate")
                
                // DEBUG LOGGING
                Log.d(TAG, "🔍 BP Parse Debug: systolic=$systolic, diastolic=$diastolic, bpHR=$bpHR")
                
                if (systolic != null && diastolic != null) {
                    val hrInfo = if (bpHR != null && bpHR > 0) ", HR: $bpHR bpm" else ""
                    Log.i(TAG, "🩺 BP: $systolic/$diastolic mmHg$hrInfo")
                    handler.post {
                        _ringData.value = _ringData.value.copy(
                            bloodPressureSystolic = systolic,
                            bloodPressureDiastolic = diastolic,
                            bloodPressureHeartRate = bpHR ?: 0,
                            lastUpdate = System.currentTimeMillis()
                        )
                        Log.d(TAG, "✅ BP data updated in ringData state")
                    }
                } else {
                    Log.w(TAG, "⚠️ BP data incomplete: systolic=$systolic, diastolic=$diastolic")
                }
                
                // Parse Stress/HRV
                val ssType = parseJsonInt(json, "ss_type")
                val hrv = parseJsonInt(json, "hrv")
                val stressField = parseJsonInt(json, "stress")
                val measureMode = parseJsonInt(json, "measureMode")
                val stress = ssType ?: hrv ?: stressField ?: measureMode
                    
                Log.d(TAG, "🔍 Stress Parse: ss_type=$ssType, hrv=$hrv, stress=$stressField, measureMode=$measureMode → final=$stress")
                
                if (stress != null && stress in 0..200) {  // Extended range for ss_type
                    Log.i(TAG, "😰 Stress/HRV: $stress")
                    handler.post {
                        _ringData.value = _ringData.value.copy(stress = stress, lastUpdate = System.currentTimeMillis())
                        Log.d(TAG, "✅ Stress data updated in ringData state")
                    }
                } else {
                    Log.w(TAG, "⚠️ Stress data out of range or null: $stress")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}", e)
        }
    }
    
    // ==================== Helper Functions ====================
    
    private fun parseJsonInt(json: String?, key: String): Int? {
        if (json.isNullOrEmpty()) return null
        return try {
            // Match both quoted "stepNum":506 and unquoted stepNum:506
            val regex = "\"?$key\"?\\s*:\\s*(\\d+)".toRegex()
            regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    
    /**
     * Parse Float value from JSON (for SpO2 decimals like 99.5)
     */
    private fun parseJsonFloat(json: String?, key: String): Float? {
        if (json.isNullOrEmpty()) return null
        return try {
            // Match both quoted and unquoted keys, and quoted and unquoted values
            // Examples: "boRate":"99.5" OR boRate:"99.5" OR boRate:99.5
            val regex = "\"?$key\"?\\s*:\\s*\"?([0-9]+\\.?[0-9]*)\"?".toRegex()
            regex.find(json)?.groupValues?.get(1)?.toFloatOrNull()
        } catch (e: Exception) {
            null
        }
    }
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Start periodic keep-alive to prevent connection timeout
     * Requests battery every 5 seconds to maintain connection
     */
    private fun startKeepAlive() {
        stopKeepAlive()  // Cancel any existing keep-alive
        
        keepAliveJob = ioScope.launch {
            while (isActive) {
                delay(5000)  // Every 5 seconds
                Log.d(TAG, "💓 Keep-alive: requesting battery")
                requestBattery()
            }
        }
        Log.i(TAG, "💓 Keep-alive started (5s interval)")
    }
    
    /**
     * Stop keep-alive
     */
    private fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
        Log.d(TAG, "💓 Keep-alive stopped")
    }
}
