package com.fitness.app.ble

import android.util.Log
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleDataResponse
import com.yucheng.ycbtsdk.response.BleRealDataResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SDK-based Heart Rate Manager
 * 
 * Uses the YC SDK for heart rate measurement since native BLE doesn't
 * expose the proprietary HR protocol. Battery and steps use native BLE.
 */
class SdkHeartRateManager {
    
    companion object {
        private const val TAG = "SdkHeartRateManager"
        
        // Measurement types from SDK
        private const val MEASURE_TYPE_HEART_RATE = 0  // 0x00: Heart Rate
        private const val MEASURE_TYPE_BLOOD_PRESSURE = 1  // 0x01: Blood Pressure
        private const val MEASURE_TYPE_BLOOD_OXYGEN = 2  // 0x02: Blood Oxygen
    }
    
    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()
    
    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()
    
    private var isCallbackRegistered = false
    
    init {
        registerRealTimeDataCallback()
    }
    
    /**
     * Register callback to receive real-time health data from the SDK
     */
    private fun registerRealTimeDataCallback() {
        if (isCallbackRegistered) return
        
        try {
            YCBTClient.appRegisterRealDataCallBack(object : BleRealDataResponse {
                override fun onRealDataResponse(dataType: Int, dataMap: HashMap<*, *>?) {
                    Log.i(TAG, "═══════════════════════════════════")
                    Log.i(TAG, "❤️ SDK Real Data: type=$dataType, data=$dataMap")
                    Log.i(TAG, "═══════════════════════════════════")
                    
                    when (dataType) {
                        Constants.DATATYPE.Real_UploadHeart -> {
                            // Heart rate data received
                            val hr = dataMap?.get("heartValue") as? Int
                                ?: dataMap?.get("heart") as? Int
                                ?: dataMap?.get("value") as? Int
                            
                            if (hr != null && hr in 40..200) {
                                Log.i(TAG, "❤️❤️❤️ SDK HEART RATE: $hr bpm ❤️❤️❤️")
                                _heartRate.value = hr
                            }
                        }
                        Constants.DATATYPE.Real_UploadPPG -> {
                            // PPG data might contain HR too
                            Log.d(TAG, "PPG data: $dataMap")
                        }
                        else -> {
                            Log.d(TAG, "Other data type: $dataType")
                        }
                    }
                }
            })
            isCallbackRegistered = true
            Log.i(TAG, "✓ SDK real-time data callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register SDK callback: ${e.message}")
        }
    }
    
    /**
     * Start heart rate measurement using SDK
     */
    fun startHeartRateMeasurement() {
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "❤️ STARTING SDK HEART RATE MEASUREMENT")
        Log.i(TAG, "═══════════════════════════════════")
        
        _isMeasuring.value = true
        
        try {
            // Register callback if not already done
            registerRealTimeDataCallback()
            
            // Start measurement (type 0 = heart rate)
            // First param: 1 = start, 0 = stop
            // Second param: measurement type
            YCBTClient.appStartMeasurement(1, MEASURE_TYPE_HEART_RATE, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.i(TAG, "❤️ SDK startMeasurement response: code=$code, ratio=$ratio, data=$resultMap")
                    if (code == 0) {
                        Log.i(TAG, "❤️ SDK HR measurement started successfully")
                    } else {
                        Log.e(TAG, "❤️ SDK HR measurement failed with code: $code")
                        _isMeasuring.value = false
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SDK HR measurement: ${e.message}")
            _isMeasuring.value = false
        }
    }
    
    /**
     * Stop heart rate measurement
     */
    fun stopHeartRateMeasurement() {
        Log.i(TAG, "❤️ STOPPING SDK HEART RATE MEASUREMENT")
        
        try {
            // Stop measurement (type 0 = stop)
            YCBTClient.appStartMeasurement(0, MEASURE_TYPE_HEART_RATE, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.i(TAG, "❤️ SDK stopMeasurement response: code=$code")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop SDK HR measurement: ${e.message}")
        } finally {
            _isMeasuring.value = false
        }
    }
    
    /**
     * Get the current heart rate value
     */
    fun getCurrentHeartRate(): Int? = _heartRate.value
}
