package com.fitness.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Pure Native GATT BLE Manager - NO SDK DEPENDENCY
 * 
 * This implementation uses only Android's native BLE APIs, completely bypassing
 * the YCBT SDK which doesn't work properly with the R9 ring.
 * 
 * ## Key Features
 * - Pure native Android BLE (no SDK)
 * - Enables notifications for real-time data
 * - No conflicting GATT connections
 * - Should stay connected indefinitely
 * 
 * @author DKGS Labs
 * @version 2.0.0 - Pure Native GATT
 */
class NativeGattManager private constructor(private val context: Context) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _ringData = MutableStateFlow(RingData())
    val ringData: StateFlow<RingData> = _ringData.asStateFlow()
    
    internal var connectedMacAddress: String = ""
    internal var connectedDeviceName: String = ""
    
    private val discoveredDevices = mutableMapOf<String, BleDevice>()
    private var isInitialized = false
    
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var nativeScanCallback: ScanCallback? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var isConnecting = false
    private var connectionRetryCount = 0
    private val MAX_RETRIES = 3
    
    // Keep-alive mechanism to prevent disconnect
    private val keepAliveHandler = Handler(Looper.getMainLooper())
    private val KEEP_ALIVE_INTERVAL = 5000L  // 5 seconds
    private var keepAliveRunnable: Runnable? = null
    
    companion object {
        private const val TAG = "NativeGattManager"
        private const val DEFAULT_SCAN_DURATION = 8
        
        // Standard BLE UUIDs
        private val BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")
        
        // Device Info Service
        private val DEVICE_INFO_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
        
        // Custom Ring Services (YC-specific)
        private val CUSTOM_SERVICE_EFE0 = UUID.fromString("f000efe0-0451-4000-0000-00000000b000")
        private val CUSTOM_CHAR_EFE1 = UUID.fromString("f000efe1-0451-4000-0000-00000000b000")  // Write
        private val CUSTOM_CHAR_EFE3 = UUID.fromString("f000efe3-0451-4000-0000-00000000b000")  // Read/Notify
        
        // FEE7 Service (handshake/battery)
        private val SERVICE_FEE7 = UUID.fromString("0000FEE7-0000-1000-8000-00805f9b34fb")
        private val CHAR_FEA1 = UUID.fromString("0000FEA1-0000-1000-8000-00805f9b34fb")  // Notify - likely battery/health
        private val CHAR_FEA2 = UUID.fromString("0000FEA2-0000-1000-8000-00805f9b34fb")  // Indicate
        private val CHAR_FEC7 = UUID.fromString("0000FEC7-0000-1000-8000-00805f9b34fb")  // Write
        private val CHAR_FEC8 = UUID.fromString("0000FEC8-0000-1000-8000-00805f9b34fb")  // Indicate
        
        // Client Characteristic Configuration Descriptor (for enabling notifications)
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        @Volatile
        private var instance: NativeGattManager? = null

        fun getInstance(context: Context): NativeGattManager {
            return instance ?: synchronized(this) {
                instance ?: NativeGattManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Initialize - Just check Bluetooth is available
     * NO SDK initialization!
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bluetoothManager?.adapter == null) {
            Log.e(TAG, "Bluetooth not available")
            return
        }

        isInitialized = true
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ PURE NATIVE GATT MANAGER INITIALIZED")
        Log.i(TAG, "✓ NO SDK - 100% Native Android BLE")
        Log.i(TAG, "═══════════════════════════════════")
    }

    /**
     * Start BLE scan using native Android scanner
     */
    @SuppressLint("MissingPermission")
    fun startScan(scanDuration: Int = DEFAULT_SCAN_DURATION) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized")
            _scanState.value = ScanState.Error("Not initialized")
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothLeScanner = bluetoothManager?.adapter?.bluetoothLeScanner

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner not available")
            _scanState.value = ScanState.Error("Bluetooth not available")
            return
        }

        discoveredDevices.clear()
        _scanState.value = ScanState.Scanning

        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔍 STARTING NATIVE BLE SCAN")
        Log.i(TAG, "Duration: ${scanDuration}s")
        Log.i(TAG, "═══════════════════════════════════")

        nativeScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                processScanResult(result)
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { processScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed: $errorCode")
                _scanState.value = ScanState.Error("Scan failed: $errorCode")
            }
        }

        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            bluetoothLeScanner?.startScan(emptyList(), settings, nativeScanCallback)
            Log.i(TAG, "✓ Scan started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan: ${e.message}")
            _scanState.value = ScanState.Error("Failed to start scan")
            return
        }

        // Stop scan after duration
        Handler(Looper.getMainLooper()).postDelayed({
            stopScan()
            val devices = discoveredDevices.values.toList()
            Log.i(TAG, "Scan complete. Found ${devices.size} devices")
            _scanState.value = if (devices.isEmpty()) {
                ScanState.Error("No devices found")
            } else {
                ScanState.DevicesFound(devices)
            }
        }, scanDuration * 1000L)
    }

    @SuppressLint("MissingPermission")
    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val macAddress = device.address
        val deviceName = result.scanRecord?.deviceName ?: device.name ?: "Unknown"
        val rssi = result.rssi

        if (discoveredDevices.containsKey(macAddress)) {
            discoveredDevices[macAddress] = BleDevice(deviceName, macAddress, rssi)
            return
        }

        Log.d(TAG, "📱 Found: $deviceName ($macAddress) RSSI: $rssi")
        discoveredDevices[macAddress] = BleDevice(deviceName, macAddress, rssi)
        _scanState.value = ScanState.DevicesFound(discoveredDevices.values.toList())
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        nativeScanCallback?.let {
            try {
                bluetoothLeScanner?.stopScan(it)
                Log.d(TAG, "Scan stopped")
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping scan: ${e.message}")
            }
        }
        nativeScanCallback = null
    }

    /**
     * Connect to device using PURE NATIVE GATT
     * No SDK involvement at all!
     */
    @SuppressLint("MissingPermission")
    fun connectDevice(macAddress: String, deviceName: String? = null) {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔌 CONNECTING VIA PURE NATIVE GATT")
        Log.i(TAG, "MAC: $macAddress")
        Log.i(TAG, "═══════════════════════════════════")

        if (!isInitialized) {
            Log.e(TAG, "Not initialized")
            _connectionState.value = ConnectionState.Error("Not initialized")
            return
        }

        if (isConnecting) {
            Log.w(TAG, "Already connecting")
            return
        }

        stopScan()
        isConnecting = true
        connectedMacAddress = macAddress
        connectedDeviceName = deviceName ?: "Ring"
        _connectionState.value = ConnectionState.Connecting
        _ringData.value = RingData() // Reset

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter not available")
            _connectionState.value = ConnectionState.Error("Bluetooth not available")
            isConnecting = false
            return
        }

        try {
            val device = bluetoothAdapter.getRemoteDevice(macAddress)
            
            // Close existing connection and wait for BLE stack to reset
            bluetoothGatt?.let { gatt ->
                gatt.disconnect()
                gatt.close()
                Thread.sleep(500)  // Give BLE stack time to clean up
            }
            bluetoothGatt = null

            // Connect using native GATT with autoConnect=true for persistent connection
            // autoConnect=true helps maintain connection and handles reconnection better
            bluetoothGatt = device.connectGatt(
                context,
                true,  // autoConnect = true for more stable persistent connection
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )

            Log.i(TAG, "✓ Native GATT connection initiated...")

        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}")
            _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
            isConnecting = false
        }
    }

    /**
     * Native GATT Callback - Handles all BLE events
     */
    private val gattCallback = object : BluetoothGattCallback() {
        
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "CONNECTION STATE: status=$status, newState=$newState")
            Log.i(TAG, "═══════════════════════════════════")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✓✓✓ CONNECTED! Discovering services...")
                    isConnecting = false
                    connectionRetryCount = 0  // Reset retry counter on success
                    _connectionState.value = ConnectionState.Connected
                    
                    // Request HIGH PRIORITY connection to prevent timeout/disconnect
                    gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    Log.i(TAG, "📶 Requested HIGH PRIORITY connection")
                    
                    // Update ring data with device info
                    _ringData.value = _ringData.value.copy(
                        deviceName = connectedDeviceName,
                        macAddress = connectedMacAddress,
                        lastUpdate = System.currentTimeMillis()
                    )
                    
                    // Show toast
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Ring Connected!", Toast.LENGTH_SHORT).show()
                    }
                    
                    // Start keep-alive mechanism
                    startKeepAlive()
                    
                    // Discover services
                    gatt?.discoverServices()
                }
                
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "✗ DISCONNECTED (status=$status)")
                    isConnecting = false
                    
                    // Stop keep-alive when disconnected
                    stopKeepAlive()
                    
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    
                    // Handle retryable GATT errors:
                    // - status 8 = GATT_CONN_TIMEOUT (connection timeout)
                    // - status 133 = GATT_ERROR (generic error)
                    val isRetryableError = status == 8 || status == 133
                    
                    if (isRetryableError && connectionRetryCount < MAX_RETRIES) {
                        connectionRetryCount++
                        Log.w(TAG, "⚠️ GATT error (status=$status) - retry $connectionRetryCount/$MAX_RETRIES in 2 seconds...")
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (connectedMacAddress.isNotEmpty()) {
                                Log.i(TAG, "🔄 Retrying connection to $connectedMacAddress...")
                                connectDevice(connectedMacAddress, connectedDeviceName)
                            }
                        }, 2000L * connectionRetryCount)  // Increasing delay
                    } else {
                        // Give up or normal disconnect
                        connectionRetryCount = 0
                        _connectionState.value = ConnectionState.Disconnected
                        
                        if (isRetryableError) {
                            Log.e(TAG, "❌ Connection failed after $MAX_RETRIES retries. Try toggling Bluetooth.")
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "Connection failed. Toggle Bluetooth and retry.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "📋 SERVICES DISCOVERED! Status: $status")
            Log.i(TAG, "═══════════════════════════════════")

            if (status != BluetoothGatt.GATT_SUCCESS || gatt == null) {
                Log.e(TAG, "Service discovery failed")
                return
            }

            // Log all services
            gatt.services.forEach { service ->
                val isCustom = service.uuid.toString().contains("efe0", true) || 
                               service.uuid.toString().contains("fee7", true)
                Log.d(TAG, "Service: ${service.uuid}${if(isCustom) " ★CUSTOM★" else ""}")
                
                service.characteristics.forEach { char ->
                    val props = char.properties
                    val propStr = buildString {
                        if (props and BluetoothGattCharacteristic.PROPERTY_READ != 0) append("READ ")
                        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) append("WRITE ")
                        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) append("NOTIFY ")
                        if (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) append("INDICATE ")
                    }
                    Log.d(TAG, "  Char: ${char.uuid} [$propStr]")
                }
            }

            // Start reading and enabling notifications
            Handler(Looper.getMainLooper()).post {
                startDataRetrieval(gatt)
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                handleCharacteristicData(characteristic.uuid, characteristic.value)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicData(characteristic.uuid, value)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic != null) {
                Log.i(TAG, "📨 NOTIFICATION RECEIVED!")
                handleCharacteristicData(characteristic.uuid, characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.i(TAG, "📨 NOTIFICATION RECEIVED!")
            handleCharacteristicData(characteristic.uuid, value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.d(TAG, "Descriptor write: ${descriptor?.uuid}, status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ Notifications enabled for ${descriptor?.characteristic?.uuid}")
            }
        }
    }

    /**
     * Start reading data and enabling notifications
     */
    @SuppressLint("MissingPermission")
    private fun startDataRetrieval(gatt: BluetoothGatt) {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "📊 STARTING DATA RETRIEVAL")
        Log.i(TAG, "═══════════════════════════════════")

        val operationQueue = mutableListOf<() -> Unit>()
        
        // DISABLED: Standard battery service (2A19) returns wrong 100%
        // Real battery comes from EFE3 status packets (byte[8])
        // gatt.getService(BATTERY_SERVICE_UUID)?.getCharacteristic(BATTERY_LEVEL_UUID)?.let { char ->
        //     operationQueue.add { 
        //         Log.d(TAG, "Reading battery...")
        //         gatt.readCharacteristic(char) 
        //     }
        // }
        
        // 2. Enable notifications on FEA1 (likely real battery/health data)
        gatt.getService(SERVICE_FEE7)?.getCharacteristic(CHAR_FEA1)?.let { char ->
            if (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                operationQueue.add {
                    Log.d(TAG, "Enabling FEA1 notifications...")
                    enableNotifications(gatt, char)
                }
            }
        }
        
        // 3. Enable notifications on EFE3 (custom health data)
        gatt.getService(CUSTOM_SERVICE_EFE0)?.getCharacteristic(CUSTOM_CHAR_EFE3)?.let { char ->
            if (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                operationQueue.add {
                    Log.d(TAG, "Enabling EFE3 notifications...")
                    enableNotifications(gatt, char)
                }
            }
        }
        
        // 4. Enable indication on FEA2
        gatt.getService(SERVICE_FEE7)?.getCharacteristic(CHAR_FEA2)?.let { char ->
            if (char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                operationQueue.add {
                    Log.d(TAG, "Enabling FEA2 indications...")
                    enableNotifications(gatt, char)
                }
            }
        }
        
        // 5. Read FEA1 directly
        gatt.getService(SERVICE_FEE7)?.getCharacteristic(CHAR_FEA1)?.let { char ->
            if (char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                operationQueue.add {
                    Log.d(TAG, "Reading FEA1...")
                    gatt.readCharacteristic(char)
                }
            }
        }

        // Execute operations with delays (BLE requires sequential operations)
        operationQueue.forEachIndexed { index, operation ->
            Handler(Looper.getMainLooper()).postDelayed({
                operation()
            }, 500L * (index + 1))
        }

        Log.i(TAG, "✓ Queued ${operationQueue.size} operations")
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            gatt.setCharacteristicNotification(characteristic, true)
            
            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                val value = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }
                
                @Suppress("DEPRECATION")
                descriptor.value = value
                gatt.writeDescriptor(descriptor)
                
                Log.d(TAG, "✓ Notification descriptor written for ${characteristic.uuid}")
            } else {
                Log.w(TAG, "No CCCD descriptor for ${characteristic.uuid}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling notifications: ${e.message}")
        }
    }

    /**
     * Handle incoming characteristic data
     */
    private fun handleCharacteristicData(uuid: UUID, value: ByteArray?) {
        if (value == null || value.isEmpty()) return

        val hexString = value.joinToString("") { "%02X".format(it) }
        val intArray = value.map { it.toInt() and 0xFF }

        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "📦 DATA RECEIVED")
        Log.i(TAG, "UUID: $uuid")
        Log.i(TAG, "Hex: $hexString")
        Log.i(TAG, "Int: $intArray")
        Log.i(TAG, "═══════════════════════════════════")

        val uuidString = uuid.toString().lowercase()

        when {
            // DISABLED: Standard Battery Level (2A19) returns wrong 100%
            // Real battery comes from EFE3 status packets
            uuidString.contains("2a19") -> {
                val battery = value[0].toInt() and 0xFF
                Log.d(TAG, "🔋 IGNORED: 2A19 battery = $battery% (wrong, using EFE3 instead)")
                // Don't update ringData - we use EFE3 for real battery
            }
            
            // FEA1 - Likely contains real battery or health data
            uuidString.contains("fea1") -> {
                Log.i(TAG, "📊 FEA1 DATA RECEIVED!")
                parseFea1Data(value)
            }
            
            // EFE3 - Custom health data
            uuidString.contains("efe3") -> {
                Log.i(TAG, "📊 EFE3 DATA RECEIVED!")
                parseEfe3Data(value)
            }
            
            // FEA2 - Heart Rate and real-time health data
            uuidString.contains("fea2") -> {
                Log.i(TAG, "❤️ FEA2 DATA RECEIVED - POTENTIAL HR!")
                parseFea2Data(value)
            }
            
            // FEC8/FEC9 - Device info
            uuidString.contains("fec8") || uuidString.contains("fec9") -> {
                Log.d(TAG, "Device info: $hexString")
            }
        }

        // Check for potential battery values (look for 72 for debugging)
        value.forEachIndexed { index, byte ->
            val intVal = byte.toInt() and 0xFF
            if (intVal in 60..80) {
                Log.w(TAG, "⚡ POTENTIAL BATTERY at index $index: $intVal%")
            }
        }
    }

    private fun parseFea1Data(value: ByteArray) {
        // FEA1 DATA FORMAT (10 bytes):
        // [0] = 7 (type/flag - constant)
        // [1] = ★ STEPS COUNT ★
        // [2] = 0
        // [3] = 0
        // [4] = 63 (unknown, possibly distance or calories related?)
        // [5-9] = other data (0, 0, 2, 0, 0)
        
        if (value.size < 2) return
        
        val packetType = value[0].toInt() and 0xFF
        
        // Only parse if packet type is 7 (status packet)
        if (packetType == 7) {
            // ★ STEPS at byte[1] ★
            // Note: If steps > 255, it might be 16-bit (bytes[1,2])
            val steps = if (value.size >= 3) {
                // Try 16-bit first (little-endian) for steps > 255
                val steps16bit = (value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8)
                if (steps16bit > 0 && value[2].toInt() != 0) {
                    steps16bit  // Use 16-bit if high byte is non-zero
                } else {
                    value[1].toInt() and 0xFF  // Use single byte
                }
            } else {
                value[1].toInt() and 0xFF
            }
            
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "👟👟� STEPS: $steps 👟👟👟")
            Log.i(TAG, "═══════════════════════════════════")
            
            // Update the ring data with steps
            _ringData.value = _ringData.value.copy(
                steps = steps,
                lastUpdate = System.currentTimeMillis()
            )
            
            // Log other bytes for future analysis
            if (value.size >= 5) {
                val byte4 = value[4].toInt() and 0xFF
                Log.d(TAG, "FEA1: type=$packetType, steps=$steps, byte[4]=$byte4")
            }
        }
    }
    
    /**
     * Parse FEA2 data - Heart Rate / Real-time health data
     */
    private fun parseFea2Data(value: ByteArray) {
        if (value.isEmpty()) return
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "❤️ FEA2 HEART RATE PACKET ANALYSIS")
        Log.i(TAG, "═══════════════════════════════════")
        
        // Log all bytes for analysis
        val allBytes = value.take(20).mapIndexed { i, b -> "[$i]=${b.toInt() and 0xFF}" }.joinToString(", ")
        Log.i(TAG, "All bytes: $allBytes")
        
        // Look for potential HR values (40-200 typical HR range)
        for (i in 0 until minOf(value.size, 15)) {
            val byteVal = value[i].toInt() and 0xFF
            if (byteVal in 40..200) {
                Log.i(TAG, "❤️ POTENTIAL HR: byte[$i] = $byteVal bpm")
            }
        }
        
        // Try common positions for HR data
        // Position 0 or 1 is most common for HR
        val packetType = value[0].toInt() and 0xFF
        val potentialHR1 = if (value.size > 1) value[1].toInt() and 0xFF else 0
        val potentialHR2 = if (value.size > 2) value[2].toInt() and 0xFF else 0
        
        Log.i(TAG, "packet type=${packetType}, byte[1]=$potentialHR1, byte[2]=$potentialHR2")
        
        // If this looks like an HR packet (values in valid HR range 40-200)
        val hrValue = when {
            potentialHR1 in 40..200 -> potentialHR1
            potentialHR2 in 40..200 -> potentialHR2
            packetType in 40..200 -> packetType  // Sometimes HR is at byte[0]
            else -> 0
        }
        
        if (hrValue > 0) {
            Log.i(TAG, "❤️❤️❤️ HEART RATE FOUND: $hrValue bpm ❤️❤️❤️")
            _ringData.value = _ringData.value.copy(
                heartRate = hrValue,
                heartRateMeasuring = false,  // Measurement complete
                lastUpdate = System.currentTimeMillis()
            )
        }
        
        Log.i(TAG, "═══════════════════════════════════")
    }

    private fun parseEfe3Data(value: ByteArray) {
        // EFE3 DATA FORMAT (20 bytes):
        // Multiple packet types:
        // - type 0x0F, subtype 0x06: Status packet with timestamp + battery at byte[8]
        // - type 0x0F, subtype 0x85: Unknown data (contains 85 values)
        // - type 0x0F, subtype 0xF1: May contain HR or other measurements
        // - type 0xF0: Contains data (possibly battery?)
        // - type 0x81: Acknowledgment packet
        
        if (value.size < 9) return
        
        val packetType = value[0].toInt() and 0xFF
        val packetSubType = value[1].toInt() and 0xFF
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "� EFE3 PACKET: type=0x${packetType.toString(16).uppercase()}, subtype=0x${packetSubType.toString(16).uppercase()}")
        Log.i(TAG, "═══════════════════════════════════")
        
        // Print ALL 20 bytes for analysis
        val allBytes = value.take(20).mapIndexed { i, b -> "[$i]=${b.toInt() and 0xFF}" }.joinToString(", ")
        Log.d(TAG, "All bytes: $allBytes")
        
        // Print potential battery values (50-100)
        for (i in 0 until minOf(value.size, 20)) {
            val byteVal = value[i].toInt() and 0xFF
            if (byteVal in 60..100) {
                Log.i(TAG, "🔋 POTENTIAL BATTERY: byte[$i] = $byteVal%")
            }
        }
        
        // Print potential HR values (40-200 range)
        for (i in 0 until minOf(value.size, 20)) {
            val byteVal = value[i].toInt() and 0xFF
            if (byteVal in 40..200) {
                Log.i(TAG, "❤️ POTENTIAL HR: byte[$i] = $byteVal bpm")
            }
        }
        
        when (packetType) {
            0x0F -> {
                when (packetSubType) {
                    0x06 -> {
                        // Status packet format:
                        // byte[8] = battery %
                        // byte[12] = stored/last HR value
                        val battery = value[8].toInt() and 0xFF
                        if (battery in 1..100) {
                            Log.i(TAG, "🔋🔋🔋 BATTERY (type 0x06): $battery% 🔋🔋🔋")
                            _ringData.value = _ringData.value.copy(
                                battery = battery,
                                lastUpdate = System.currentTimeMillis()
                            )
                        }
                        
                        // Check for stress level at byte[2]
                        if (value.size > 2) {
                            val stress = value[2].toInt() and 0xFF
                            if (stress in 0..100) {
                                Log.i(TAG, "😰 STRESS LEVEL (byte[2]): $stress")
                                _ringData.value = _ringData.value.copy(
                                    stress = stress,
                                    lastUpdate = System.currentTimeMillis()
                                )
                            }
                        }
                        
                        // Check for HR at byte[12] (stored/last measured HR)
                        if (value.size > 12) {
                            val storedHR = value[12].toInt() and 0xFF
                            if (storedHR in 40..200) {
                                Log.i(TAG, "❤️ STORED HR in 0x06 packet: byte[12]=$storedHR bpm")
                                
                                // ALWAYS update HR when valid value received
                                val previousHR = _ringData.value.heartRate
                                _ringData.value = _ringData.value.copy(
                                    heartRate = storedHR,
                                    heartRateMeasuring = false,
                                    lastUpdate = System.currentTimeMillis()
                                )
                                
                                // Show Toast only when HR changes significantly (avoid spam)
                                if (previousHR != storedHR) {
                                    Log.i(TAG, "❤️❤️❤️ HEART RATE UPDATED: $storedHR bpm ❤️❤️❤️")
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(context, "❤️ Heart Rate: $storedHR bpm", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                    0x15, 0x16 -> {
                        // ❤️ REAL-TIME HEART RATE PACKET!
                        // Format: [0x0F, 0x15/0x16, HR_value, ...]
                        Log.i(TAG, "❤️❤️❤️ REAL-TIME HR PACKET DETECTED (0x${packetSubType.toString(16)}) ❤️❤️❤️")
                        
                        // HR is typically at byte[2] or byte[3]
                        for (i in 2 until minOf(value.size, 8)) {
                            val hr = value[i].toInt() and 0xFF
                            if (hr in 40..200) {
                                Log.i(TAG, "❤️❤️❤️ HEART RATE: $hr bpm ❤️❤️❤️")
                                _ringData.value = _ringData.value.copy(
                                    heartRate = hr,
                                    heartRateMeasuring = false,
                                    lastUpdate = System.currentTimeMillis()
                                )
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "Heart Rate: $hr bpm", Toast.LENGTH_SHORT).show()
                                }
                                break  // Found valid HR, stop searching
                            }
                        }
                    }
                    0x85 -> {
                        // This packet shows 85 in multiple positions - NOT battery
                        Log.d(TAG, "🔍 Type 0x85 packet (ignored for battery)")
                    }
                    0xF1 -> {
                        // This might be HR or other measurement data
                        // Packet: [15, 241, HR?, ...]
                        Log.i(TAG, "❤️ Type 0xF1 - POTENTIAL HR/MEASUREMENT DATA!")
                        
                        // Look for valid HR value in positions 2-6
                        for (i in 2 until minOf(value.size, 7)) {
                            val hr = value[i].toInt() and 0xFF
                            if (hr in 40..200) {
                                Log.i(TAG, "❤️❤️❤️ HEART RATE FROM 0xF1: $hr bpm ❤️❤️❤️")
                                _ringData.value = _ringData.value.copy(
                                    heartRate = hr,
                                    heartRateMeasuring = false,
                                    lastUpdate = System.currentTimeMillis()
                                )
                                break
                            }
                        }
                    }
                    else -> {
                        // Check if any 0x0F subtype packet contains HR
                        if (_ringData.value.heartRateMeasuring) {
                            for (i in 2 until minOf(value.size, 10)) {
                                val hr = value[i].toInt() and 0xFF
                                if (hr in 40..200) {
                                    Log.i(TAG, "❤️ DETECTED HR in 0x0F/0x${packetSubType.toString(16)}: byte[$i]=$hr bpm")
                                    _ringData.value = _ringData.value.copy(
                                        heartRate = hr,
                                        heartRateMeasuring = false,
                                        lastUpdate = System.currentTimeMillis()
                                    )
                                    break
                                }
                            }
                        }
                        Log.d(TAG, "🔍 Type 0x0F subtype 0x${packetSubType.toString(16).uppercase()} - unknown")
                    }
                }
            }
            0xF0 -> {
                // Type 0xF0 packet - may contain battery at byte[8]
                // Example: [240, 18, 4, 85, 0, 85, 0, 0, 85, ...]
                // BUT: 85 is often a false positive, real battery is at byte[8]
                val battery = value[8].toInt() and 0xFF
                
                // Only update if it's a reasonable value and NOT the common false positive
                if (battery in 1..100 && battery != 85) {
                    Log.i(TAG, "🔋 BATTERY (type 0xF0): $battery%")
                    _ringData.value = _ringData.value.copy(
                        battery = battery,
                        lastUpdate = System.currentTimeMillis()
                    )
                } else {
                    Log.d(TAG, "🔍 Type 0xF0: byte[8]=$battery (not updating - likely false positive)")
                }
            }
            0x88 -> {
                // Type 0x88 packet - CONTAINS REAL BATTERY at byte[8]!
                // Example: [136, 240, 1, 0, 12, 167, 1, 16, 69, 0, 0, 0, 83, 80, 0, 0, 14, 69, 0, 0]
                //                                      ^^
                //                                   byte[8] = 69% (real battery)
                val battery = value[8].toInt() and 0xFF
                if (battery in 1..100) {
                    Log.i(TAG, "🔋🔋� BATTERY (type 0x88): $battery% 🔋🔋🔋")
                    _ringData.value = _ringData.value.copy(
                        battery = battery,
                        lastUpdate = System.currentTimeMillis()
                    )
                }
            }
            0x81 -> {
                // Acknowledgment packet
                Log.d(TAG, "🔍 Type 0x81 - ACK packet")
            }
            else -> {
                // Check if any unknown packet has battery at byte[8]
                if (value.size > 8) {
                    val potentialBattery = value[8].toInt() and 0xFF
                    if (potentialBattery in 60..100 && _ringData.value.battery == null) {
                        Log.i(TAG, "🔋 Found battery in unknown packet type 0x${packetType.toString(16).uppercase()}: byte[8]=$potentialBattery")
                        _ringData.value = _ringData.value.copy(
                            battery = potentialBattery,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
                Log.d(TAG, "🔍 Unknown packet type 0x${packetType.toString(16).uppercase()}")
            }
        }
        
        Log.i(TAG, "═══════════════════════════════════")
    }

    /**
     * Disconnect from ring
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        Log.d(TAG, "Disconnecting...")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.Connected
    }
    
    /**
     * Start keep-alive mechanism to prevent BLE disconnect
     * Sends periodic RSSI read requests to maintain connection
     */
    @SuppressLint("MissingPermission")
    private fun startKeepAlive() {
        stopKeepAlive()  // Clear any existing
        
        keepAliveRunnable = object : Runnable {
            override fun run() {
                val gatt = bluetoothGatt
                if (gatt != null && _connectionState.value == ConnectionState.Connected) {
                    // Read RSSI to keep connection alive
                    val rssiRead = gatt.readRemoteRssi()
                    Log.d(TAG, "📶 Keep-alive ping (RSSI read: $rssiRead)")
                    
                    // Schedule next ping
                    keepAliveHandler.postDelayed(this, KEEP_ALIVE_INTERVAL)
                } else {
                    Log.d(TAG, "📶 Keep-alive stopped - not connected")
                }
            }
        }
        
        // Start after initial delay
        keepAliveHandler.postDelayed(keepAliveRunnable!!, KEEP_ALIVE_INTERVAL)
        Log.i(TAG, "📶 Keep-alive started (interval: ${KEEP_ALIVE_INTERVAL}ms)")
    }
    
    /**
     * Stop keep-alive mechanism
     */
    private fun stopKeepAlive() {
        keepAliveRunnable?.let {
            keepAliveHandler.removeCallbacks(it)
            Log.d(TAG, "📶 Keep-alive stopped")
        }
        keepAliveRunnable = null
    }

    /**
     * Manually read battery
     */
    @SuppressLint("MissingPermission")
    fun refreshBattery() {
        val gatt = bluetoothGatt ?: return
        gatt.getService(BATTERY_SERVICE_UUID)?.getCharacteristic(BATTERY_LEVEL_UUID)?.let { char ->
            gatt.readCharacteristic(char)
        }
    }

    /**
     * Start heart rate measurement
     * Writes command to EFE1 to trigger HR measurement
     * Uses YC SDK protocol format with 0x0F header
     */
    @SuppressLint("MissingPermission")
    fun startHeartRateMeasurement() {
        val gatt = bluetoothGatt ?: run {
            Log.w(TAG, "Cannot start HR measurement - not connected")
            return
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "❤️ STARTING HEART RATE MEASUREMENT")
        Log.i(TAG, "═══════════════════════════════════")
        
        // Simplified HR commands - only send essential ones to avoid disconnect
        // Too many commands causes GATT_CONN_TIMEOUT (status=8)
        val hrCommands = listOf(
            byteArrayOf(0x0F, 0x15, 0x01),  // Start HR measurement
        )
        
        // Primary: Write to EFE1 (main command characteristic on EFE0 service)
        gatt.getService(CUSTOM_SERVICE_EFE0)?.getCharacteristic(CUSTOM_CHAR_EFE1)?.let { char ->
            if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                
                hrCommands.forEachIndexed { index, cmd ->
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (bluetoothGatt != null) {  // Check still connected
                            Log.i(TAG, "❤️ Sending HR command to EFE1: ${cmd.joinToString { String.format("%02X", it) }}")
                            char.value = cmd
                            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            gatt.writeCharacteristic(char)
                        }
                    }, 3000L * index)  // Increased spacing to 3s
                }
            } else {
                Log.d(TAG, "EFE1 does not support write")
            }
        } ?: Log.d(TAG, "EFE1 characteristic not found")
        
        // Note: Removed FEC7 backup command - was causing GATT_CONN_TIMEOUT (status=8)
        
        // Update UI to show measuring
        _ringData.value = _ringData.value.copy(
            heartRateMeasuring = true,
            lastUpdate = System.currentTimeMillis()
        )
        
        // Show progress Toast
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "❤️ Measuring HR... Keep wearing the ring (30 sec)", Toast.LENGTH_LONG).show()
        }
        
        // Show a mid-progress Toast at 15 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (_ringData.value.heartRateMeasuring) {
                Toast.makeText(context, "❤️ Still measuring... 15 seconds remaining", Toast.LENGTH_SHORT).show()
            }
        }, 15000L)
        
        // Reset measuring flag after 35 seconds (30s measurement + 5s buffer)
        Handler(Looper.getMainLooper()).postDelayed({
            if (_ringData.value.heartRateMeasuring) {
                _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
                Log.w(TAG, "❤️ HR measurement timed out after 35s - ensure ring is worn properly")
                Toast.makeText(context, "❤️ Measurement timeout - try again", Toast.LENGTH_SHORT).show()
            }
        }, 35000L)
    }

    /**
     * Stop heart rate measurement
     */
    @SuppressLint("MissingPermission")
    fun stopHeartRateMeasurement() {
        val gatt = bluetoothGatt ?: return
        
        Log.i(TAG, "❤️ STOPPING HEART RATE MEASUREMENT")
        
        // Send stop command
        val stopCmd = byteArrayOf(0x15, 0x00)  // Stop HR measurement
        
        gatt.getService(SERVICE_FEE7)?.getCharacteristic(CHAR_FEC7)?.let { char ->
            char.value = stopCmd
            gatt.writeCharacteristic(char)
        }
        
        _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
    }
    
    /**
     * Start blood pressure measurement
     */
    @SuppressLint("MissingPermission")
    fun startBloodPressureMeasurement() {
        val gatt = bluetoothGatt ?: run {
            Log.w(TAG, "Cannot start BP measurement - not connected")
            return
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🩺 STARTING BLOOD PRESSURE MEASUREMENT")
        Log.i(TAG, "═══════════════════════════════════")
        
        // BP measurement commands (type 0x02 in YC protocol)
        val bpCommands = listOf(
            byteArrayOf(0x0F, 0x17, 0x01),  // Start BP measurement
            byteArrayOf(0x17, 0x01),         // Fallback
        )
        
        gatt.getService(CUSTOM_SERVICE_EFE0)?.getCharacteristic(CUSTOM_CHAR_EFE1)?.let { char ->
            bpCommands.forEachIndexed { index, cmd ->
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.i(TAG, "🩺 Sending BP command: ${cmd.joinToString { String.format("%02X", it) }}")
                    char.value = cmd
                    char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    gatt.writeCharacteristic(char)
                }, 1500L * index)
            }
        }
        
        _ringData.value = _ringData.value.copy(
            bloodPressureMeasuring = true,
            lastUpdate = System.currentTimeMillis()
        )
        
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "🩺 Measuring BP... Keep wearing the ring (30 sec)", Toast.LENGTH_LONG).show()
        }
        
        // Timeout after 40 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (_ringData.value.bloodPressureMeasuring) {
                _ringData.value = _ringData.value.copy(bloodPressureMeasuring = false)
                Log.w(TAG, "🩺 BP measurement timed out")
                Toast.makeText(context, "🩺 BP measurement timeout - try again", Toast.LENGTH_SHORT).show()
            }
        }, 40000L)
    }
    
    /**
     * Stop blood pressure measurement
     */
    @SuppressLint("MissingPermission")
    fun stopBloodPressureMeasurement() {
        val gatt = bluetoothGatt ?: return
        
        Log.i(TAG, "🩺 STOPPING BLOOD PRESSURE MEASUREMENT")
        
        val stopCmd = byteArrayOf(0x17, 0x00)
        gatt.getService(CUSTOM_SERVICE_EFE0)?.getCharacteristic(CUSTOM_CHAR_EFE1)?.let { char ->
            char.value = stopCmd
            gatt.writeCharacteristic(char)
        }
        
        _ringData.value = _ringData.value.copy(bloodPressureMeasuring = false)
    }
    
    /**
     * Start SpO2 (blood oxygen) measurement
     */
    @SuppressLint("MissingPermission")
    fun startSpO2Measurement() {
        val gatt = bluetoothGatt ?: run {
            Log.w(TAG, "Cannot start SpO2 measurement - not connected")
            return
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🫁 STARTING SPO2 MEASUREMENT")
        Log.i(TAG, "═══════════════════════════════════")
        
        // SpO2 measurement commands (type 0x03 in YC protocol)
        val spo2Commands = listOf(
            byteArrayOf(0x0F, 0x18, 0x01),  // Start SpO2 measurement
            byteArrayOf(0x18, 0x01),         // Fallback
        )
        
        gatt.getService(CUSTOM_SERVICE_EFE0)?.getCharacteristic(CUSTOM_CHAR_EFE1)?.let { char ->
            spo2Commands.forEachIndexed { index, cmd ->
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.i(TAG, "🫁 Sending SpO2 command: ${cmd.joinToString { String.format("%02X", it) }}")
                    char.value = cmd
                    char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    gatt.writeCharacteristic(char)
                }, 1500L * index)
            }
        }
        
        _ringData.value = _ringData.value.copy(
            spO2Measuring = true,
            lastUpdate = System.currentTimeMillis()
        )
        
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "🫁 Measuring SpO2... Keep wearing the ring (30 sec)", Toast.LENGTH_LONG).show()
        }
        
        // Timeout after 40 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (_ringData.value.spO2Measuring) {
                _ringData.value = _ringData.value.copy(spO2Measuring = false)
                Log.w(TAG, "🫁 SpO2 measurement timed out")
                Toast.makeText(context, "🫁 SpO2 measurement timeout - try again", Toast.LENGTH_SHORT).show()
            }
        }, 40000L)
    }
    
    /**
     * Stop SpO2 measurement
     */
    @SuppressLint("MissingPermission")
    fun stopSpO2Measurement() {
        val gatt = bluetoothGatt ?: return
        
        Log.i(TAG, "🫁 STOPPING SPO2 MEASUREMENT")
        
        val stopCmd = byteArrayOf(0x18, 0x00)
        gatt.getService(CUSTOM_SERVICE_EFE0)?.getCharacteristic(CUSTOM_CHAR_EFE1)?.let { char ->
            char.value = stopCmd
            gatt.writeCharacteristic(char)
        }
        
        _ringData.value = _ringData.value.copy(spO2Measuring = false)
    }
}
