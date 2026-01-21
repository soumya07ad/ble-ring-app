package com.fitness.app.ble

import android.content.Context
import android.util.Log
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleConnectResponse
import com.yucheng.ycbtsdk.response.BleDataResponse
import com.yucheng.ycbtsdk.response.BleDeviceToAppDataResponse
import com.yucheng.ycbtsdk.response.BleRealDataResponse
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
 * This replaces the native GATT approach to avoid connection conflicts.
 */
class SdkBleManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SdkBleManager"
        
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
    
    private var isInitialized = false
    private var connectedMacAddress: String? = null
    
    /**
     * Initialize SDK and register callbacks
     */
    fun initialize() {
        if (isInitialized) return
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "Initializing SDK BLE Manager")
        Log.i(TAG, "═══════════════════════════════════")
        
        // Register connection state callback
        YCBTClient.registerBleStateChange(connectionCallback)
        
        // Register real-time data callback (for HR)
        YCBTClient.appRegisterRealDataCallBack(realDataCallback)
        
        // Register device-to-app data callback
        YCBTClient.deviceToApp(deviceToAppCallback)
        
        isInitialized = true
        Log.i(TAG, "✓ SDK BLE Manager initialized")
    }
    
    // Scan results
    private val _scanResults = MutableStateFlow<List<Ring>>(emptyList())
    val scanResults: StateFlow<List<Ring>> = _scanResults.asStateFlow()
    
    /**
     * Start scanning for devices
     */
    fun startScan(durationSeconds: Int) {
        Log.i(TAG, "SDK Start Scan")
        _scanResults.value = emptyList()
        val foundDevices = mutableListOf<Ring>()
        
        YCBTClient.startScanBle({ code, device, rssi, scanRecord ->
            if (code == 0 && device != null) {
                val name = device.name ?: "Unknown Ring"
                val mac = device.address
                
                // Filter for our ring (optional: based on name or service UUIDs in previous logs)
                Log.d(TAG, "Found: $name ($mac) RSSI: $rssi")
                
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
        }, durationSeconds)
    }
    
    /**
     * Stop scanning
     */
    fun stopScan() {
        Log.i(TAG, "SDK Stop Scan")
        YCBTClient.stopScanBle()
    }
    
    /**
     * Connect to a BLE device using SDK
     */
    fun connectToDevice(macAddress: String) {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "� SDK Connecting to: $macAddress")
        Log.i(TAG, "═══════════════════════════════════")
        
        _connectionState.value = BleConnectionState.Connecting
        connectedMacAddress = macAddress
        
        YCBTClient.connectBle(macAddress, object : BleConnectResponse {
            override fun onConnectResponse(code: Int) {
                Log.i(TAG, "SDK Connect response: code=$code")
                handleConnectionStateChange(code)
            }
        })
    }
    
    /**
     * Disconnect from device
     */
    fun disconnect() {
        Log.i(TAG, "SDK Disconnecting from device")
        YCBTClient.disconnectBle()
        connectedMacAddress = null
        _connectionState.value = BleConnectionState.Disconnected
        _ringData.value = RingData()
    }
    
    /**
     * Get battery and device info from SDK
     */
    fun refreshDeviceInfo() {
        Log.i(TAG, "Requesting device info from SDK...")
        
        YCBTClient.getDeviceInfo(object : BleDataResponse {
            override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                if (code == 0 && resultMap != null) {
                    Log.i(TAG, "═══════════════════════════════════")
                    Log.i(TAG, "� SDK Device Info: $resultMap")
                    Log.i(TAG, "═══════════════════════════════════")
                    
                    // Extract battery percentage
                    val battery = resultMap["battery"] as? Int ?: resultMap["电量"] as? Int
                    if (battery != null && battery in 1..100) {
                        Log.i(TAG, "🔋🔋🔋 SDK BATTERY: $battery% 🔋🔋🔋")
                        _ringData.value = _ringData.value.copy(
                            battery = battery,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
            }
        })
    }
    
    /**
     * Get steps from SDK historical data
     */
    fun refreshStepsData() {
        Log.i(TAG, "Requesting steps data from SDK...")
        
        YCBTClient.healthHistoryData(Constants.DATATYPE.Health_HistorySport, object : BleDataResponse {
            override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                if (code == 0 && resultMap != null) {
                    Log.i(TAG, "═══════════════════════════════════")
                    Log.i(TAG, "👟 SDK Sport Data: $resultMap")
                    Log.i(TAG, "═══════════════════════════════════")
                    
                    // Parse steps from sport data
                    val data = resultMap["data"] as? ArrayList<*>
                    if (data != null && data.isNotEmpty()) {
                        // Get the latest entry
                        val latest = data.last() as? HashMap<*, *>
                        val steps = latest?.get("sportStep") as? Int
                        
                        if (steps != null) {
                            Log.i(TAG, "👟👟👟 SDK STEPS: $steps 👟👟👟")
                            _ringData.value = _ringData.value.copy(
                                steps = steps,
                                lastUpdate = System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
        })
    }
    
    /**
     * Start heart rate measurement
     */
    fun startHeartRateMeasurement() {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "❤️ STARTING SDK HEART RATE MEASUREMENT")
        Log.i(TAG, "═══════════════════════════════════")
        
        _ringData.value = _ringData.value.copy(heartRateMeasuring = true)
        
        // Type 0 = heart rate
        YCBTClient.appStartMeasurement(1, 0, object : BleDataResponse {
            override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                Log.i(TAG, "❤️ SDK HR measurement start: code=$code")
                if (code != 0) {
                    _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
                }
            }
        })
    }
    
    /**
     * Stop heart rate measurement
     */
    fun stopHeartRateMeasurement() {
        Log.i(TAG, "❤️ STOPPING SDK HR MEASUREMENT")
        
        YCBTClient.appStartMeasurement(0, 0, object : BleDataResponse {
            override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                Log.i(TAG, "❤️ SDK HR measurement stop: code=$code")
            }
        })
        
        _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
    }
    
    // ==================== SDK Callbacks ====================
    
    private val connectionCallback = object : BleConnectResponse {
        override fun onConnectResponse(code: Int) {
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "� SDK Connection State Changed: code=$code")
            Log.i(TAG, "═══════════════════════════════════")
            handleConnectionStateChange(code)
        }
    }
    
    private val realDataCallback = object : BleRealDataResponse {
        override fun onRealDataResponse(dataType: Int, dataMap: HashMap<*, *>?) {
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "❤️ SDK Real Data: type=$dataType, data=$dataMap")
            Log.i(TAG, "═══════════════════════════════════")
            
            when (dataType) {
                Constants.DATATYPE.Real_UploadHeart -> {
                    // Real-time heart rate data
                    val hr = dataMap?.get("heartValue") as? Int
                    if (hr != null && hr in 40..200) {
                        Log.i(TAG, "❤️❤️❤️ SDK HEART RATE: $hr bpm ❤️❤️❤️")
                        _ringData.value = _ringData.value.copy(
                            heartRate = hr,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
            }
        }
    }
    
    private val deviceToAppCallback = object : BleDeviceToAppDataResponse {
        override fun onDataResponse(dataType: Int, dataMap: HashMap<*, *>?) {
            Log.i(TAG, "Device to App: type=$dataType, data=$dataMap")
            
            // Handle measurement completion
            if (dataType == 0 && dataMap != null) {
                when (dataMap["dataType"] as? Int) {
                    Constants.DATATYPE.DeviceMeasurementResult -> {
                        val data = dataMap["datas"] as? ByteArray
                        if (data != null && data.size >= 2) {
                            // data[0] = measurement type, data[1] = status
                            if (data[1].toInt() == 1) {
                                Log.i(TAG, "❤️ HR measurement completed successfully")
                                _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun handleConnectionStateChange(code: Int) {
        when (code) {
            Constants.BLEState.Disconnect -> {
                Log.i(TAG, "� SDK Disconnected")
                _connectionState.value = BleConnectionState.Disconnected
                _ringData.value = RingData()
            }
            Constants.BLEState.Connected -> {
                Log.i(TAG, "� SDK Connected (establishing services...)")
                _connectionState.value = BleConnectionState.Connecting
            }
            Constants.BLEState.ReadWriteOK -> {
                Log.i(TAG, "✓✓✓ SDK READY (services discovered)")
                _connectionState.value = connectedMacAddress?.let {
                    BleConnectionState.Connected(Ring(macAddress = it, name = "R9 Ring"))
                } ?: BleConnectionState.Disconnected
                
                // Automatically fetch device info and steps
                refreshDeviceInfo()
                refreshStepsData()
            }
            else -> {
                Log.w(TAG, "Unknown connection state: $code")
            }
        }
    }
}
