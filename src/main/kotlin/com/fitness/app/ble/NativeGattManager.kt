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
import com.fitness.app.domain.model.FirmwareInfo
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.SleepData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Pure Native BLE Manager for R9 Ring
 * 
 * NO SDK DEPENDENCY — Uses reverse-engineered byte-array protocol.
 * All commands are raw 20-byte ByteArrays starting with 0xFC.
 * All responses are parsed at the byte level.
 * 
 * Protocol Reference: documentation/mrd_sdk_reverse_engineering_report.md
 * 
 * @author DKGS Labs
 * @version 3.0.0 - Pure Native BLE (No MRD SDK)
 */
class NativeGattManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "NativeGattManager"
        
        // ═══════════════════════════════════════════════════════════
        // BLE Service & Characteristic UUIDs (from ManConstants.java)
        // ═══════════════════════════════════════════════════════════
        private val SERVICE_UUID      = UUID.fromString("f000efe0-0451-4000-0000-00000000b000")
        private val WRITE_CHAR_UUID   = UUID.fromString("f000efe1-0451-4000-0000-00000000b000")
        private val NOTIFY_CHAR_UUID  = UUID.fromString("f000efe3-0451-4000-0000-00000000b000")
        private val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        
        // Protocol constants
        private const val HEADER: Byte = 0xFC.toByte()
        private const val PACKET_SIZE = 20
        
        @Volatile
        private var INSTANCE: NativeGattManager? = null
        
        fun getInstance(context: Context): NativeGattManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NativeGattManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    // ═══════════════════════════════════
    // Bluetooth State Enum
    // ═══════════════════════════════════
    enum class BluetoothState {
        NOT_AVAILABLE,
        DISABLED,
        ENABLED
    }
    
    // ═══════════════════════════════════
    // State Flows (same public API as MrdBleManager)
    // ═══════════════════════════════════
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private val _ringData = MutableStateFlow(RingData())
    val ringData: StateFlow<RingData> = _ringData.asStateFlow()
    
    private val _scanResults = MutableStateFlow<List<Ring>>(emptyList())
    val scanResults: StateFlow<List<Ring>> = _scanResults.asStateFlow()
    
    private val _measurementTimer = MutableStateFlow(MeasurementTimer())
    val measurementTimer: StateFlow<MeasurementTimer> = _measurementTimer.asStateFlow()
    
    // Coroutines
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var measurementJob: Job? = null
    private var connectionTimeoutJob: Job? = null
    private var keepAliveJob: Job? = null
    private var reconnectJob: Job? = null
    
    // Auto-reconnect
    private var autoReconnectEnabled = true
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3
    private var lastConnectedMac: String? = null
    private var lastConnectedName: String? = null
    private var userInitiatedDisconnect = false
    
    // BLE
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    
    init {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔧 NativeGattManager initialized!")
        Log.i(TAG, "   Pure Native BLE — No SDK")
        Log.i(TAG, "═══════════════════════════════════")
    }
    
    // ═══════════════════════════════════
    // Bluetooth State Checks
    // ═══════════════════════════════════
    
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null
    
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    
    fun getBluetoothState(): BluetoothState = when {
        bluetoothAdapter == null -> BluetoothState.NOT_AVAILABLE
        !bluetoothAdapter.isEnabled -> BluetoothState.DISABLED
        else -> BluetoothState.ENABLED
    }
    
    // ═══════════════════════════════════════════════════════════
    // SCANNING — Native BluetoothLeScanner
    // ═══════════════════════════════════════════════════════════
    
    @SuppressLint("MissingPermission")
    fun startScan(durationSeconds: Int = 6) {
        when (getBluetoothState()) {
            BluetoothState.NOT_AVAILABLE -> {
                Log.e(TAG, "❌ No Bluetooth hardware")
                return
            }
            BluetoothState.DISABLED -> {
                Log.e(TAG, "❌ Bluetooth is disabled")
                return
            }
            BluetoothState.ENABLED -> { /* proceed */ }
        }
        
        if (isScanning) {
            Log.w(TAG, "⚠️ Already scanning")
            return
        }
        
        isScanning = true
        _scanResults.value = emptyList()
        val foundDevices = mutableMapOf<String, Ring>()
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔍 STARTING NATIVE BLE SCAN")
        Log.i(TAG, "   Duration: ${durationSeconds}s")
        Log.i(TAG, "═══════════════════════════════════")
        
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "❌ BluetoothLeScanner not available")
            isScanning = false
            return
        }
        
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: return  // Skip unnamed devices
                val mac = device.address
                
                if (!foundDevices.containsKey(mac)) {
                    Log.i(TAG, "📡 Found: $name ($mac) RSSI: ${result.rssi}")
                    val ring = Ring(
                        name = name,
                        macAddress = mac,
                        isConnected = false
                    )
                    foundDevices[mac] = ring
                    _scanResults.value = foundDevices.values.toList()
                }
            }
            
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "❌ Scan failed: errorCode=$errorCode")
                isScanning = false
            }
        }
        
        scanner.startScan(scanCallback)
        
        // Auto-stop after duration
        handler.postDelayed({
            try {
                scanner.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.w(TAG, "Stop scan exception: ${e.message}")
            }
            isScanning = false
            Log.i(TAG, "🔍 Scan complete. Found ${foundDevices.size} devices")
        }, durationSeconds * 1000L)
    }
    
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(object : ScanCallback() {})
            } catch (e: Exception) {
                // Ignore
            }
            isScanning = false
            Log.d(TAG, "🔍 Scan stopped")
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // CONNECTION — Pure Native GATT
    // ═══════════════════════════════════════════════════════════
    
    @SuppressLint("MissingPermission")
    fun connectToDevice(macAddress: String, deviceName: String? = null) {
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
        Log.i(TAG, "🔌 NATIVE GATT Connecting to: $macAddress")
        Log.i(TAG, "   No SDK — Pure byte-array protocol")
        Log.i(TAG, "═══════════════════════════════════")
        
        // Store for auto-reconnect
        lastConnectedMac = macAddress
        lastConnectedName = deviceName
        userInitiatedDisconnect = false
        
        _connectionState.value = BleConnectionState.Connecting
        connectedDevice = device
        
        // 30-second connection timeout
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(30000)
            if (_connectionState.value is BleConnectionState.Connecting) {
                Log.e(TAG, "❌ Connection timeout after 30 seconds")
                disconnect()
                _connectionState.value = BleConnectionState.Error("Connection timeout")
            }
        }
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
    
    @SuppressLint("MissingPermission")
    fun disconnect() {
        Log.i(TAG, "🔌 Disconnecting (user initiated)")
        
        userInitiatedDisconnect = true
        reconnectJob?.cancel()
        reconnectAttempts = 0
        
        bluetoothGatt?.let {
            it.disconnect()
            it.close()
        }
        bluetoothGatt = null
        connectedDevice = null
        _connectionState.value = BleConnectionState.Disconnected
        _ringData.value = RingData()
    }
    
    // ═══════════════════════════════════════════════════════════
    // GATT CALLBACK — Heart of native BLE
    // ═══════════════════════════════════════════════════════════
    
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(TAG, "🔗 Connection state: newState=$newState, status=$status")
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✓ Connected! Discovering services...")
                    reconnectAttempts = 0
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val statusMessage = when (status) {
                        0 -> "Success (user initiated)"
                        8 -> "GATT_CONN_TIMEOUT"
                        19 -> "GATT_CONN_TERMINATE_PEER_USER"
                        22 -> "GATT_CONN_TERMINATE_LOCAL_HOST"
                        133 -> "GATT_ERROR"
                        else -> "Unknown: $status"
                    }
                    Log.w(TAG, "❌ Disconnected: $statusMessage")
                    
                    stopKeepAlive()
                    gatt.close()
                    bluetoothGatt = null
                    
                    handler.post {
                        val shouldReconnect = autoReconnectEnabled &&
                                !userInitiatedDisconnect &&
                                lastConnectedMac != null &&
                                reconnectAttempts < maxReconnectAttempts &&
                                status != 0
                        
                        if (shouldReconnect) {
                            _connectionState.value = BleConnectionState.Error(
                                "Connection lost. Reconnecting... (${reconnectAttempts + 1}/$maxReconnectAttempts)"
                            )
                            scheduleReconnect()
                        } else {
                            _connectionState.value = BleConnectionState.Disconnected
                            reconnectAttempts = 0
                        }
                    }
                }
            }
        }
        
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            connectionTimeoutJob?.cancel()
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ Services discovered")
                
                // Enable notifications on the notify characteristic
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
                        
                        Log.i(TAG, "📊 Connected! Requesting all initial data...")
                        // Request all data with staggered delays to avoid BLE congestion
                        handler.postDelayed({ requestBattery() }, 500)
                        handler.postDelayed({ requestFirmware() }, 1200)
                        handler.postDelayed({ requestHeartRate() }, 2000)
                        handler.postDelayed({ requestSteps() }, 2800)
                        handler.postDelayed({ requestSpO2() }, 3600)
                        handler.postDelayed({ requestBloodPressure() }, 4400)
                        handler.postDelayed({ requestStress() }, 5200)
                        handler.postDelayed({ startKeepAlive() }, 6000)
                    }
                } else {
                    Log.e(TAG, "❌ Notify characteristic not found!")
                }
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { data ->
                Log.d(TAG, "📥 Data received: ${bytesToHex(data)}")
                parseNotification(data)
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.d(TAG, "📥 Data received: ${bytesToHex(value)}")
            parseNotification(value)
        }
        
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✓ Write success")
            } else {
                Log.w(TAG, "⚠️ Write failed: status=$status")
            }
        }
        
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ MTU changed: $mtu")
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // NATIVE COMMANDS — Raw byte arrays (no SDK!)
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Build a 20-byte command packet.
     * Header is always 0xFC (byte 0). 
     * Caller specifies bytes 1..N, rest zero-padded.
     */
    private fun buildCommand(vararg bytes: Int): ByteArray {
        val cmd = ByteArray(PACKET_SIZE)
        cmd[0] = HEADER
        bytes.forEachIndexed { index, value ->
            cmd[index + 1] = value.toByte()
        }
        return cmd
    }
    
    /** Request battery level: FC 0F 06 */
    fun requestBattery() {
        writeData(buildCommand(0x0F, 0x06))
    }
    
    /** Request firmware version: FC 0F 05 */
    fun requestFirmware() {
        writeData(buildCommand(0x0F, 0x05))
    }
    
    /** Request last heart rate: FC 0A 00 */
    fun requestHeartRate() {
        writeData(buildCommand(0x0A, 0x00))
    }
    
    /** Request real-time steps: FC 03 00 */
    fun requestSteps() {
        Log.i(TAG, "📤 Requesting step data (native)...")
        writeData(buildCommand(0x03, 0x00))
    }
    
    /** Request last SpO2: FC 12 00 */
    fun requestSpO2() {
        writeData(buildCommand(0x12, 0x00))
    }
    
    /** Request last blood pressure: FC 11 00 */
    fun requestBloodPressure() {
        writeData(buildCommand(0x11, 0x00))
    }
    
    /** Request HRV/stress history: FC 5D 00 */
    fun requestStress() {
        Log.i(TAG, "📤 Requesting stress/HRV data (native)...")
        writeData(buildCommand(0x5D, 0x00))
    }
    
    /** Request temperature: FC 40 00 */
    fun requestTemperature() {
        writeData(buildCommand(0x40, 0x00))
    }
    
    /** Request sleep summary: FC 0C 01 */
    fun requestSleepHistory() {
        Log.i(TAG, "📤 Requesting sleep history (native)...")
        writeData(buildCommand(0x0C, 0x01))
    }
    
    /** Start HR test: FC 09 01 */
    fun startHeartRateTest() {
        writeData(buildCommand(0x09, 0x01))
    }
    
    /** Start BP test: FC 09 02 */
    fun startBloodPressureTest() {
        writeData(buildCommand(0x09, 0x02))
    }
    
    /** Start SpO2 test: FC 09 04 */
    fun startSpO2Test() {
        writeData(buildCommand(0x09, 0x04))
    }
    
    /** Start Stress test: FC 09 09 */
    fun startStressTest() {
        writeData(buildCommand(0x09, 0x09))
    }
    
    // ═══════════════════════════════════════════════════════════
    // TIMED MEASUREMENTS (same logic as MrdBleManager)
    // ═══════════════════════════════════════════════════════════
    
    fun startHeartRateMeasurement() {
        startTimedMeasurement(
            type = MeasurementType.HEART_RATE,
            requestFunc = { requestHeartRate() }
        )
    }
    
    fun startBloodPressureMeasurement() {
        startTimedMeasurement(
            type = MeasurementType.BLOOD_PRESSURE,
            requestFunc = { requestBloodPressure() }
        )
    }
    
    fun startSpO2Measurement() {
        startTimedMeasurement(
            type = MeasurementType.SPO2,
            requestFunc = { requestSpO2() }
        )
    }
    
    fun startStressMeasurement() {
        startTimedMeasurement(
            type = MeasurementType.STRESS,
            requestFunc = { requestStress() }
        )
    }
    
    private fun startTimedMeasurement(
        type: MeasurementType,
        durationSeconds: Int = 30,
        requestFunc: () -> Unit
    ) {
        stopMeasurement()
        
        Log.i(TAG, "⏱ Starting ${type.name} measurement for $durationSeconds seconds")
        
        measurementJob = CoroutineScope(Dispatchers.Main).launch {
            _measurementTimer.value = MeasurementTimer(
                isActive = true,
                measurementType = type,
                remainingSeconds = durationSeconds,
                totalSeconds = durationSeconds
            )
            
            for (second in durationSeconds downTo 0) {
                _measurementTimer.value = _measurementTimer.value.copy(
                    remainingSeconds = second
                )
                
                if (second % 2 == 0) {
                    requestFunc()
                }
                
                if (second > 0) {
                    delay(1000)
                }
            }
            
            Log.i(TAG, "⏱ ${type.name} measurement complete")
            _measurementTimer.value = MeasurementTimer()
        }
    }
    
    fun stopMeasurement() {
        measurementJob?.cancel()
        measurementJob = null
        _measurementTimer.value = MeasurementTimer()
    }
    
    // ═══════════════════════════════════════════════════════════
    // DATA PARSING — Pure byte-level (no SDK!)
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Main notification dispatcher.
     * Routes incoming 20-byte packets to type-specific parsers.
     */
    private fun parseNotification(data: ByteArray) {
        if (data.isEmpty()) return
        
        var validData = data
        
        // Handle packets missing the FC header (raw response from ring)
        if (data.isNotEmpty() && data[0] != HEADER) {
             val newData = ByteArray(data.size + 1)
             newData[0] = HEADER
             System.arraycopy(data, 0, newData, 1, data.size)
             validData = newData
             Log.d(TAG, "🔧 Normalized packet (prepended FC): ${bytesToHex(validData)}")
        }
        
        if (validData.size < 3) {
            Log.w(TAG, "⚠️ Packet too short: ${validData.size}")
            return
        }
        
        val metricId = validData[1].toInt() and 0xFF
        
        when (metricId) {
            0x0F -> parseSystemInfo(validData)       // Battery + Firmware
            0x0A -> parseHeartRate(validData)         // Heart Rate
            0x03 -> parseSteps(validData)             // Steps, Distance, Calories
            0x12 -> parseSpO2(validData)              // Blood Oxygen
            0x11 -> parseBloodPressure(validData)     // Blood Pressure
            0x5D -> parseHRV(validData)               // HRV → Stress
            0x40 -> parseTemperature(validData)       // Temperature
            0x0C -> parseSleepHistory(validData)       // Sleep History
            0x23 -> parseSleepSummary(validData)       // Sleep Summary
            0x09 -> parseHealthTest(validData)         // Health test responses
            else -> Log.d(TAG, "📦 Unknown metric: 0x${"%02X".format(metricId)}")
        }
    }
    
    // ───────────────────────────────────────────────
    // 3.5 System Info: Battery & Firmware (0x0F)
    // ───────────────────────────────────────────────
    private fun parseSystemInfo(data: ByteArray) {
        val subCommand = data[2].toInt() and 0xFF
        
        when (subCommand) {
            0x06 -> { // Battery
                if (data.size > 10) {
                    val battery = data[9].toInt() and 0xFF
                    val state = data[10].toInt() and 0xFF
                    val isCharging = state == 1
                    
                    if (battery in 0..100) {
                        Log.i(TAG, "🔋 Battery: $battery% ${if (isCharging) "⚡ CHARGING" else ""}")
                        handler.post {
                            _ringData.value = _ringData.value.copy(
                                battery = battery,
                                isCharging = isCharging,
                                lastUpdate = System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
            0x05 -> { // Firmware
                if (data.size > 9) {
                    val major = data[7].toInt() and 0xFF
                    val minor = data[8].toInt() and 0xFF
                    val patch = data[9].toInt() and 0xFF
                    val version = "$major.$minor.$patch"
                    
                    var type = ""
                    if (data.size > 12 && (data[10].toInt() and 0xFF) == 0x55) {
                        val t1 = bcdToInt(data[11].toInt() and 0xFF)
                        val t2 = bcdToInt(data[12].toInt() and 0xFF)
                        type = "$t1.$t2"
                    }
                    
                    Log.i(TAG, "📱 Firmware: v$version (type: $type)")
                    handler.post {
                        _ringData.value = _ringData.value.copy(
                            firmwareInfo = FirmwareInfo(
                                type = type,
                                version = version,
                                lastUpdate = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }
    
    // ───────────────────────────────────────────────
    // 3.7 Heart Rate (0x0A)
    // ───────────────────────────────────────────────
    private fun parseHeartRate(data: ByteArray) {
        if (data.size > 13) {
            val hr = data[13].toInt() and 0xFF
            
            if (hr in 40..220) {
                Log.i(TAG, "❤️ Heart Rate: $hr bpm")
                handler.post {
                    _ringData.value = _ringData.value.copy(
                        heartRate = hr,
                        lastUpdate = System.currentTimeMillis()
                    )
                }
            } else if (hr > 0) {
                Log.d(TAG, "❤️ HR out of range: $hr")
            }
        }
    }
    
    // ───────────────────────────────────────────────
    // 3.6 Steps (0x03)
    // ───────────────────────────────────────────────
    private fun parseSteps(data: ByteArray) {
        if (data.size < 11) return
        
        val subType = data[2].toInt() and 0xFF
        
        // Real-time/Last data (subType == 0x00)
        if (subType != 0x80.toInt() && subType != 0xC0.toInt()) {
            // Steps = byte3(data[3], data[4], data[5])
            val steps = byte3(data[3], data[4], data[5])
            val distance = byte3(data[6], data[7], data[8])
            val calories = byte3(data[9], data[10], data[11])
            
            Log.i(TAG, "👟 Steps: $steps (dist: ${distance}m, cal: ${calories})")
            handler.post {
                _ringData.value = _ringData.value.copy(
                    steps = steps,
                    distance = distance,
                    calories = calories,
                    lastUpdate = System.currentTimeMillis()
                )
            }
        } else if (subType == 0x80) {
            // History count
            val count = data[3].toInt() and 0xFF
            Log.i(TAG, "👟 Step history count: $count")
        }
    }
    
    // ───────────────────────────────────────────────
    // 3.2 SpO2 (0x12)
    // ───────────────────────────────────────────────
    private fun parseSpO2(data: ByteArray) {
        if (data.size > 14) {
            val intPart = data[13].toInt() and 0xFF
            val decPart = data[14].toInt() and 0xFF
            val spo2 = intPart + decPart / 10.0f
            
            if (spo2 in 80f..100f) {
                Log.i(TAG, "🫁 SpO2: $spo2%")
                handler.post {
                    _ringData.value = _ringData.value.copy(
                        spO2 = spo2,
                        lastUpdate = System.currentTimeMillis()
                    )
                }
            } else {
                Log.d(TAG, "🫁 SpO2 out of range: $spo2")
            }
        }
    }
    
    // ───────────────────────────────────────────────
    // 3.1 Blood Pressure (0x11)
    // ───────────────────────────────────────────────
    private fun parseBloodPressure(data: ByteArray) {
        if (data.size > 14) {
            val systolic = data[12].toInt() and 0xFF
            val diastolic = data[13].toInt() and 0xFF
            val hr = data[14].toInt() and 0xFF
            
            if (systolic > 0 && diastolic > 0) {
                val hrInfo = if (hr > 0) ", HR: $hr bpm" else ""
                Log.i(TAG, "🩺 BP: $systolic/$diastolic mmHg$hrInfo")
                handler.post {
                    _ringData.value = _ringData.value.copy(
                        bloodPressureSystolic = systolic,
                        bloodPressureDiastolic = diastolic,
                        bloodPressureHeartRate = hr,
                        lastUpdate = System.currentTimeMillis()
                    )
                }
            }
        }
    }
    
    // ───────────────────────────────────────────────
    // 3.3 HRV → Stress (0x5D)
    // ───────────────────────────────────────────────
    private fun parseHRV(data: ByteArray) {
        if (data.size > 12) {
            val hrvValue = data[12].toInt() and 0xFF
            
            if (hrvValue in 1..200) {
                Log.i(TAG, "😰 HRV/Stress: $hrvValue")
                handler.post {
                    _ringData.value = _ringData.value.copy(
                        stress = hrvValue,
                        lastUpdate = System.currentTimeMillis()
                    )
                }
            }
        }
    }
    
    // ───────────────────────────────────────────────
    // 3.8 Temperature (0x40)
    // ───────────────────────────────────────────────
    private fun parseTemperature(data: ByteArray) {
        if (data.size > 13) {
            val raw = (data[12].toInt() and 0xFF) * 256 + (data[13].toInt() and 0xFF)
            val tempC = raw / 10.0f
            
            if (tempC in 30f..45f) {
                Log.i(TAG, "🌡️ Temperature: ${tempC}°C")
                // Temperature not in RingData model yet, just log
            }
        }
    }
    
    // ───────────────────────────────────────────────
    // 3.4 Sleep (0x0C History, 0x23 Summary)
    // ───────────────────────────────────────────────
    private fun parseSleepSummary(data: ByteArray) {
        if (data.size > 17) {
            val deepSleep = ((data[12].toInt() and 0xFF) shl 8) or (data[13].toInt() and 0xFF)
            val lightSleep = ((data[14].toInt() and 0xFF) shl 8) or (data[15].toInt() and 0xFF)
            val awake = ((data[16].toInt() and 0xFF) shl 8) or (data[17].toInt() and 0xFF)
            val totalMinutes = deepSleep + lightSleep + awake
            
            if (totalMinutes > 0) {
                val quality = calculateSleepQuality(deepSleep, totalMinutes)
                Log.i(TAG, "😴 Sleep: ${totalMinutes}min (deep: $deepSleep, light: $lightSleep, quality: $quality%)")
                
                handler.post {
                    _ringData.value = _ringData.value.copy(
                        sleepData = SleepData(
                            totalMinutes = totalMinutes,
                            deepMinutes = deepSleep,
                            lightMinutes = lightSleep,
                            awakeMinutes = awake,
                            quality = quality
                        ),
                        lastUpdate = System.currentTimeMillis()
                    )
                }
            }
        }
    }
    
    private fun parseSleepHistory(data: ByteArray) {
        // Sleep history uses complex multi-packet structure
        // For now, log raw data; full implementation requires packet assembly
        Log.d(TAG, "😴 Sleep history packet: ${bytesToHex(data)}")
    }
    
    // ───────────────────────────────────────────────
    // Health Test Response (0x09)
    // ───────────────────────────────────────────────
    private fun parseHealthTest(data: ByteArray) {
        if (data.size > 2) {
            val testType = data[2].toInt() and 0xFF
            Log.d(TAG, "🧪 Health test response: type=0x${"%02X".format(testType)}")
            // Test results come back as the metric's own response (e.g., 0x0A for HR)
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // WRITE — Send raw byte arrays to ring
    // ═══════════════════════════════════════════════════════════
    
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
    
    // ═══════════════════════════════════════════════════════════
    // KEEP-ALIVE & AUTO-RECONNECT
    // ═══════════════════════════════════════════════════════════
    
    private var keepAliveIndex = 0
    
    private fun startKeepAlive() {
        stopKeepAlive()
        keepAliveIndex = 0
        
        keepAliveJob = ioScope.launch {
            while (isActive) {
                delay(10000)  // Every 10 seconds, cycle through one metric
                
                when (keepAliveIndex % 7) {
                    0 -> {
                        Log.d(TAG, "💓 Keep-alive: requesting battery")
                        requestBattery()
                    }
                    1 -> {
                        Log.d(TAG, "💓 Keep-alive: requesting heart rate")
                        requestHeartRate()
                    }
                    2 -> {
                        Log.d(TAG, "💓 Keep-alive: requesting steps")
                        requestSteps()
                    }
                    3 -> {
                        Log.d(TAG, "💓 Keep-alive: requesting SpO2")
                        requestSpO2()
                    }
                    4 -> {
                        Log.d(TAG, "💓 Keep-alive: requesting blood pressure")
                        requestBloodPressure()
                    }
                    5 -> {
                        Log.d(TAG, "💓 Keep-alive: requesting stress")
                        requestStress()
                    }
                    6 -> {
                        Log.d(TAG, "💓 Keep-alive: requesting firmware")
                        requestFirmware()
                    }
                }
                keepAliveIndex++
            }
        }
        Log.i(TAG, "💓 Keep-alive started (10s interval, cycling through all metrics)")
    }
    
    private fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }
    
    private fun scheduleReconnect() {
        val mac = lastConnectedMac ?: return
        
        reconnectJob?.cancel()
        reconnectJob = ioScope.launch {
            val delayMs = (2000L * (1 shl reconnectAttempts)).coerceAtMost(8000L)
            Log.i(TAG, "🔄 Scheduling reconnect in ${delayMs}ms (attempt ${reconnectAttempts + 1}/$maxReconnectAttempts)")
            
            delay(delayMs)
            reconnectAttempts++
            performReconnect(mac)
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun performReconnect(macAddress: String) {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "❌ Cannot reconnect: Bluetooth disabled")
            handler.post {
                _connectionState.value = BleConnectionState.Error("Bluetooth is disabled")
            }
            return
        }
        
        Log.i(TAG, "🔄 Reconnecting to: $macAddress (attempt $reconnectAttempts/$maxReconnectAttempts)")
        
        handler.post {
            _connectionState.value = BleConnectionState.Connecting
        }
        
        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        if (device == null) {
            Log.e(TAG, "❌ Device not found for reconnect")
            handler.post {
                _connectionState.value = BleConnectionState.Error("Device not found")
            }
            return
        }
        
        connectedDevice = device
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
    
    fun setAutoReconnectEnabled(enabled: Boolean) {
        autoReconnectEnabled = enabled
        Log.i(TAG, "🔄 Auto-reconnect ${if (enabled) "enabled" else "disabled"}")
        if (!enabled) {
            reconnectJob?.cancel()
            reconnectAttempts = 0
        }
    }
    
    fun isAutoReconnectEnabled(): Boolean = autoReconnectEnabled
    
    // ═══════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════
    
    /** 3-byte big-endian unsigned int: (b1 << 16) | (b2 << 8) | b3 */
    private fun byte3(b1: Byte, b2: Byte, b3: Byte): Int {
        return ((b1.toInt() and 0xFF) shl 16) or
               ((b2.toInt() and 0xFF) shl 8) or
               (b3.toInt() and 0xFF)
    }
    
    /** BCD decode: treats hex representation as decimal. E.g., 0x25 → 25 */
    private fun bcdToInt(value: Int): Int {
        return try {
            Integer.parseInt(Integer.toHexString(value and 0xFF))
        } catch (e: NumberFormatException) {
            0
        }
    }
    
    /** Calculate sleep quality (0-100) based on deep sleep ratio */
    private fun calculateSleepQuality(deepMinutes: Int, totalMinutes: Int): Int {
        if (totalMinutes == 0) return 0
        val deepPercentage = (deepMinutes.toFloat() / totalMinutes * 100).toInt()
        return when {
            deepPercentage >= 20 -> 90
            deepPercentage >= 15 -> 75
            deepPercentage >= 10 -> 60
            else -> 40
        }
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }
}
