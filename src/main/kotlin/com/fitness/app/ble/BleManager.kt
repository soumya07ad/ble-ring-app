package com.fitness.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleDataResponse
import com.yucheng.ycbtsdk.response.BleRealDataResponse
import com.yucheng.ycbtsdk.response.BleConnectResponse
import com.yucheng.ycbtsdk.Constants
import com.fitness.app.ble.models.BandBaseInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * BLE Manager - Bluetooth Low Energy communication handler for R9 Smart Ring
 * 
 * ## Architecture
 * This is a hybrid implementation that combines:
 * - **Native Android BLE Scanner**: For device discovery (YCBT SDK scan has compatibility issues)
 * - **YCBT SDK**: For connection management and data commands
 * 
 * ## Design Pattern
 * Thread-safe singleton with reactive StateFlow for UI updates.
 * Follows MVVM pattern - exposes StateFlows consumed by ViewModels.
 * 
 * ## Known Limitations (SDK V1.0.4)
 * - SDK callbacks may not fire reliably for R9 ring model
 * - State 10 (ReadWriteOK) never arrives; commands sent at State 7
 * - Auto-reconnect loop every ~15 seconds (cannot be disabled)
 * 
 * ## Usage
 * ```kotlin
 * val bleManager = BleManager.getInstance(context)
 * bleManager.initialize()
 * bleManager.connectionState.collect { state -> /* update UI */ }
 * bleManager.startScan()
 * bleManager.connectDevice(device)
 * ```
 * 
 * @see FitnessApplication SDK initialization
 * @see BleState Connection states
 * @see RingData Data model
 * 
 * @author DKGS Labs
 * @version 1.0.0
 */
class BleManager private constructor(private val context: Context) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Ring data from SDK callbacks
    private val _ringData = MutableStateFlow(RingData())
    val ringData: StateFlow<RingData> = _ringData.asStateFlow()
    
    // Connected device info - internal for MVVM repository access
    internal var connectedMacAddress: String = ""
    internal var connectedDeviceName: String = ""
    
    // Flag to prevent duplicate data retrieval
    private var dataRetrievalStarted = false

    private val discoveredDevices = mutableMapOf<String, BleDevice>() // MAC -> Device
    private var isInitialized = false
    
    // Native BLE scanner (replaces YCBT SDK scanning)
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var nativeScanCallback: ScanCallback? = null
    
    // Connection management
    private var isConnecting = false
    private var connectionTimeoutHandler: android.os.Handler? = null
    private var connectionStartTime: Long = 0
    
    // ═══════════════════════════════════════════════════════════════════════
    // NATIVE BLE GATT - For direct data reading (bypasses broken SDK callbacks)
    // ═══════════════════════════════════════════════════════════════════════
    private var nativeGatt: BluetoothGatt? = null
    private var nativeGattConnected = false
    
    // Periodic battery refresh
    private var batteryRefreshHandler: android.os.Handler? = null
    private var batteryRefreshRunnable: Runnable? = null
    
    // Standard BLE Service UUIDs
    companion object {
        private const val TAG = "BleManager"
        private const val DEFAULT_SCAN_DURATION = 6 // seconds
        private const val BATTERY_REFRESH_INTERVAL_MS = 60_000L // 60 seconds
        
        // Standard Battery Service (most BLE devices support this)
        private val BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")
        
        // Device Information Service
        private val DEVICE_INFO_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
        private val MANUFACTURER_NAME_UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb")
        private val MODEL_NUMBER_UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb")
        private val FIRMWARE_VERSION_UUID = UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb")
        
        // Heart Rate Service
        private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        private val HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        
        // Custom Ring Service (YC-specific) - May contain real battery data
        private val CUSTOM_SERVICE_FF01 = UUID.fromString("0000FF01-0000-1000-8000-00805f9b34fb")
        private val CUSTOM_CHAR_EFE3 = UUID.fromString("0000EFE3-0000-1000-8000-00805f9b34fb")
        
        // Handshake UUIDs (FEE7 service with FEC7/FEC9 characteristics)
        // Based on log: Service: 0000fee7-... has Char: 0000fec7/fec9/fea1
        private val HANDSHAKE_SERVICE_UUID = UUID.fromString("0000FEE7-0000-1000-8000-00805f9b34fb")
        private val HANDSHAKE_CHAR_FEC7 = UUID.fromString("0000FEC7-0000-1000-8000-00805f9b34fb")  // Write only
        private val HANDSHAKE_CHAR_FEC9 = UUID.fromString("0000FEC9-0000-1000-8000-00805f9b34fb")  // Read
        private val HANDSHAKE_CHAR_FEA1 = UUID.fromString("0000FEA1-0000-1000-8000-00805f9b34fb")  // Read/Notify (battery?)

        @Volatile
        @Suppress("StaticFieldLeak") // False positive - we use applicationContext
        private var instance: BleManager? = null

        fun getInstance(context: Context): BleManager {
            return instance ?: synchronized(this) {
                instance ?: BleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Initialize YCBT SDK - Call once during app startup
     * Safe to call multiple times (idempotent)
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "BLE already initialized")
            return
        }

        try {
            // SDK initialization
            YCBTClient.initClient(context, true)
            
            // CRITICAL FIX: Disable auto-reconnect to prevent 15-second disconnect loop
            // This allows the connection to reach state 10 (ReadWriteOK)
            YCBTClient.setReconnect(false)
            Log.i(TAG, "✓ Auto-reconnect DISABLED - connection will stay stable")
            
            // Register global connection state listener
            registerConnectionListener()
            
            isInitialized = true
            Log.d(TAG, "BLE initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BLE: ${e.message}", e)
        }
    }

    /**
     * Handle data pushed from ring via deviceToApp global callback
     * This is called by FitnessApplication when the ring pushes data
     * 
     * DEMO APP PATTERN: The ring pushes battery and health data automatically
     */
    fun handleDevicePushData(dataType: Int, dataMap: HashMap<*, *>) {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "PROCESSING DEVICE PUSH DATA")
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "dataType: $dataType")
        Log.i(TAG, "dataMap: $dataMap")
        
        try {
            // Check for battery value in the pushed data
            val dataObj = dataMap["data"]
            Log.d(TAG, "data object: $dataObj (type: ${dataObj?.javaClass?.name})")
            
            when (dataObj) {
                is Map<*, *> -> {
                    // Check for battery in data
                    val batteryValue = dataObj["deviceBatteryValue"]
                    if (batteryValue != null) {
                        val battery = batteryValue.toString().toIntOrNull() ?: 0
                        if (battery > 0) {
                            Log.i(TAG, "✓✓✓ GOT BATTERY FROM PUSH: $battery% ✓✓✓")
                            _ringData.value = _ringData.value.copy(
                                battery = battery,
                                lastUpdate = System.currentTimeMillis()
                            )
                        }
                    }
                    
                    // Check for other health data
                    dataObj.forEach { (key, value) ->
                        Log.d(TAG, "  $key = $value")
                        when (key) {
                            "sportStep" -> {
                                val steps = value?.toString()?.toIntOrNull() ?: 0
                                if (steps > 0) {
                                    _ringData.value = _ringData.value.copy(steps = steps, lastUpdate = System.currentTimeMillis())
                                }
                            }
                            "heartValue" -> {
                                val hr = value?.toString()?.toIntOrNull() ?: 0
                                if (hr > 0) {
                                    _ringData.value = _ringData.value.copy(heartRate = hr, lastUpdate = System.currentTimeMillis())
                                }
                            }
                        }
                    }
                }
                is String -> {
                    Log.d(TAG, "Data is string: $dataObj")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing push data: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NATIVE BLE GATT - Direct data reading (bypasses broken SDK callbacks)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Native GATT callback for direct BLE data reading
     * This bypasses the SDK's broken callback mechanism
     */
    private val nativeGattCallback = object : BluetoothGattCallback() {
        
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "NATIVE GATT: Connection state changed")
            Log.i(TAG, "status=$status, newState=$newState")
            Log.i(TAG, "═══════════════════════════════════")
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✓ NATIVE GATT: Connected!")
                    nativeGattConnected = true
                    // Discover services to find battery characteristic
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "✗ NATIVE GATT: Disconnected")
                    nativeGattConnected = false
                    nativeGatt?.close()
                    nativeGatt = null
                }
            }
        }
        
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "NATIVE GATT: Services discovered!")
            Log.i(TAG, "═══════════════════════════════════")
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed with status: $status")
                return
            }
            
            
            // DISABLED: Handshake doesn't work because it's sent on our native GATT,
            // but the SDK has its own internal GATT connection that doesn't see it.
            // writeHandshake(gatt)
            
            // Log all discovered services and queue reads for ALL readable characteristics
            // This is to debug the "100% battery" issue - maybe real battery is elsewhere?
            val priorityChars = mutableListOf<BluetoothGattCharacteristic>() // FF01/EFE3 first
            val otherChars = mutableListOf<BluetoothGattCharacteristic>()
            
            gatt?.services?.forEach { service ->
                val isCustomService = service.uuid == CUSTOM_SERVICE_FF01 || 
                    service.uuid.toString().contains("ff01", ignoreCase = true)
                Log.d(TAG, "Service: ${service.uuid}${if(isCustomService) " ★PRIORITY★" else ""}")
                
                service.characteristics.forEach { char ->
                    val props = char.properties
                    val isReadable = (props and BluetoothGattCharacteristic.PROPERTY_READ) != 0
                    val isNotify = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    val isIndicate = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    val isEFE3 = char.uuid == CUSTOM_CHAR_EFE3 || 
                        char.uuid.toString().contains("efe3", ignoreCase = true)
                    
                    Log.d(TAG, "  Char: ${char.uuid} " +
                            "[${if(isReadable) "READ" else ""}" +
                            "${if(isNotify) " NOTIFY" else ""}" +
                            "${if(isIndicate) " INDICATE" else ""}]" +
                            "${if(isEFE3) " ★EFE3★" else ""}")
                    
                    if (isReadable) {
                        // Prioritize FF01 service and EFE3 characteristic
                        if (isCustomService || isEFE3) {
                            priorityChars.add(char)
                        } else {
                            otherChars.add(char)
                        }
                    }
                }
            }
            
            // Combine with priority chars first
            val readableChars = priorityChars + otherChars
            Log.i(TAG, "Found ${readableChars.size} readable characteristics (${priorityChars.size} priority)")
            
            // Read them one by one with optimized 100ms delay (was 500ms)
            readableChars.forEachIndexed { index, char ->
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "READING DEBUG: ${char.uuid}")
                    gatt?.readCharacteristic(char)
                }, 100L * (index + 1))
            }

            // Also ensure periodic refresh starts
            Handler(Looper.getMainLooper()).post {
                startPeriodicBatteryRefresh()
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                handleCharacteristicRead(characteristic)
            } else {
                Log.w(TAG, "NATIVE GATT: Read failed, status=$status, uuid=${characteristic?.uuid}")
            }
        }
        
        // Modern callback (API 33+)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicReadValue(characteristic.uuid, value)
            } else {
                Log.w(TAG, "NATIVE GATT: Read failed, status=$status, uuid=${characteristic.uuid}")
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.value
        if (value != null) {
            handleCharacteristicReadValue(characteristic.uuid, value)
        }
    }
    
    private fun handleCharacteristicReadValue(uuid: UUID, value: ByteArray) {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "NATIVE GATT: READ RESPONSE")
        Log.i(TAG, "UUID: $uuid")
        Log.i(TAG, "Value (Hex): ${value.joinToString("") { "%02x".format(it) }}")
        Log.i(TAG, "Value (Int): [${value.joinToString(", ") { it.toInt().and(0xFF).toString() }}]")
        Log.i(TAG, "═══════════════════════════════════")
        
        // Check if ANY value matches the user's "70%" claim (decimal 70 = hex 46)
        if (value.any { it.toInt().and(0xFF) in 65..75 }) {
            Log.e(TAG, "!!! FOUND POTENTIAL BATTERY VALUE (65-75 range) at UUID: $uuid !!!")
        }

        val uuidString = uuid.toString().lowercase()
        
        when {
            // Standard Battery Level (2A19)
            uuidString.contains("2a19") -> {
                if (value.isNotEmpty()) {
                    val battery = value[0].toInt() and 0xFF
                    Log.i(TAG, "✓✓✓ BATTERY FROM NATIVE BLE (2A19): $battery% ✓✓✓")
                    if (battery in 1..100) {
                        _ringData.value = _ringData.value.copy(
                            battery = battery,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
            }
            
            // FEA1 - DISABLED: byte[4] showed 160% which is impossible
            // The data is dynamic and NOT battery. Just log for debugging.
            uuidString.contains("fea1") -> {
                if (value.size >= 5) {
                    Log.d(TAG, "FEA1 raw data: ${value.joinToString(", ") { (it.toInt() and 0xFF).toString() }}")
                    // DO NOT update battery from FEA1 - values are unreliable
                }
            }
            
            // FF01 - Another potential battery source
            uuidString.contains("ff01") -> {
                if (value.isNotEmpty()) {
                    val potentialBattery = value[0].toInt() and 0xFF
                    Log.d(TAG, "FF01 byte[0]: $potentialBattery")
                    if (potentialBattery in 1..100 && _ringData.value.battery == null) {
                        _ringData.value = _ringData.value.copy(
                            battery = potentialBattery,
                            lastUpdate = System.currentTimeMillis()
                        )
                        Log.i(TAG, "✓✓✓ BATTERY FROM FF01: $potentialBattery% ✓✓✓")
                    }
                }
            }
            
            // Heart Rate Measurement (2A37)
            uuidString.contains("2a37") -> {
                if (value.size >= 2) {
                    val flags = value[0].toInt() and 0xFF
                    val hrIndex = if (flags and 0x01 == 0) 1 else 2
                    if (value.size > hrIndex) {
                        val hr = value[hrIndex].toInt() and 0xFF
                        if (hr in 30..220) {
                            Log.i(TAG, "✓✓✓ HEART RATE: $hr bpm ✓✓✓")
                            _ringData.value = _ringData.value.copy(
                                heartRate = hr,
                                lastUpdate = System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Connect using native BLE GATT (bypasses SDK for data reading)
     * Called after SDK connects to state 7
     */
    @SuppressLint("MissingPermission")
    private fun connectNativeGatt(macAddress: String) {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "STARTING NATIVE GATT CONNECTION")
        Log.i(TAG, "MAC: $macAddress")
        Log.i(TAG, "═══════════════════════════════════")
        
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            
            if (bluetoothAdapter == null) {
                Log.e(TAG, "BluetoothAdapter not available")
                return
            }
            
            val device = bluetoothAdapter.getRemoteDevice(macAddress)
            
            // Close existing GATT if any
            nativeGatt?.close()
            nativeGatt = null
            nativeGattConnected = false
            
            // Connect with TRANSPORT_LE for BLE devices
            nativeGatt = device.connectGatt(
                context,
                false,  // autoConnect = false for faster connection
                nativeGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
            
            Log.i(TAG, "Native GATT connection initiated...")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting native GATT: ${e.message}", e)
        }
    }

    // (Helper for readBatteryFromNativeGatt - no longer needed as we read ALL, but keeping signature valid if called elsewhere)
    @SuppressLint("MissingPermission")
    private fun readBatteryFromNativeGatt(gatt: BluetoothGatt?) {
         // This function is now superseded by the bulk read in onServicesDiscovered
         // But we can keep it as a wrapper to read specific 2A19 if needed
         val batteryService = gatt?.getService(BATTERY_SERVICE_UUID)
         val batteryChar = batteryService?.getCharacteristic(BATTERY_LEVEL_UUID)
         if (batteryChar != null) {
             gatt?.readCharacteristic(batteryChar)
         }
    }
    
    /**
     * RAW GATT HANDSHAKE - Send timestamp/keep-alive data immediately after connect
     * Many Chinese BLE devices require a write within ~5 seconds or they disconnect.
     * Based on ChatGPT research: "the device disconnects unless there's an operation almost immediately"
     */
    @SuppressLint("MissingPermission")
    private fun writeHandshake(gatt: BluetoothGatt?) {
        if (gatt == null) {
            Log.w(TAG, "Cannot write handshake: gatt is null")
            return
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "WRITING RAW GATT HANDSHAKE")
        Log.i(TAG, "═══════════════════════════════════")
        
        // Try to find the FEE7 service (handshake service)
        val handshakeService = gatt.getService(HANDSHAKE_SERVICE_UUID)
        if (handshakeService == null) {
            Log.w(TAG, "FEE7 handshake service not found, trying with lowercase...")
            // Try alternate approach - iterate and find
            gatt.services?.forEach { service ->
                if (service.uuid.toString().contains("fee7", ignoreCase = true)) {
                    Log.i(TAG, "Found FEE7 service via iteration")
                    writeToFEC7Char(gatt, service)
                    return
                }
            }
            Log.w(TAG, "FEE7 service not found at all - skipping handshake")
            return
        }
        
        writeToFEC7Char(gatt, handshakeService)
    }
    
    @SuppressLint("MissingPermission")
    private fun writeToFEC7Char(gatt: BluetoothGatt, service: BluetoothGattService) {
        // FEC7 is the write-only characteristic in FEE7 service
        var fec7Char = service.getCharacteristic(HANDSHAKE_CHAR_FEC7)
        
        if (fec7Char == null) {
            // Try alternate approach
            service.characteristics.forEach { char ->
                if (char.uuid.toString().contains("fec7", ignoreCase = true)) {
                    fec7Char = char
                }
            }
        }
        
        if (fec7Char == null) {
            Log.w(TAG, "FEC7 characteristic not found - trying EFE1 (write char)")
            // Some devices use EFE1 as write characteristic
            gatt.services?.forEach { svc ->
                svc.characteristics.forEach { char ->
                    val props = char.properties
                    val isWritable = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                                     (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                    if (isWritable && char.uuid.toString().contains("efe1", ignoreCase = true)) {
                        Log.i(TAG, "Found EFE1 write characteristic")
                        fec7Char = char
                    }
                }
            }
        }
        
        if (fec7Char == null) {
            Log.w(TAG, "No writable handshake characteristic found")
            return
        }
        
        Log.i(TAG, "Writing handshake to: ${fec7Char!!.uuid}")
        
        // Build timestamp handshake data
        // Format based on common YC SDK patterns: [year-2000, month, day, hour, minute, second]
        val calendar = java.util.Calendar.getInstance()
        val year = (calendar.get(java.util.Calendar.YEAR) - 2000).toByte()
        val month = (calendar.get(java.util.Calendar.MONTH) + 1).toByte()
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH).toByte()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY).toByte()
        val minute = calendar.get(java.util.Calendar.MINUTE).toByte()
        val second = calendar.get(java.util.Calendar.SECOND).toByte()
        
        // Common handshake format: command header + timestamp
        val handshakeData = byteArrayOf(
            0x01,  // Command type (time sync)
            year, month, day, hour, minute, second
        )
        
        Log.d(TAG, "Handshake data: ${handshakeData.joinToString(" ") { "%02X".format(it) }}")
        
        try {
            // Use deprecated setValue for broader API compatibility
            @Suppress("DEPRECATION")
            fec7Char!!.setValue(handshakeData)
            
            val success = gatt.writeCharacteristic(fec7Char!!)
            Log.i(TAG, "Handshake write result: $success")
            
            if (success) {
                Log.i(TAG, "✓ HANDSHAKE SENT - Ring should now stay connected!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing handshake: ${e.message}", e)
        }
    }
    
    /**
     * Disconnect native GATT
     */
    @SuppressLint("MissingPermission")
    private fun disconnectNativeGatt() {
        Log.d(TAG, "Disconnecting native GATT...")
        stopPeriodicBatteryRefresh()
        nativeGatt?.disconnect()
        nativeGatt?.close()
        nativeGatt = null
        nativeGattConnected = false
    }
    
    /**
     * PUBLIC: Refresh battery level on demand
     * Call this from ViewModel/Repository to get fresh battery reading
     */
    @SuppressLint("MissingPermission")
    fun refreshBattery() {
        if (nativeGattConnected && nativeGatt != null) {
            Log.i(TAG, "🔋 Refreshing battery via native GATT...")
            readBatteryFromNativeGatt(nativeGatt)
        } else {
            Log.w(TAG, "Cannot refresh battery - native GATT not connected")
        }
    }
    
    /**
     * Start periodic battery refresh (every 60 seconds)
     * Uses SDK getDeviceInfo for accurate battery (native BLE returns stale cached value)
     */
    private fun startPeriodicBatteryRefresh() {
        Log.i(TAG, "Starting periodic battery refresh (every ${BATTERY_REFRESH_INTERVAL_MS/1000}s) via SDK")
        
        batteryRefreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
        batteryRefreshRunnable = object : Runnable {
            override fun run() {
                // Check SDK connection state (more reliable than native GATT)
                val isConnected = try {
                    YCBTClient.connectState() == Constants.BLEState.ReadWriteOK
                } catch (e: Exception) {
                    false
                }
                
                if (isConnected) {
                    Log.d(TAG, "⏰ Periodic battery refresh via SDK getDeviceInfo")
                    refreshBatteryViaSdk()
                    batteryRefreshHandler?.postDelayed(this, BATTERY_REFRESH_INTERVAL_MS)
                } else {
                    Log.w(TAG, "Periodic refresh skipped - SDK not connected")
                    stopPeriodicBatteryRefresh()
                }
            }
        }
        
        // Start first refresh after the interval (initial read happens on connect)
        batteryRefreshHandler?.postDelayed(batteryRefreshRunnable!!, BATTERY_REFRESH_INTERVAL_MS)
    }
    
    /**
     * Refresh battery using SDK getDeviceInfo + readDeviceInfo (cached data)
     * 
     * SDK DOCUMENTATION APPROACH:
     * 1. Call getDeviceInfo() to populate SDK's internal cache
     * 2. Wait 2-3 seconds for cache to populate
     * 3. Call readDeviceInfo() to read battery from cache SYNCHRONOUSLY (no callback!)
     */
    private fun refreshBatteryViaSdk() {
        Log.i(TAG, "⚡ refreshBatteryViaSdk() CALLED")
        try {
            // STEP 1: Call getDeviceInfo() to populate cache
            YCBTClient.getDeviceInfo(object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.i(TAG, "🔋🔋🔋 SDK CALLBACK FIRED! code=$code, resultMap=${resultMap != null}")
                    
                    if (code == 0 && resultMap != null) {
                        try {
                            Log.d(TAG, "🔋 ResultMap raw: $resultMap")
                            
                            // METHOD 1: Direct HashMap access (most reliable)
                            val dataMap = resultMap["data"] as? Map<*, *>
                            if (dataMap != null) {
                                val batteryValue = dataMap["deviceBatteryValue"]?.toString()?.toIntOrNull()
                                val deviceVersion = dataMap["deviceVersion"]?.toString()
                                
                                Log.i(TAG, "🔋 Direct HashMap: battery=$batteryValue, version=$deviceVersion")
                                
                                if (batteryValue != null && batteryValue in 1..100) {
                                    Log.i(TAG, "✓✓✓ BATTERY FROM SDK (direct HashMap): $batteryValue% ✓✓✓")
                                    _ringData.value = _ringData.value.copy(
                                        battery = batteryValue,
                                        lastUpdate = System.currentTimeMillis()
                                    )
                                    return  // Success, exit
                                }
                            }
                            
                            // METHOD 2: Gson with proper JSON conversion
                            val gson = com.google.gson.Gson()
                            val jsonStr = gson.toJson(resultMap)
                            Log.d(TAG, "🔋 Device info JSON: $jsonStr")
                            
                            val bandInfo = gson.fromJson(jsonStr, BandBaseInfo::class.java)
                            
                            if (bandInfo?.data != null) {
                                val batteryStr = bandInfo.data.deviceBatteryValue
                                val battery = batteryStr?.toIntOrNull() ?: 0
                                
                                Log.i(TAG, "✓✓✓ BATTERY FROM SDK (Gson): $battery% ✓✓✓")
                                
                                if (battery in 1..100) {
                                    _ringData.value = _ringData.value.copy(
                                        battery = battery,
                                        lastUpdate = System.currentTimeMillis()
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing device info: ${e.message}", e)
                        }
                    } else {
                        Log.w(TAG, "🔋 SDK returned code=$code or null resultMap")
                    }
                }
            })
            Log.i(TAG, "⚡ getDeviceInfo() COMMAND SENT")
            
            // STEP 2: Wait 3 seconds, then try reading from SDK cache
            // Per SDK docs: readDeviceInfo() reads CACHED data synchronously
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.i(TAG, "🗂️ Attempting to read battery from SDK cache (readDeviceInfo)...")
                    
                    // Try reflection to call YCBTClient.readDeviceInfo()
                    val ycbtClass = YCBTClient::class.java
                    val method = ycbtClass.getDeclaredMethod("readDeviceInfo", String::class.java)
                    
                    // Try different possible constant names
                    val possibleKeys = listOf(
                        "deviceBatteryValue",
                        "DEVICEBATTERY",
                        "battery",
                        "BATTERY"
                    )
                    
                    for (key in possibleKeys) {
                        try {
                            val result = method.invoke(null, key)
                            Log.d(TAG, "🗂️ readDeviceInfo('$key') = $result")
                            
                            if (result != null) {
                                val batteryValue = result.toString().toIntOrNull()
                                if (batteryValue != null && batteryValue in 1..100) {
                                    Log.i(TAG, "✓✓✓ BATTERY FROM SDK CACHE: $batteryValue% ✓✓✓")
                                    _ringData.value = _ringData.value.copy(
                                        battery = batteryValue,
                                        lastUpdate = System.currentTimeMillis()
                                    )
                                    return@postDelayed  // Success, exit
                                }
                            }
                        } catch (e: Exception) {
                            // Key not found, try next
                        }
                    }
                    
                    Log.w(TAG, "🗂️ Could not read battery from SDK cache")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error reading from SDK cache: ${e.message}", e)
                }
            }, 3000)  // Wait 3 seconds for cache to populate
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calling getDeviceInfo: ${e.message}", e)
        }
    }
    
    /**
     * Stop periodic battery refresh
     */
    private fun stopPeriodicBatteryRefresh() {
        Log.d(TAG, "Stopping periodic battery refresh")
        batteryRefreshRunnable?.let { batteryRefreshHandler?.removeCallbacks(it) }
        batteryRefreshHandler = null
        batteryRefreshRunnable = null
    }

    /**
     * Start BLE scan using NATIVE Android BLE scanner
     * YCBT SDK scanning doesn't work for fresh devices - only returns scan finished callback
     * @param scanDuration Duration in seconds (default: 6)
     */
    fun startScan(scanDuration: Int = DEFAULT_SCAN_DURATION) {
        if (!isInitialized) {
            Log.e(TAG, "BLE not initialized. Call initialize() first.")
            _scanState.value = ScanState.Error("BLE not initialized")
            return
        }

        // Get native Android BLE scanner
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner not available")
            _scanState.value = ScanState.Error("Bluetooth not available")
            return
        }

        discoveredDevices.clear()
        _scanState.value = ScanState.Scanning

        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "STARTING NATIVE BLE SCAN")
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "Scan duration: $scanDuration seconds")
        Log.i(TAG, "Using Native Android BluetoothLeScanner")

        // Create native scan callback
        nativeScanCallback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                processScanResult(result)
            }

            @SuppressLint("MissingPermission")
            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { processScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error: $errorCode")
                _scanState.value = ScanState.Error("Scan failed: $errorCode")
            }
        }

        // Start native BLE scan
        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            // CRITICAL: Use empty filter list instead of null for Xiaomi/MIUI compatibility
            // Xiaomi requires BLUETOOTH_PRIVILEGED for null filters, but accepts empty list
            val filters = emptyList<android.bluetooth.le.ScanFilter>()
            
            // Start scan with permission suppression
            @Suppress("MissingPermission") // Permissions checked by caller
            val scanStarted = bluetoothLeScanner?.startScan(filters, settings, nativeScanCallback)
            Log.i(TAG, "✓ Native BLE scan started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan: ${e.message}", e)
            _scanState.value = ScanState.Error("Failed to start scan: ${e.message}")
            return
        }

        // Schedule scan stop
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            stopScan()
            
            val finalDevices = discoveredDevices.values.toList()
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "SCAN FINISHED")
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "Total devices discovered: ${finalDevices.size}")
            
            if (finalDevices.isEmpty()) {
                Log.w(TAG, "⚠️  NO DEVICES FOUND")
                Log.w(TAG, "   Possible reasons:")
                Log.w(TAG, "   1. Ring not in pairing mode")
                Log.w(TAG, "   2. Ring too far away")
                Log.w(TAG, "   3. Bluetooth interference")
                Log.w(TAG, "   4. Ring connected to another device")
            }
            
            _scanState.value = if (finalDevices.isEmpty()) {
                ScanState.Error("No devices found")
            } else {
                ScanState.DevicesFound(finalDevices)
            }
        }, scanDuration * 1000L)
    }

    /**
     * Process individual scan result from native BLE scanner
     */
    @SuppressLint("MissingPermission")
    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val macAddress = device.address
        
        // Get device name from scan record or device
        val deviceName = result.scanRecord?.deviceName ?: device.name ?: "Unknown Device"
        val rssi = result.rssi

        Log.d(TAG, "✓ DEVICE FOUND")
        Log.d(TAG, "   MAC: $macAddress")
        Log.d(TAG, "   Name: $deviceName")
        Log.d(TAG, "   RSSI: $rssi dBm")
        
        // Log Manufacturer Data for battery discovery (some rings advertise battery here)
        result.scanRecord?.let { scanRecord ->
            val manufacturerData = scanRecord.manufacturerSpecificData
            if (manufacturerData != null && manufacturerData.size() > 0) {
                Log.i(TAG, "   ═══ MANUFACTURER DATA ═══")
                for (i in 0 until manufacturerData.size()) {
                    val companyId = manufacturerData.keyAt(i)
                    val data = manufacturerData.valueAt(i)
                    val hexDump = data.joinToString("") { "%02X".format(it) }
                    Log.i(TAG, "   CompanyID: 0x${companyId.toString(16).uppercase().padStart(4, '0')}")
                    Log.i(TAG, "   Data (Hex): $hexDump")
                    Log.i(TAG, "   Data (Dec): [${data.joinToString(", ") { (it.toInt() and 0xFF).toString() }}]")
                    
                    // Check if any byte is 70 (potential battery)
                    data.forEachIndexed { idx, byte ->
                        val intVal = byte.toInt() and 0xFF
                        if (intVal == 70) {
                            Log.e(TAG, "   !!! FOUND 70 (0x46) AT INDEX $idx - POSSIBLE BATTERY !!!")
                        }
                    }
                }
            }
            
            // Also log service UUIDs being advertised
            scanRecord.serviceUuids?.forEach { uuid ->
                Log.d(TAG, "   Advertised Service: $uuid")
            }
        }

        // Check for duplicates
        if (discoveredDevices.containsKey(macAddress)) {
            Log.d(TAG, "   Device already in list, updating RSSI")
            // Update RSSI if device already exists
            discoveredDevices[macAddress] = BleDevice(deviceName, macAddress, rssi)
            _scanState.value = ScanState.DevicesFound(discoveredDevices.values.toList())
            return
        }

        // Add to discovered devices
        val bleDevice = BleDevice(deviceName, macAddress, rssi)
        discoveredDevices[macAddress] = bleDevice

        // Emit updated list
        _scanState.value = ScanState.DevicesFound(discoveredDevices.values.toList())
        
        Log.i(TAG, "✓ NEW DEVICE ADDED (Total: ${discoveredDevices.size})")
    }

    /**
     * Stop native BLE scan
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        nativeScanCallback?.let {
            try {
                bluetoothLeScanner?.stopScan(it)
                Log.d(TAG, "Native BLE scan stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan: ${e.message}")
            }
        }
        nativeScanCallback = null
    }

    /**
     * Connect to a BLE device using MAC address
     * @param macAddress Device MAC address
     * @param rssi Signal strength (for logging only)
     * @param deviceName Device name (for logging only)
     */
    fun connectDevice(macAddress: String, rssi: Int = -100, deviceName: String? = null) {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "CONNECTION ATTEMPT")
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "MAC Address: $macAddress")
        Log.i(TAG, "Device Name: ${deviceName ?: "NULL"}")
        Log.i(TAG, "RSSI: $rssi dBm")
        Log.i(TAG, "Timestamp: ${System.currentTimeMillis()}")
        
        // WARNING: NULL device name (not blocking)
        if (deviceName == null || deviceName == "NULL" || deviceName.isBlank()) {
            Log.w(TAG, "⚠️  Device name is NULL or blank")
            Log.w(TAG, "   → Ring may be sleeping or not advertising name")
            Log.w(TAG, "   → Attempting connection anyway...")
        }
        
        // WARNING: Weak signal (not blocking)
        if (rssi < -85) {
            Log.w(TAG, "⚠️  Weak signal detected: $rssi dBm")
            Log.w(TAG, "   → Connection may be unstable")
            Log.w(TAG, "   → Attempting connection anyway...")
        }
        
        // Guard: Already connecting
        if (isConnecting) {
            Log.w(TAG, "Connection already in progress, ignoring duplicate request")
            return
        }
        
        // Guard: BLE not initialized
        if (!isInitialized) {
            Log.e(TAG, "BLE not initialized")
            _connectionState.value = ConnectionState.Error("BLE not initialized")
            return
        }

        Log.i(TAG, "✓ Pre-connection validation passed")
        
        // Reset data retrieval flag for new connection
        dataRetrievalStarted = false
        _ringData.value = RingData() // Reset ring data
        
        // CRITICAL: Disconnect before reconnecting to reset SDK internal state
        // The SDK has an internal isRepeat flag that causes reconnects every 15 seconds
        try {
            YCBTClient.disconnectBle()
            Log.i(TAG, "   Disconnected to reset SDK state")
        } catch (e: Exception) {
            Log.w(TAG, "   disconnectBle failed: ${e.message}")
        }
        
        // CRITICAL: Stop scan before connecting
        // WHY: Scan may interfere with connection stability
        stopScan()
        Log.d(TAG, "   Scan stopped before connection attempt")
        
        // CRITICAL VENDOR REQUIREMENT: Android system bonding BEFORE SDK connect
        // WHY: YCBT SDK requires device to be bonded at OS level first
        // This is why other apps work - they trigger bonding first
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            @SuppressLint("MissingPermission") // Permissions checked by caller (ViewModel)
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(macAddress)
            
            if (bluetoothDevice != null) {
                @SuppressLint("MissingPermission") // Permissions checked by caller (ViewModel)
                val bondState = bluetoothDevice.bondState
                Log.i(TAG, "   Current bond state: $bondState")
                
                when (bondState) {
                    BluetoothDevice.BOND_NONE -> {
                        Log.i(TAG, "   → Device not bonded, initiating Android system bonding...")
                        @SuppressLint("MissingPermission") // Permissions checked by caller
                        val bondResult = bluetoothDevice.createBond()
                        Log.i(TAG, "   → createBond() returned: $bondResult")
                        
                        if (!bondResult) {
                            Log.e(TAG, "   ✗ Failed to initiate bonding")
                            _connectionState.value = ConnectionState.Error(
                                "Failed to initiate pairing. Please try again."
                            )
                            return
                        }
                        
                        // Wait for bonding to complete (user may need to confirm PIN)
                        Log.i(TAG, "   → Waiting for user to confirm pairing...")
                        // Note: Bonding happens asynchronously, SDK connect will proceed
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Log.i(TAG, "   → Device currently bonding, waiting...")
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        Log.i(TAG, "   → Device already bonded ✓")
                    }
                }
            } else {
                Log.w(TAG, "   ⚠️  Could not get BluetoothDevice for MAC: $macAddress")
            }
        } catch (e: Exception) {
            Log.e(TAG, "   ✗ Bonding check failed: ${e.message}", e)
            // Continue anyway - SDK might handle it
        }
        
        isConnecting = true
        connectionStartTime = System.currentTimeMillis()
        _connectionState.value = ConnectionState.Connecting
        
        // CRITICAL: Set MAC address BEFORE connecting so it's available when state 7 arrives
        // Native GATT needs this to connect when state 7 is received in global listener
        connectedMacAddress = macAddress
        connectedDeviceName = deviceName ?: "Ring"
        Log.i(TAG, "   Set connectedMacAddress=$connectedMacAddress for native GATT")
        
        Log.i(TAG, "   Starting connection timeout (15s)...")
        Log.i(TAG, "   → Calling YCBTClient.connectBle($macAddress)")
        // Start connection timeout (15 seconds)
        startConnectionTimeout(macAddress)

        YCBTClient.connectBle(macAddress, object : BleConnectResponse {
            override fun onConnectResponse(code: Int) {
                val elapsed = System.currentTimeMillis() - connectionStartTime
                Log.d(TAG, "Connection callback received: code=$code after ${elapsed}ms")
                
                // CRITICAL: YCBT SDK uses multiple state codes during connection
                // Code 1 = ReadWriteOK (legacy) - MAY BE ONLY CALLBACK in some cases
                // Code 6 = Connected (discovering services)
                // Code 7 = Fully connected (services discovered, MTU negotiated)
                // Code 2 = Disconnect
                // Code 3 = Disconnected
                // Code 4, 5 = Intermediate states (ignore)
                
                when (code) {
                    1 -> {
                        // State 1 = ReadWriteOK - BACKUP trigger when state 7 doesn't arrive
                        // This is critical because state 7 is not consistently received!
                        cancelConnectionTimeout()
                        isConnecting = false
                        _connectionState.value = ConnectionState.Connected
                        Log.i(TAG, "✓ Connected successfully (state 1) to $macAddress")
                        Log.i(TAG, "═══════════════════════════════════")
                        // Call onConnectionSuccess as BACKUP (flag prevents duplicates)
                        onConnectionSuccess(macAddress, deviceName)
                    }
                    6 -> {
                        // Connected, discovering services
                        Log.i(TAG, "✓ Connected (state 6) - discovering services...")
                        // Don't cancel timeout yet - wait for state 7
                    }
                    7 -> {
                        // FULLY CONNECTED - services discovered, MTU negotiated
                        // This is the PREFERRED trigger, but may not always arrive
                        cancelConnectionTimeout()
                        isConnecting = false
                        _connectionState.value = ConnectionState.Connected
                        Log.i(TAG, "✓ FULLY CONNECTED (state 7) to $macAddress")
                        Log.i(TAG, "   Services discovered, MTU negotiated")
                        Log.i(TAG, "═══════════════════════════════════")
                        // Don't call onConnectionSuccess here - global listener handles state 7
                    }
                    2, 3 -> {
                        // Disconnect states
                        cancelConnectionTimeout()
                        isConnecting = false
                        
                        if (elapsed < 2000) {
                            Log.w(TAG, "✗ Immediate disconnect (state $code) after ${elapsed}ms")
                            Log.w(TAG, "   → Ring likely connected to another device/app")
                            _connectionState.value = ConnectionState.Error(
                                "This ring is currently connected to another device or app. Please disconnect it first and try again."
                            )
                        } else {
                            Log.w(TAG, "✗ Disconnected (state $code) after ${elapsed}ms")
                            _connectionState.value = ConnectionState.Error(
                                "Connection lost. Please try again."
                            )
                        }
                        Log.i(TAG, "═══════════════════════════════════")
                    }
                    4, 5 -> {
                        // Intermediate connection states - ignore
                        Log.d(TAG, "   Intermediate state $code - continuing...")
                    }
                    else -> {
                        // Unknown state
                        Log.w(TAG, "   Unknown state code: $code")
                    }
                }
            }
        })
    }
    
    /**
     * Start connection timeout
     * WHY: Prevent indefinite waiting if device doesn't respond
     */
    private fun startConnectionTimeout(macAddress: String) {
        connectionTimeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        connectionTimeoutHandler?.postDelayed({
            if (isConnecting) {
                val elapsed = System.currentTimeMillis() - connectionStartTime
                Log.e(TAG, "✗ Connection timeout after ${elapsed}ms")
                Log.e(TAG, "   Device: $macAddress")
                isConnecting = false
                _connectionState.value = ConnectionState.Timeout
                
                // Attempt to cancel SDK connection
                try {
                    YCBTClient.disconnectBle()
                    Log.d(TAG, "Cancelled connection attempt")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cancel connection: ${e.message}")
                }
            }
        }, 20000) // 20 seconds (state 1 may arrive at ~15s)
    }
    
    /**
     * Cancel connection timeout
     */
    private fun cancelConnectionTimeout() {
        connectionTimeoutHandler?.removeCallbacksAndMessages(null)
        connectionTimeoutHandler = null
    }

    /**
     * Disconnect from current BLE device
     */
    fun disconnect() {
        if (!isInitialized) return
        
        // Disconnect native GATT first
        disconnectNativeGatt()
        
        YCBTClient.disconnectBle()
        _connectionState.value = ConnectionState.Disconnected
        Log.d(TAG, "Disconnect requested")
    }

    /**
     * Check current connection state from SDK
     */
    fun isConnected(): Boolean {
        return if (isInitialized) {
            try {
                // connectState() returns Int representing connection state
                val state = YCBTClient.connectState()
                // Compare with ReadWriteOK constant
                state == Constants.BLEState.ReadWriteOK
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get connection state: ${e.message}")
                false
            }
        } else {
            false
        }
    }

    /**
     * Register global BLE connection state listener
     * Handles UI state updates for reconnect and disconnect events
     * 
     * FIX: Wait for state 1 (ReadWriteOK) before sending data commands.
     * The demo app waits for ReadWriteOK, not just BLE connected (state 7).
     */
    private fun registerConnectionListener() {
        YCBTClient.registerBleStateChange(object : BleConnectResponse {
            override fun onConnectResponse(code: Int) {
                // ENHANCED LOGGING: Show hex and decimal for all states
                Log.i(TAG, "═══════════════════════════════════")
                Log.i(TAG, "BLE STATE CHANGE: $code (0x${code.toString(16).uppercase()})")
                Log.i(TAG, "═══════════════════════════════════")
                
                when (code) {
                    // State 10 (0x0a) = ReadWriteOK - THIS IS THE TARGET STATE!
                    Constants.BLEState.ReadWriteOK, 10, 0x0a -> {
                        cancelConnectionTimeout()
                        isConnecting = false
                        _connectionState.value = ConnectionState.Connected
                        Log.i(TAG, "🎉🎉🎉 STATE 10 REACHED - ReadWriteOK! 🎉🎉🎉")
                        Log.i(TAG, "SDK is now ready for commands!")
                        
                        // Connect native GATT for direct battery reading
                        if (connectedMacAddress.isNotEmpty()) {
                            connectNativeGatt(connectedMacAddress)
                        }
                        
                        // NOW send commands - SDK is fully ready
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!dataRetrievalStarted && _connectionState.value == ConnectionState.Connected) {
                                onConnectionSuccess(connectedMacAddress, connectedDeviceName)
                            }
                        }, 200)  // Short delay, SDK is ready
                    }
                    
                    // State 9 (0x09) = CharacteristicNotification enabled
                    9, 0x09 -> {
                        Log.i(TAG, "📡 STATE 9: Notifications enabled - waiting for state 10...")
                        _connectionState.value = ConnectionState.Connected
                    }
                    
                    // State 8 (0x08) = CharacteristicDiscovered
                    8, 0x08 -> {
                        Log.i(TAG, "🔍 STATE 8: Characteristics discovered - waiting for state 9...")
                        _connectionState.value = ConnectionState.Connected
                    }
                    
                    // State 7 (0x07) = ServicesDiscovered
                    7, 0x07 -> {
                        Log.i(TAG, "📋 STATE 7: Services discovered - initiating SDK handshake...")
                        _connectionState.value = ConnectionState.Connected
                        cancelConnectionTimeout()
                        isConnecting = false
                        
                        // CRITICAL: Send SDK handshake IMMEDIATELY to unlock ring
                        // The demo app sends these commands right after connection
                        // This may help the ring progress to state 8, 9, 10
                        Handler(Looper.getMainLooper()).post {
                            sendSdkHandshake()
                        }
                        
                        // Connect native GATT for direct data reading (backup)
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (connectedMacAddress.isNotEmpty()) {
                                Log.i(TAG, "🔌 Connecting native GATT for direct data access...")
                                connectNativeGatt(connectedMacAddress)
                            }
                        }, 1000)
                        
                        // FALLBACK: If state 10 doesn't arrive in 5 seconds, proceed anyway
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!dataRetrievalStarted && _connectionState.value == ConnectionState.Connected) {
                                Log.w(TAG, "⚠️ State 10 timeout - proceeding with data retrieval at state 7...")
                                onConnectionSuccess(connectedMacAddress, connectedDeviceName)
                            }
                        }, 5000)  // Wait 5 seconds for state 10
                    }
                    
                    // State 6 (0x06) = Connected
                    6, 0x06 -> {
                        Log.i(TAG, "🔗 STATE 6: BLE connected - waiting for services...")
                        _connectionState.value = ConnectionState.Connected
                    }
                    
                    // State 5 (0x05) = Connecting
                    5, 0x05 -> {
                        Log.i(TAG, "⏳ STATE 5: Connecting...")
                    }
                    
                     // State 4 (0x04) = Disconnecting
                    4, 0x04 -> {
                        Log.i(TAG, "🔌 STATE 4: Disconnecting...")
                        dataRetrievalStarted = false
                    }
                    
                    // State 3 (0x03) = Disconnect
                    Constants.BLEState.Disconnect, 3, 0x03 -> {
                        Log.i(TAG, "❌ STATE 3: Disconnected")
                        _connectionState.value = ConnectionState.Disconnected
                        dataRetrievalStarted = false
                    }
                    
                    // State 2 (0x02) = NotOpen (Bluetooth off?)
                    2, 0x02 -> {
                        Log.w(TAG, "⚠️ STATE 2: NotOpen - is Bluetooth enabled?")
                        _connectionState.value = ConnectionState.Disconnected
                        dataRetrievalStarted = false
                    }
                    
                    // State 1 (0x01) = Timeout
                    1, 0x01 -> {
                        Log.e(TAG, "⏰ STATE 1: Connection timeout!")
                        _connectionState.value = ConnectionState.Timeout
                        dataRetrievalStarted = false
                    }
                    
                    else -> {
                        Log.w(TAG, "❓ Unknown state: $code (0x${code.toString(16)})")
                    }
                }
            }
        })
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SDK HANDSHAKE - Send initialization commands to unlock ring
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Send SDK handshake commands to help ring progress from state 7 to state 10
     * The demo app sends these commands immediately after connection.
     * This may trigger the SDK to complete characteristic discovery and notification setup.
     */
    private fun sendSdkHandshake() {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🤝 SENDING SDK HANDSHAKE")
        Log.i(TAG, "═══════════════════════════════════")
        
        try {
            // STEP 1: Set language (English = 0x00)
            // Demo app does this FIRST after connection
            Log.i(TAG, "🤝 Step 1: Setting language (English)...")
            YCBTClient.settingLanguage(0x00, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.i(TAG, "🤝 settingLanguage response: code=$code")
                    if (code == 0) {
                        Log.i(TAG, "✓ Language set successfully")
                    }
                }
            })
            
            // STEP 2: Send phone time sync (after 300ms delay)
            Handler(Looper.getMainLooper()).postDelayed({
                Log.i(TAG, "🤝 Step 2: Syncing phone time...")
                try {
                    val calendar = java.util.Calendar.getInstance()
                    val year = calendar.get(java.util.Calendar.YEAR)
                    val month = calendar.get(java.util.Calendar.MONTH) + 1
                    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(java.util.Calendar.MINUTE)
                    val second = calendar.get(java.util.Calendar.SECOND)
                    
                    // Try different time sync methods
                    try {
                        // Method 1: settingTime (if exists)
                        val method = YCBTClient::class.java.getMethod(
                            "settingTime",
                            Int::class.java, Int::class.java, Int::class.java,
                            Int::class.java, Int::class.java, Int::class.java,
                            BleDataResponse::class.java
                        )
                        method.invoke(null, year, month, day, hour, minute, second, object : BleDataResponse {
                            override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                                Log.i(TAG, "🤝 settingTime response: code=$code")
                            }
                        })
                        Log.i(TAG, "✓ Time sync command sent")
                    } catch (e: NoSuchMethodException) {
                        Log.w(TAG, "settingTime method not found, skipping...")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Time sync failed: ${e.message}")
                }
            }, 300)
            
            // STEP 3: Send phone model (after 600ms delay)
            Handler(Looper.getMainLooper()).postDelayed({
                Log.i(TAG, "🤝 Step 3: Sending phone model...")
                YCBTClient.appMobileModel(android.os.Build.MODEL, object : BleDataResponse {
                    override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                        Log.i(TAG, "🤝 appMobileModel response: code=$code")
                        if (code == 0) {
                            Log.i(TAG, "✓ Phone model sent successfully")
                        }
                    }
                })
            }, 600)
            
            // STEP 4: Request device info (after 1000ms delay)
            // This may trigger the ring to complete state progression
            Handler(Looper.getMainLooper()).postDelayed({
                Log.i(TAG, "🤝 Step 4: Requesting device info...")
                YCBTClient.getDeviceInfo(object : BleDataResponse {
                    override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                        Log.i(TAG, "🤝 getDeviceInfo response: code=$code")
                        if (code == 0 && resultMap != null) {
                            Log.i(TAG, "✓✓✓ DEVICE INFO RECEIVED! ✓✓✓")
                            Log.i(TAG, "Response: $resultMap")
                            parseAndUpdateDeviceInfo(resultMap)
                        }
                    }
                })
            }, 1000)
            
            // STEP 5: Enable heart rate monitoring (after 1500ms delay)
            Handler(Looper.getMainLooper()).postDelayed({
                Log.i(TAG, "🤝 Step 5: Enabling heart rate monitoring...")
                try {
                    YCBTClient.settingHeartMonitor(0x01, 10, object : BleDataResponse {
                        override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                            Log.i(TAG, "🤝 settingHeartMonitor response: code=$code")
                        }
                    })
                } catch (e: Exception) {
                    Log.w(TAG, "settingHeartMonitor failed: ${e.message}")
                }
            }, 1500)
            
            Log.i(TAG, "🤝 Handshake commands queued successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ SDK Handshake error: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RING DATA RETRIEVAL - Called after successful connection
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called when connection is fully established
     * Shows toast and starts reading data from ring
     * 
     * FULL DEMO PATTERN: Chain callbacks sequentially
     * appMobileModel -> settingLanguage -> getDeviceInfo -> other commands
     */
    private fun onConnectionSuccess(macAddress: String, deviceName: String?) {
        // Prevent duplicate calls
        if (dataRetrievalStarted) {
            Log.d(TAG, "Data retrieval already started, skipping duplicate call")
            return
        }
        dataRetrievalStarted = true
        
        // Reset step flags for fresh execution (in case of reconnect)
        step2Started = false
        step2bStarted = false
        step3Started = false
        step4Started = false
        
        connectedMacAddress = macAddress
        connectedDeviceName = deviceName ?: "Ring"
        
        // Update ring data with device info
        _ringData.value = _ringData.value.copy(
            deviceName = connectedDeviceName,
            macAddress = macAddress,
            lastUpdate = System.currentTimeMillis()
        )
        
        // Show toast on main thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Ring Connected Successfully!",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "STARTING DATA RETRIEVAL (Demo Pattern)")
        Log.i(TAG, "═══════════════════════════════════")
        
        // STEP 1: Send phone model to ring (demo app does this!)
        // This might be required for the ring to "unlock" and respond
        Log.i(TAG, "Step 1: Sending phone model...")
        YCBTClient.appMobileModel(android.os.Build.MODEL, object : BleDataResponse {
            override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                Log.d(TAG, "appMobileModel callback: code=$code")
                if (code == 0) {
                    Log.i(TAG, "✓ Phone model sent successfully")
                    // STEP 2: Set language (chain from callback)
                    step2SetLanguage()
                } else {
                    Log.w(TAG, "appMobileModel failed, trying step 2 anyway...")
                    step2SetLanguage()
                }
            }
        })
        
        // Fallback: If callback doesn't fire in 2 seconds, proceed anyway
        Handler(Looper.getMainLooper()).postDelayed({
            step2SetLanguage()
        }, 2000)
    }
    
    private var step2Started = false
    private fun step2SetLanguage() {
        if (step2Started) return
        step2Started = true
        
        Log.i(TAG, "Step 2: Setting language (English)...")
        YCBTClient.settingLanguage(0x00, object : BleDataResponse {
            override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                Log.d(TAG, "settingLanguage callback: code=$code")
                if (code == 0) {
                    Log.i(TAG, "✓ Language set successfully")
                    // STEP 2b: Set phone time (Demo App does this!)
                    step2b_setPhoneTime()
                } else {
                    Log.w(TAG, "settingLanguage failed, trying step 2b anyway...")
                    step2b_setPhoneTime()
                }
            }
        })
        
        // Fallback: If callback doesn't fire in 2 seconds, proceed anyway
        Handler(Looper.getMainLooper()).postDelayed({
            step2b_setPhoneTime()
        }, 2000)
    }
    
    // NEW: Step 2b - Set phone time (Demo App Pattern)
    // NOTE: appSendTimeToDev method may not exist in this SDK version
    // Trying settingTime or skipping if method not found
    private var step2bStarted = false
    private fun step2b_setPhoneTime() {
        if (step2bStarted) return
        step2bStarted = true
        
        Log.i(TAG, "Step 2b: Syncing phone time...")
        try {
            // Get current time components
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1  // Calendar months are 0-indexed
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = calendar.get(java.util.Calendar.MINUTE)
            val second = calendar.get(java.util.Calendar.SECOND)
            
            Log.d(TAG, "Setting time: $year-$month-$day $hour:$minute:$second")
            
            // Try to call settingTime via reflection since exact method name is unknown
            // If method doesn't exist, skip to step 3
            try {
                val method = YCBTClient::class.java.getMethod(
                    "settingTime",
                    Int::class.java, Int::class.java, Int::class.java,
                    Int::class.java, Int::class.java, Int::class.java,
                    BleDataResponse::class.java
                )
                method.invoke(null, year, month, day, hour, minute, second, object : BleDataResponse {
                    override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                        Log.d(TAG, "settingTime callback: code=$code")
                        if (code == 0) {
                            Log.i(TAG, "✓ Phone time synced successfully")
                        } else {
                            Log.w(TAG, "settingTime failed with code=$code")
                        }
                        step3GetDeviceInfo()
                    }
                })
            } catch (noSuchMethodException: NoSuchMethodException) {
                Log.w(TAG, "settingTime method not found in SDK, skipping time sync...")
                step3GetDeviceInfo()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting phone time: ${e.message}, skipping...")
            step3GetDeviceInfo()
        }
        
        // Fallback: If callback doesn't fire in 2 seconds, proceed anyway
        Handler(Looper.getMainLooper()).postDelayed({
            step3GetDeviceInfo()
        }, 2000)
    }
    
    private var step3Started = false
    private fun step3GetDeviceInfo() {
        if (step3Started) return
        step3Started = true
        
        Log.i(TAG, "Step 3: Getting device info...")
        YCBTClient.getDeviceInfo(object : BleDataResponse {
            override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                Log.d(TAG, "getDeviceInfo callback: code=$code")
                if (code == 0 && resultMap != null) {
                    Log.i(TAG, "✓✓✓ GOT DEVICE INFO! ✓✓✓")
                    Log.i(TAG, "Response: $resultMap")
                    parseAndUpdateDeviceInfo(resultMap)
                    // STEP 4: Enable real-time data
                    step4EnableRealTimeData()
                } else {
                    Log.w(TAG, "getDeviceInfo failed with code=$code")
                    step4EnableRealTimeData()
                }
            }
        })
        
        // Fallback: If callback doesn't fire in 3 seconds, proceed anyway
        Handler(Looper.getMainLooper()).postDelayed({
            step4EnableRealTimeData()
        }, 3000)
    }
    
    private var step4Started = false
    private fun step4EnableRealTimeData() {
        if (step4Started) return
        step4Started = true
        
        Log.i(TAG, "Step 4: Enabling real-time data...")
        
        // Try to get all real-time data at once (demo app uses this)
        Log.i(TAG, "Calling getAllRealDataFromDevice...")
        try {
            YCBTClient.getAllRealDataFromDevice(object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.i(TAG, "getAllRealDataFromDevice callback: code=$code")
                    if (code == 0 && resultMap != null) {
                        Log.i(TAG, "✓✓✓ GOT ALL REAL DATA! ✓✓✓")
                        Log.i(TAG, "Response: $resultMap")
                        parseAndUpdateDeviceInfo(resultMap)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error calling getAllRealDataFromDevice: ${e.message}")
        }
        
        enableRealTimeData()
        registerRealTimeCallback()
        readSleepHistory()
        
        // Reset step flags for next connection cycle
        Handler(Looper.getMainLooper()).postDelayed({
            step2Started = false
            step3Started = false
            step4Started = false
        }, 5000)
    }

    /**
     * Read device info (battery level, firmware version)
     * 
     * WORKAROUND: The SDK's async callback never fires because of internal auto-reconnect.
     * Solution: Call getDeviceInfo() to trigger caching, wait, then use readDeviceInfo() 
     * to read the cached data synchronously.
     */
    private fun readDeviceInfo() {
        Log.d(TAG, "Reading device info...")
        
        try {
            // Step 1: Trigger the SDK to fetch device info and cache it
            YCBTClient.getDeviceInfo(object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    // This callback rarely fires due to SDK auto-reconnect
                    // But we try to use it if it does
                    Log.d(TAG, "getDeviceInfo callback: code=$code")
                    if (code == 0 && resultMap != null) {
                        Log.d(TAG, "Async response received: $resultMap")
                        parseAndUpdateDeviceInfo(resultMap)
                    }
                }
            })
            
            // Step 2: Wait for SDK to cache the data, then read synchronously
            Handler(Looper.getMainLooper()).postDelayed({
                readCachedDeviceInfo()
            }, 1500) // Wait 1.5 seconds (reduced from 3s - we only have ~12s before auto-reconnect)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading device info: ${e.message}", e)
        }
    }
    
    /**
     * Read cached device info using synchronous SDK method
     * This bypasses the async callback issue
     */
    private fun readCachedDeviceInfo() {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "READING CACHED DEVICE INFO")
        Log.i(TAG, "═══════════════════════════════════")
        
        try {
            // Try to read device type (the only documented constant)
            val deviceType = YCBTClient.readDeviceInfo(Constants.FunctionConstant.DEVICETYPE)
            Log.d(TAG, "Device Type: $deviceType (0x00=Watch/Bracelet, 0x01=Ring)")
            
            // The battery value is NOT available via readDeviceInfo
            // It only comes via the async callback from getDeviceInfo
            // Since callback rarely fires, we'll keep trying via polling
            Log.d(TAG, "Battery must come from async callback - starting polling...")
            pollForBatteryData()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached device info: ${e.message}", e)
        }
    }
    
    /**
     * Poll for battery data by repeatedly calling getDeviceInfo
     */
    private var pollAttempt = 0
    private fun pollForBatteryData() {
        if (pollAttempt >= 5) {
            Log.w(TAG, "Gave up polling for battery after 5 attempts")
            pollAttempt = 0
            return
        }
        
        pollAttempt++
        Log.d(TAG, "Polling for battery data (attempt $pollAttempt/5)...")
        
        try {
            YCBTClient.getDeviceInfo(object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.d(TAG, "Poll attempt $pollAttempt callback: code=$code")
                    if (code == 0 && resultMap != null) {
                        Log.i(TAG, "✓ Got device info response!")
                        parseAndUpdateDeviceInfo(resultMap)
                        pollAttempt = 0 // Reset on success
                    } else {
                        // Schedule next poll attempt
                        scheduleNextPoll()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Poll error: ${e.message}")
            scheduleNextPoll()
        }
        
        // Also schedule next attempt in case callback doesn't fire
        scheduleNextPoll()
    }
    
    private fun scheduleNextPoll() {
        if (pollAttempt < 5 && (_ringData.value.battery == null || _ringData.value.battery == 0)) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (_ringData.value.battery == null || _ringData.value.battery == 0) { // Only poll if still no battery
                    pollForBatteryData()
                }
            }, 2000) // Wait 2 seconds between polls (reduced from 5s to fit in ~12s window)
        }
    }
    
    /**
     * Parse device info from async response (if callback fires)
     * Uses Gson exactly like SDK documentation specifies:
     * val info = Gson().fromJson(resultMap.toString(), BandBaseInfo::class.java)
     */
    private fun parseAndUpdateDeviceInfo(resultMap: HashMap<*, *>) {
        try {
            // SDK DOCUMENTATION PATTERN:
            // String backVal = resultMap.toString();
            // Gson gson = new Gson();
            // BandBaseInfo bandBaseInfo = gson.fromJson(backVal, BandBaseInfo.class);
            
            val backVal = resultMap.toString()
            Log.d(TAG, "Raw resultMap string: $backVal")
            
            val gson = com.google.gson.Gson()
            val info = gson.fromJson(backVal, com.fitness.app.ble.models.BandBaseInfo::class.java)
            
            Log.d(TAG, "Parsed BandBaseInfo: $info")
            Log.d(TAG, "Battery value string: ${info?.data?.deviceBatteryValue}")
            Log.d(TAG, "Device version: ${info?.data?.deviceVersion}")
            
            val battery = info?.getBatteryPercentage() ?: 0
            
            if (battery > 0) {
                Log.i(TAG, "✓✓✓ BATTERY FROM SDK: $battery% ✓✓✓")
                _ringData.value = _ringData.value.copy(
                    battery = battery,
                    lastUpdate = System.currentTimeMillis()
                )
            } else {
                Log.w(TAG, "Battery value is 0 or null from parsed info")
                // Also try direct extraction as fallback
                val directBattery = resultMap["deviceBatteryValue"]?.toString()?.toIntOrNull()
                if (directBattery != null && directBattery > 0) {
                    Log.i(TAG, "✓ Battery from direct extraction: $directBattery%")
                    _ringData.value = _ringData.value.copy(
                        battery = directBattery,
                        lastUpdate = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse device info: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Enable real-time data streaming from ring
     * mode: 0x00=Steps, 0x01=HeartRate, 0x07=Comprehensive
     */
    private fun enableRealTimeData() {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "ENABLING REAL-TIME DATA")
        Log.i(TAG, "═══════════════════════════════════")
        
        try {
            // Try mode 0x00 for steps first (most reliable)
            Log.d(TAG, "Enabling steps data (mode=0x00)...")
            YCBTClient.appRealDataFromDevice(1, 0x00, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.d(TAG, "Steps mode enabled: code=$code, result=$resultMap")
                }
            })
            
            // Also try mode 0x01 for heart rate
            Log.d(TAG, "Enabling heart rate data (mode=0x01)...")
            YCBTClient.appRealDataFromDevice(1, 0x01, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.d(TAG, "Heart rate mode enabled: code=$code, result=$resultMap")
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling real-time data: ${e.message}", e)
        }
        
        // Also read historical sport data for steps
        readSportHistory()
    }
    
    /**
     * Read historical sport data (steps, distance, calories)
     */
    private fun readSportHistory() {
        Log.d(TAG, "Reading sport history data...")
        try {
            YCBTClient.healthHistoryData(Constants.DATATYPE.Health_HistorySport, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.i(TAG, "═══════════════════════════════════")
                    Log.i(TAG, "SPORT HISTORY RESPONSE")
                    Log.i(TAG, "═══════════════════════════════════")
                    Log.d(TAG, "Response code: $code")
                    Log.d(TAG, "Response data: $resultMap")
                    Log.d(TAG, "Response keys: ${resultMap?.keys}")
                    
                    if (code == 0 && resultMap != null) {
                        // The SDK returns data in a nested list structure
                        val dataList = resultMap["data"]
                        Log.d(TAG, "Data list: $dataList (type: ${dataList?.javaClass?.name})")
                        
                        var totalSteps = 0
                        var totalDistance = 0
                        var totalCalories = 0
                        
                        // Parse the data - it's usually a list of sport records
                        if (dataList is List<*>) {
                            for (record in dataList) {
                                Log.d(TAG, "Sport record: $record")
                                if (record is Map<*, *>) {
                                    val steps = (record["sportStep"] as? Number)?.toInt() ?: 0
                                    val dist = (record["sportDistance"] as? Number)?.toInt() ?: 0
                                    val cal = (record["sportCalorie"] as? Number)?.toInt() ?: 0
                                    totalSteps += steps
                                    totalDistance += dist
                                    totalCalories += cal
                                }
                            }
                        }
                        
                        // Also try direct access
                        if (totalSteps == 0) {
                            totalSteps = (resultMap["sportStep"] as? Number)?.toInt() ?: 0
                            totalDistance = (resultMap["sportDistance"] as? Number)?.toInt() ?: 0
                            totalCalories = (resultMap["sportCalorie"] as? Number)?.toInt() ?: 0
                        }
                        
                        Log.i(TAG, "✓ Sport data: Steps=$totalSteps, Dist=$totalDistance, Cal=$totalCalories")
                        
                        if (totalSteps > 0 || totalDistance > 0 || totalCalories > 0) {
                            _ringData.value = _ringData.value.copy(
                                steps = totalSteps,
                                distance = totalDistance,
                                calories = totalCalories,
                                lastUpdate = System.currentTimeMillis()
                            )
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sport history: ${e.message}", e)
        }
    }

    /**
     * Register callback for real-time data updates
     */
    private fun registerRealTimeCallback() {
        Log.d(TAG, "Registering real-time data callback...")
        try {
            YCBTClient.appRegisterRealDataCallBack(object : BleRealDataResponse {
                override fun onRealDataResponse(dataType: Int, dataMap: HashMap<*, *>?) {
                    if (dataMap == null) return
                    
                    Log.d(TAG, "Real-time data received: type=$dataType, data=$dataMap")
                    
                    when (dataType) {
                        Constants.DATATYPE.Real_UploadHeart -> {
                            val heartRate = (dataMap["heartValue"] as? Number)?.toInt() ?: 0
                            if (heartRate > 0) {
                                Log.i(TAG, "❤️  Heart rate: $heartRate BPM")
                                _ringData.value = _ringData.value.copy(
                                    heartRate = heartRate,
                                    lastUpdate = System.currentTimeMillis()
                                )
                            }
                        }
                        Constants.DATATYPE.Real_UploadSport -> {
                            val steps = (dataMap["sportStep"] as? Number)?.toInt() ?: _ringData.value.steps
                            val distance = (dataMap["sportDistance"] as? Number)?.toInt() ?: _ringData.value.distance
                            val calories = (dataMap["sportCalorie"] as? Number)?.toInt() ?: _ringData.value.calories
                            
                            Log.i(TAG, "👣 Steps: $steps, Distance: ${distance}m, Calories: ${calories}kcal")
                            _ringData.value = _ringData.value.copy(
                                steps = steps,
                                distance = distance,
                                calories = calories,
                                lastUpdate = System.currentTimeMillis()
                            )
                        }
                        Constants.DATATYPE.Real_UploadBloodOxygen -> {
                            val spo2 = (dataMap["bloodOxygenValue"] as? Number)?.toInt() ?: 0
                            if (spo2 > 0) {
                                Log.i(TAG, "🩸 SpO2: $spo2%")
                                _ringData.value = _ringData.value.copy(
                                    spO2 = spo2,
                                    lastUpdate = System.currentTimeMillis()
                                )
                            }
                        }
                        else -> {
                            Log.d(TAG, "Other data type: $dataType")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error registering real-time callback: ${e.message}", e)
        }
    }

    /**
     * Read historical sleep data from ring
     */
    private fun readSleepHistory() {
        Log.d(TAG, "Reading sleep history...")
        try {
            YCBTClient.healthHistoryData(Constants.DATATYPE.Health_HistorySleep, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.d(TAG, "Sleep history response: code=$code, data=$resultMap")
                    if (code == 0 && resultMap != null) {
                        val deepSleep = (resultMap["deepSleepTotal"] as? Number)?.toInt() ?: 0
                        val lightSleep = (resultMap["lightSleepTotal"] as? Number)?.toInt() ?: 0
                        
                        Log.i(TAG, "😴 Sleep: Deep=${deepSleep}min, Light=${lightSleep}min")
                        _ringData.value = _ringData.value.copy(
                            deepSleep = deepSleep,
                            lightSleep = lightSleep,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sleep history: ${e.message}", e)
        }
    }
}
