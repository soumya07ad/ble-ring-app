package com.fitness.app.ble

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleConnectResponse
import com.yucheng.ycbtsdk.response.BleDataResponse
import com.yucheng.ycbtsdk.response.BleDeviceToAppDataResponse
import com.yucheng.ycbtsdk.response.BleRealDataResponse
import com.yucheng.ycbtsdk.bean.ScanDeviceBean
import com.fitness.app.domain.model.Ring
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SDK-based BLE Manager
 * 
 * Uses YC SDK for ALL ring operations:
 * - Connection management
 * - Battery retrieval
 * - Steps retrieval
 * - Heart rate measurement
 * 
 * CRITICAL: YCBTClient.initClient() MUST be called in Application.onCreate()
 * before using this manager!
 * 
 * Based on YCBleSdkDemo reference implementation.
 */
class SdkBleManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SdkBleManager"
        private const val DATA_REFRESH_INTERVAL_MS = 5000L  // Refresh every 5 seconds
        
        @Volatile
        private var INSTANCE: SdkBleManager? = null
        
        fun getInstance(context: Context): SdkBleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SdkBleManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    // Connection state
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    // Ring data
    private val _ringData = MutableStateFlow(RingData())
    val ringData: StateFlow<RingData> = _ringData.asStateFlow()
    
    // Scan results
    private val _scanResults = MutableStateFlow<List<Ring>>(emptyList())
    val scanResults: StateFlow<List<Ring>> = _scanResults.asStateFlow()
    
    // Internal state
    private var isInitialized = false
    private var connectedMacAddress: String? = null
    private var connectedDeviceName: String? = null
    
    // CONNECTION STATE GUARDS - Critical to prevent duplicate connect calls!
    @Volatile
    private var isConnecting = false
    
    // Handler for periodic data refresh
    private val handler = Handler(Looper.getMainLooper())
    private var isRefreshingData = false
    
    private val dataRefreshRunnable = object : Runnable {
        override fun run() {
            if (_connectionState.value is BleConnectionState.Connected) {
                Log.d(TAG, "⏰ Periodic data refresh...")
                getAllRealData()
                handler.postDelayed(this, DATA_REFRESH_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Initialize SDK and register callbacks
     * Note: YCBTClient.initClient() is called in FitnessApplication
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized, skipping...")
            return
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "Initializing SDK BLE Manager")
        Log.i(TAG, "═══════════════════════════════════")
        
        // Register connection state callback
        YCBTClient.registerBleStateChange(connectionCallback)
        Log.i(TAG, "✓ Registered connection state callback")
        
        // Register real-time data callback (for HR, etc.)
        YCBTClient.appRegisterRealDataCallBack(realDataCallback)
        Log.i(TAG, "✓ Registered real-time data callback")
        
        // Register device-to-app data callback
        YCBTClient.deviceToApp(deviceToAppCallback)
        Log.i(TAG, "✓ Registered device-to-app callback")
        
        isInitialized = true
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ SDK BLE Manager READY")
        Log.i(TAG, "═══════════════════════════════════")
    }
    
    // ==================== Scanning ====================
    
    /**
     * Start scanning for devices
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun startScan(durationSeconds: Int = 6) {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔍 SDK Starting Scan ($durationSeconds seconds)")
        Log.i(TAG, "═══════════════════════════════════")
        
        _scanResults.value = emptyList()
        val foundDevices = mutableListOf<Ring>()
        
        try {
            YCBTClient.startScanBle({ code: Int, device: ScanDeviceBean? ->
                if (device != null) {
                    val name = device.deviceName ?: device.device?.name ?: "Unknown Ring"
                    val mac = device.deviceMac ?: device.device?.address ?: ""
                    val rssi = device.deviceRssi
                    
                    if (mac.isNotEmpty()) {
                        Log.d(TAG, "📍 Found: $name ($mac) RSSI: $rssi")
                        
                        // Avoid duplicates
                        if (foundDevices.none { it.macAddress == mac }) {
                            val ring = Ring(
                                name = name,
                                macAddress = mac,
                                rssi = rssi,
                                isConnected = false
                            )
                            foundDevices.add(ring)
                            _scanResults.value = foundDevices.toList()
                        }
                    }
                }
                
                if (code != 0) {
                    Log.d(TAG, "🔍 Scan callback code: $code")
                }
            }, durationSeconds)
            
            Log.i(TAG, "✓ Scan started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Scan failed: ${e.message}", e)
        }
    }
    
    /**
     * Stop scanning
     */
    fun stopScan() {
        Log.i(TAG, "🛑 SDK Stop Scan")
        try {
            YCBTClient.stopScanBle()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}")
        }
    }
    
    // ==================== Connection ====================
    
    /**
     * Connect to a BLE device using SDK
     * 
     * GUARDS:
     * - Prevents duplicate connect() calls
     * - Checks current connection state
     * - SDK owns connection lifecycle
     */
    fun connectToDevice(macAddress: String, deviceName: String? = null) {
        // GUARD 1: Already connecting
        if (isConnecting) {
            Log.w(TAG, "⚠️ Already connecting, ignoring duplicate connect() call")
            return
        }
        
        // GUARD 2: Already connected to this device
        val currentState = _connectionState.value
        if (currentState is BleConnectionState.Connected && connectedMacAddress == macAddress) {
            Log.w(TAG, "⚠️ Already connected to $macAddress, ignoring")
            return
        }
        
        // GUARD 3: Already connected to different device - disconnect first
        if (currentState is BleConnectionState.Connected) {
            Log.i(TAG, "Already connected, disconnecting from current device first...")
            disconnect()
        }
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "🔗 SDK Connecting to: $macAddress")
        Log.i(TAG, "═══════════════════════════════════")
        
        // Set state BEFORE calling SDK
        isConnecting = true
        _connectionState.value = BleConnectionState.Connecting
        connectedMacAddress = macAddress
        connectedDeviceName = deviceName ?: "R9 Ring"
        
        try {
            // SINGLE connect call - SDK owns the rest
            YCBTClient.connectBle(macAddress, object : BleConnectResponse {
                override fun onConnectResponse(code: Int) {
                    Log.i(TAG, "🔗 SDK Connect response: code=$code")
                    // SDK callback handles state transition
                    handleConnectionStateChange(code)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection failed: ${e.message}", e)
            isConnecting = false
            _connectionState.value = BleConnectionState.Disconnected
        }
    }
    
    /**
     * Disconnect from device
     * 
     * Clears all connection state and lets SDK handle cleanup
     */
    fun disconnect() {
        Log.i(TAG, "🔌 SDK Disconnecting from device")
        
        // Reset state flags BEFORE SDK call
        isConnecting = false
        stopDataRefresh()
        
        try {
            YCBTClient.disconnectBle()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
        
        connectedMacAddress = null
        connectedDeviceName = null
        _connectionState.value = BleConnectionState.Disconnected
        _ringData.value = RingData()
    }
    
    // ==================== Data Retrieval ====================
    
    /**
     * Get ALL real-time data from device
     * This is the MAIN method used by the Chinese demo app for data retrieval!
     * Returns: battery, heart rate, blood pressure, steps, etc.
     */
    fun getAllRealData() {
        Log.d(TAG, "📊 Requesting ALL real-time data...")
        
        try {
            YCBTClient.getAllRealDataFromDevice(object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    if (code == 0 && resultMap != null) {
                        Log.i(TAG, "═══════════════════════════════════")
                        Log.i(TAG, "📊 SDK Real Data: $resultMap")
                        Log.i(TAG, "═══════════════════════════════════")
                        
                        parseRealDataResponse(resultMap)
                    } else {
                        Log.w(TAG, "getAllRealDataFromDevice failed: code=$code")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting real data: ${e.message}", e)
        }
    }
    
    /**
     * Parse the response from getAllRealDataFromDevice
     */
    private fun parseRealDataResponse(resultMap: HashMap<*, *>) {
        var updated = false
        var currentData = _ringData.value
        
        // Battery
        val battery = resultMap["battery"] as? Int
            ?: resultMap["Battery"] as? Int
            ?: resultMap["电量"] as? Int
        if (battery != null && battery in 1..100) {
            Log.i(TAG, "🔋 Battery: $battery%")
            currentData = currentData.copy(battery = battery)
            updated = true
        }
        
        // Heart Rate
        val heartRate = resultMap["heartValue"] as? Int
            ?: resultMap["heart"] as? Int
            ?: resultMap["心率"] as? Int
        if (heartRate != null && heartRate in 40..220) {
            Log.i(TAG, "❤️ Heart Rate: $heartRate bpm")
            currentData = currentData.copy(heartRate = heartRate)
            updated = true
        }
        
        // Steps
        val steps = resultMap["step"] as? Int
            ?: resultMap["steps"] as? Int
            ?: resultMap["sportStep"] as? Int
            ?: resultMap["步数"] as? Int
        if (steps != null && steps >= 0) {
            Log.i(TAG, "👟 Steps: $steps")
            currentData = currentData.copy(steps = steps)
            updated = true
        }
        
        // Blood Oxygen (SpO2)
        val spo2 = resultMap["bloodOxygen"] as? Int
            ?: resultMap["spo2"] as? Int
            ?: resultMap["血氧"] as? Int
        if (spo2 != null && spo2 in 80..100) {
            Log.i(TAG, "💨 SpO2: $spo2%")
            currentData = currentData.copy(spO2 = spo2)
            updated = true
        }
        
        // Stress
        val stress = resultMap["stress"] as? Int
            ?: resultMap["压力"] as? Int
        if (stress != null && stress in 0..100) {
            Log.i(TAG, "😰 Stress: $stress")
            currentData = currentData.copy(stress = stress)
            updated = true
        }
        
        // Distance (meters)
        val distance = resultMap["sportDistance"] as? Int
            ?: resultMap["distance"] as? Int
        if (distance != null && distance >= 0) {
            currentData = currentData.copy(distance = distance)
            updated = true
        }
        
        // Calories
        val calories = resultMap["sportCalorie"] as? Int
            ?: resultMap["calories"] as? Int
        if (calories != null && calories >= 0) {
            currentData = currentData.copy(calories = calories)
            updated = true
        }
        
        if (updated) {
            currentData = currentData.copy(lastUpdate = System.currentTimeMillis())
            _ringData.value = currentData
            Log.i(TAG, "✓ Ring data updated!")
        }
    }
    
    /**
     * Get battery and device info from SDK
     */
    fun refreshDeviceInfo() {
        Log.i(TAG, "📱 Requesting device info...")
        
        try {
            YCBTClient.getDeviceInfo(object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    if (code == 0 && resultMap != null) {
                        Log.i(TAG, "📱 SDK Device Info: $resultMap")
                        
                        // Extract battery
                        val battery = resultMap["deviceBatteryValue"] as? Int
                            ?: resultMap["battery"] as? Int
                            ?: (resultMap["deviceBatteryValue"] as? String)?.toIntOrNull()
                        
                        if (battery != null && battery in 1..100) {
                            Log.i(TAG, "🔋🔋🔋 BATTERY: $battery% 🔋🔋🔋")
                            _ringData.value = _ringData.value.copy(
                                battery = battery,
                                lastUpdate = System.currentTimeMillis()
                            )
                        }
                    } else {
                        Log.w(TAG, "getDeviceInfo failed: code=$code")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device info: ${e.message}", e)
        }
    }
    
    /**
     * Get steps from SDK historical data
     */
    fun refreshStepsData() {
        Log.i(TAG, "👟 Requesting steps data...")
        
        try {
            YCBTClient.healthHistoryData(Constants.DATATYPE.Health_HistorySport, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    if (code == 0 && resultMap != null) {
                        Log.i(TAG, "👟 SDK Sport Data: $resultMap")
                        
                        // Parse steps from sport data
                        val data = resultMap["data"] as? ArrayList<*>
                        if (data != null && data.isNotEmpty()) {
                            val latest = data.last() as? HashMap<*, *>
                            val steps = latest?.get("sportStep") as? Int
                            
                            if (steps != null) {
                                Log.i(TAG, "👟👟👟 STEPS: $steps 👟👟👟")
                                _ringData.value = _ringData.value.copy(
                                    steps = steps,
                                    lastUpdate = System.currentTimeMillis()
                                )
                            }
                        }
                    } else {
                        Log.w(TAG, "healthHistoryData failed: code=$code")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting steps: ${e.message}", e)
        }
    }
    
    // ==================== Heart Rate ====================
    
    /**
     * Start heart rate measurement
     */
    fun startHeartRateMeasurement() {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "❤️ STARTING SDK HEART RATE MEASUREMENT")
        Log.i(TAG, "═══════════════════════════════════")
        
        _ringData.value = _ringData.value.copy(heartRateMeasuring = true)
        
        try {
            // Type 0 = heart rate
            YCBTClient.appStartMeasurement(1, 0, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.i(TAG, "❤️ HR measurement start response: code=$code")
                    if (code != 0) {
                        Log.w(TAG, "HR measurement failed to start")
                        _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error starting HR: ${e.message}", e)
            _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
        }
    }
    
    /**
     * Stop heart rate measurement
     */
    fun stopHeartRateMeasurement() {
        Log.i(TAG, "❤️ Stopping HR measurement")
        
        try {
            YCBTClient.appStartMeasurement(0, 0, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.i(TAG, "❤️ HR measurement stop: code=$code")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping HR: ${e.message}")
        }
        
        _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
    }
    
    // ==================== Periodic Refresh ====================
    
    private fun startDataRefresh() {
        if (!isRefreshingData) {
            isRefreshingData = true
            Log.i(TAG, "⏰ Starting periodic data refresh (${DATA_REFRESH_INTERVAL_MS}ms)")
            handler.postDelayed(dataRefreshRunnable, DATA_REFRESH_INTERVAL_MS)
        }
    }
    
    private fun stopDataRefresh() {
        isRefreshingData = false
        handler.removeCallbacks(dataRefreshRunnable)
        Log.i(TAG, "⏰ Stopped periodic data refresh")
    }
    
    // ==================== SDK Callbacks ====================
    
    private val connectionCallback = object : BleConnectResponse {
        override fun onConnectResponse(code: Int) {
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "🔗 Connection State: code=$code")
            Log.i(TAG, "═══════════════════════════════════")
            handleConnectionStateChange(code)
        }
    }
    
    private val realDataCallback = object : BleRealDataResponse {
        override fun onRealDataResponse(dataType: Int, dataMap: HashMap<*, *>?) {
            Log.d(TAG, "📈 Real-time data: type=$dataType")
            
            if (dataMap == null) return
            
            when (dataType) {
                Constants.DATATYPE.Real_UploadHeart -> {
                    val hr = dataMap["heartValue"] as? Int
                    if (hr != null && hr in 40..220) {
                        Log.i(TAG, "❤️ Real-time HR: $hr bpm")
                        _ringData.value = _ringData.value.copy(
                            heartRate = hr,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
                Constants.DATATYPE.Real_UploadBlood -> {
                    val sbp = dataMap["bloodSBP"] as? Int
                    val dbp = dataMap["bloodDBP"] as? Int
                    if (sbp != null && dbp != null) {
                        Log.i(TAG, "💉 Blood Pressure: $sbp/$dbp")
                        _ringData.value = _ringData.value.copy(
                            bloodPressureSystolic = sbp,
                            bloodPressureDiastolic = dbp,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
            }
        }
    }
    
    private val deviceToAppCallback = object : BleDeviceToAppDataResponse {
        override fun onDataResponse(dataType: Int, dataMap: HashMap<*, *>?) {
            Log.d(TAG, "📲 Device-to-App: type=$dataType")
            
            if (dataMap == null) return
            
            // Handle measurement results
            val innerDataType = dataMap["dataType"] as? Int
            if (innerDataType == Constants.DATATYPE.DeviceMeasurementResult) {
                val data = dataMap["datas"] as? ByteArray
                if (data != null && data.size >= 2) {
                    val measureType = data[0].toInt()
                    val status = data[1].toInt()
                    Log.i(TAG, "📊 Measurement result: type=$measureType, status=$status")
                    
                    if (status == 1) {
                        // Measurement completed successfully
                        _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
                    }
                }
            }
        }
    }
    
    private fun handleConnectionStateChange(code: Int) {
        Log.i(TAG, "🔗 State change: code=$code (0x${Integer.toHexString(code)})")
        
        when (code) {
            Constants.BLEState.TimeOut -> {
                Log.w(TAG, "⏰ TimeOut (0x01)")
                isConnecting = false
                _connectionState.value = BleConnectionState.Disconnected
            }
            Constants.BLEState.Disconnect -> {
                Log.w(TAG, "❌ Disconnected (0x03)")
                isConnecting = false
                stopDataRefresh()
                _connectionState.value = BleConnectionState.Disconnected
                _ringData.value = RingData()
            }
            Constants.BLEState.Disconnecting -> {
                Log.i(TAG, "🔌 Disconnecting (0x04)")
                _connectionState.value = BleConnectionState.Connecting
            }
            Constants.BLEState.Connecting -> {
                Log.i(TAG, "🔗 Connecting (0x05)")
                _connectionState.value = BleConnectionState.Connecting
            }
            Constants.BLEState.Connected -> {
                Log.i(TAG, "✓ Connected (0x06) - waiting for services...")
                _connectionState.value = BleConnectionState.Connecting
            }
            Constants.BLEState.ServicesDiscovered -> {
                Log.i(TAG, "📋 Services Discovered (0x07)")
                _connectionState.value = BleConnectionState.Connecting
            }
            Constants.BLEState.CharacteristicDiscovered -> {
                Log.i(TAG, "📝 Characteristic Discovered (0x08)")
                _connectionState.value = BleConnectionState.Connecting
            }
            Constants.BLEState.CharacteristicNotification -> {
                Log.i(TAG, "🔔 Notification Enabled (0x09)")
                _connectionState.value = BleConnectionState.Connecting
            }
            Constants.BLEState.ReadWriteOK -> {
                Log.i(TAG, "═══════════════════════════════════")
                Log.i(TAG, "✓✓✓ ReadWriteOK (0x0a) - READY! ✓✓✓")
                Log.i(TAG, "═══════════════════════════════════")
                
                isConnecting = false
                
                _connectionState.value = connectedMacAddress?.let {
                    BleConnectionState.Connected(
                        Ring(macAddress = it, name = connectedDeviceName ?: "R9 Ring")
                    )
                } ?: BleConnectionState.Disconnected
                
                // Fetch all data on successful connection
                Log.i(TAG, "📊 Fetching initial data...")
                handler.postDelayed({
                    getAllRealData()
                    refreshDeviceInfo()
                    refreshStepsData()
                    startDataRefresh()
                }, 500)
            }
            else -> {
                Log.w(TAG, "Unknown state: $code")
            }
        }
    }
}
