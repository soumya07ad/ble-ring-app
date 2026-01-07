package com.fitness.app.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleScanResponse
import com.yucheng.ycbtsdk.response.BleConnectResponse

import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.bean.ScanDeviceBean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BLE Manager - Handles all BLE operations using YCBT SDK
 * Thread-safe singleton for managing BLE lifecycle
 */
class BleManager private constructor(private val context: Context) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val discoveredDevices = mutableMapOf<String, BleDevice>() // MAC -> Device
    private var isInitialized = false
    
    // Connection management
    private var isConnecting = false
    private var connectionTimeoutHandler: android.os.Handler? = null
    private var connectionStartTime: Long = 0

    companion object {
        private const val TAG = "BleManager"
        private const val DEFAULT_SCAN_DURATION = 6 // seconds

        @Volatile
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
            // Initialize YCBTClient with reconnect enabled
            YCBTClient.initClient(context, true, true) // context, isReconnect, isDebug
            
            // Initialize auto-reconnect manager (using reflection for SDK compatibility)
            try {
                val reconnectClass = Class.forName("com.yucheng.ycbtsdk.core.Reconnect")
                val getInstance = reconnectClass.getMethod("getInstance")
                val reconnectInstance = getInstance.invoke(null)
                val initMethod = reconnectClass.getMethod("init", Context::class.java, Boolean::class.java)
                initMethod.invoke(reconnectInstance, context, true)
                Log.d(TAG, "Reconnect initialized successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Reconnect not available or failed: ${e.message}")
            }
            
            // Register global connection state listener
            registerConnectionListener()
            
            isInitialized = true
            Log.d(TAG, "BLE initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BLE: ${e.message}", e)
        }
    }

    /**
     * Start BLE scan - Only call after permissions are granted
     * @param scanDuration Duration in seconds (default: 6)
     */
    fun startScan(scanDuration: Int = DEFAULT_SCAN_DURATION) {
        if (!isInitialized) {
            Log.e(TAG, "BLE not initialized. Call initialize() first.")
            _scanState.value = ScanState.Error("BLE not initialized")
            return
        }

        discoveredDevices.clear()
        _scanState.value = ScanState.Scanning

        YCBTClient.startScanBle(object : BleScanResponse {
            override fun onScanResponse(code: Int, device: ScanDeviceBean?) {
                // Filter out null devices
                if (device == null) return
                
                // CRITICAL: Use SDK getter methods (not properties)
                val macAddress = device.deviceMac
                if (macAddress.isNullOrBlank()) {
                    Log.w(TAG, "Scan result with null/blank MAC, skipping")
                    return
                }
                
                // Get device name (may be null/empty)
                val deviceName = device.deviceName
                val displayName = deviceName?.takeIf { it.isNotBlank() } ?: "Unknown Device"
                
                // CRITICAL FIX: Get RSSI from SDK (not hardcoded)
                // The SDK provides RSSI via getter method or property
                val rssi = try {
                    // Try to get RSSI - SDK may provide it as property or method
                    device.deviceRssi ?: -100
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get RSSI for $macAddress: ${e.message}")
                    -100  // Fallback only if SDK doesn't provide it
                }
                
                // Prevent duplicates using MAC address
                if (!discoveredDevices.containsKey(macAddress)) {
                    val bleDevice = BleDevice(displayName, macAddress, rssi)
                    discoveredDevices[macAddress] = bleDevice
                    
                    // Emit updated list
                    _scanState.value = ScanState.DevicesFound(discoveredDevices.values.toList())
                    Log.d(TAG, "Device found: $displayName")
                    Log.d(TAG, "   MAC: $macAddress")
                    Log.d(TAG, "   RSSI: $rssi dBm")
                    Log.d(TAG, "   Name from SDK: ${deviceName ?: "NULL"}")
                }
            }
        }, scanDuration)
        
        // Schedule scan finish check (SDK may not have explicit finish callback)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val finalDevices = discoveredDevices.values.toList()
            _scanState.value = if (finalDevices.isEmpty()) {
                ScanState.Error("No devices found")
            } else {
                ScanState.DevicesFound(finalDevices)
            }
            Log.d(TAG, "Scan finished. Found ${finalDevices.size} devices")
        }, (scanDuration * 1000).toLong())
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
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(macAddress)
            
            if (bluetoothDevice != null) {
                val bondState = bluetoothDevice.bondState
                Log.i(TAG, "   Current bond state: $bondState")
                
                when (bondState) {
                    BluetoothDevice.BOND_NONE -> {
                        Log.i(TAG, "   → Device not bonded, initiating Android system bonding...")
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
        
        Log.i(TAG, "   Starting connection timeout (15s)...")
        Log.i(TAG, "   → Calling YCBTClient.connectBle($macAddress)")
        // Start connection timeout (15 seconds)
        startConnectionTimeout(macAddress)

        YCBTClient.connectBle(macAddress, object : BleConnectResponse {
            override fun onConnectResponse(code: Int) {
                val elapsed = System.currentTimeMillis() - connectionStartTime
                Log.d(TAG, "Connection callback received: code=$code after ${elapsed}ms")
                
                // CRITICAL: YCBT SDK uses multiple state codes during connection
                // Code 1 = ReadWriteOK (legacy)
                // Code 6 = Connected (discovering services)
                // Code 7 = Fully connected (services discovered, MTU negotiated)
                // Code 2 = Disconnect
                // Code 3 = Disconnected
                // Code 4, 5 = Intermediate states (ignore)
                
                when (code) {
                    1 -> {
                        // Legacy ReadWriteOK state
                        cancelConnectionTimeout()
                        isConnecting = false
                        _connectionState.value = ConnectionState.Connected
                        Log.i(TAG, "✓ Connected successfully (state 1) to $macAddress")
                        Log.i(TAG, "═══════════════════════════════════")
                    }
                    6 -> {
                        // Connected, discovering services
                        Log.i(TAG, "✓ Connected (state 6) - discovering services...")
                        // Don't cancel timeout yet - wait for state 7
                    }
                    7 -> {
                        // FULLY CONNECTED - services discovered, MTU negotiated
                        cancelConnectionTimeout()
                        isConnecting = false
                        _connectionState.value = ConnectionState.Connected
                        Log.i(TAG, "✓ FULLY CONNECTED (state 7) to $macAddress")
                        Log.i(TAG, "   Services discovered, MTU negotiated")
                        Log.i(TAG, "═══════════════════════════════════")
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
        }, 15000) // 15 seconds
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
     * Handles reconnect and disconnect events
     */
    private fun registerConnectionListener() {
        YCBTClient.registerBleStateChange(object : BleConnectResponse {
            override fun onConnectResponse(code: Int) {
                Log.d(TAG, "Global state change: code=$code")
                when (code) {
                    1, 6, 7 -> {
                        // States 1, 6, 7 are all connected states
                        _connectionState.value = ConnectionState.Connected
                        Log.d(TAG, "Global: Device connected (state $code)")
                    }
                    2, 3 -> {
                        // States 2, 3 are disconnect states
                        _connectionState.value = ConnectionState.Disconnected
                        Log.d(TAG, "Global: Device disconnected (state $code)")
                    }
                    4, 5 -> {
                        // Intermediate states - ignore
                        Log.d(TAG, "Global: Intermediate state $code")
                    }
                    else -> {
                        Log.w(TAG, "Global: Unknown state code: $code")
                    }
                }
            }
        })
    }

    /**
     * Stop ongoing scan
     */
    fun stopScan() {
        // YCBT SDK auto-stops after duration, but we can reset state
        if (_scanState.value is ScanState.Scanning) {
            _scanState.value = ScanState.Idle
        }
    }
}
