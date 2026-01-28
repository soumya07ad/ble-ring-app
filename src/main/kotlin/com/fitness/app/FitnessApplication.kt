package com.fitness.app

import android.app.Application
import android.util.Log
import com.fitness.app.core.di.AppContainer
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.response.BleConnectResponse
import com.yucheng.ycbtsdk.response.BleDeviceToAppDataResponse

/**
 * Application class for one-time initialization
 * 
 * YCBTClient SDK APPROACH:
 * - SDK handles all BLE operations (scan, connect, data)
 * - Must be initialized before any BLE operations
 * 
 * MVVM: Initializes DI container for dependency injection
 */
class FitnessApplication : Application() {

    companion object {
        private const val TAG = "FitnessApplication"
        
        @Volatile
        private var instance: FitnessApplication? = null
        
        fun getInstance(): FitnessApplication = instance!!
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize MVVM DI Container
        AppContainer.initialize(this)
        Log.i(TAG, "✓ MVVM DI Container initialized")
        
        // ══════════════════════════════════════════════════════════
        // CRITICAL: Initialize YCBTClient SDK
        // This MUST be called before any BLE operations!
        // ══════════════════════════════════════════════════════════
        initializeYCBTClientSDK()
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ App started - YCBTClient SDK mode")
        Log.i(TAG, "✓ SDK handles: Scan, Connect, Data")
        Log.i(TAG, "═══════════════════════════════════")
    }
    
    /**
     * Initialize YCBTClient SDK with all necessary callbacks
     * Based on YCBleSdkDemo reference implementation
     */
    private fun initializeYCBTClientSDK() {
        try {
            // 1. Initialize the SDK
            YCBTClient.initClient(this, true)
            Log.i(TAG, "✓ YCBTClient.initClient() called")
            
            // Note: setReconnect() not available in this SDK version
            // SDK handles reconnection internally
            
            // 2. Register global connection state listener
            YCBTClient.registerBleStateChange(object : BleConnectResponse {
                override fun onConnectResponse(code: Int) {
                    Log.i(TAG, "🔗 Connection state changed: $code")
                    
                    when (code) {
                        Constants.BLEState.Disconnect -> {
                            Log.w(TAG, "❌ Disconnected from device")
                        }
                        Constants.BLEState.Connected -> {
                            Log.i(TAG, "✓ Connected (waiting for ReadWriteOK)")
                        }
                        Constants.BLEState.ReadWriteOK -> {
                            Log.i(TAG, "✓✓ ReadWriteOK - Ready to communicate!")
                        }
                        else -> {
                            Log.d(TAG, "Unknown connection state: $code")
                        }
                    }
                }
            })
            Log.i(TAG, "✓ Registered BLE state change listener")
            
            // 3. Register device-to-app data callback
            YCBTClient.deviceToApp(object : BleDeviceToAppDataResponse {
                override fun onDataResponse(dataType: Int, dataMap: HashMap<*, *>?) {
                    Log.i(TAG, "📥 Device data received: type=$dataType")
                    if (dataMap != null) {
                        Log.d(TAG, "   Data: $dataMap")
                    }
                }
            })
            Log.i(TAG, "✓ Registered device-to-app data listener")
            
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "✓ YCBTClient SDK INITIALIZED!")
            Log.i(TAG, "═══════════════════════════════════")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize YCBTClient SDK: ${e.message}", e)
        }
    }
}
