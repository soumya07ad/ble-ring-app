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
     * Per SDK V1.0.4 documentation
     */
    private fun initializeYCBTClientSDK() {
        try {
            // 1. Initialize SDK with 3 parameters per docs:
            // initClient(context, isReconnect, isDebug)
            YCBTClient.initClient(this, true, true)
            Log.i(TAG, "✓ YCBTClient.initClient(context, isReconnect=true, isDebug=true)")
            
            // 2. Enable auto-reconnect per docs
            YCBTClient.setReconnect(true)
            Log.i(TAG, "✓ YCBTClient.setReconnect(true)")
            
            // 3. Register global connection state listener
            // State codes from docs:
            // 0x01=TimeOut, 0x02=NotOpen, 0x03=Disconnect, 0x04=Disconnecting
            // 0x05=Connecting, 0x06=Connected, 0x07=ServicesDiscovered
            // 0x08=CharacteristicDiscovered, 0x09=CharacteristicNotification
            // 0x0a=ReadWriteOK (decimal 10)
            YCBTClient.registerBleStateChange(object : BleConnectResponse {
                override fun onConnectResponse(code: Int) {
                    Log.i(TAG, "🔗 Connection state: code=$code (0x${Integer.toHexString(code)})")
                    
                    when (code) {
                        Constants.BLEState.TimeOut -> Log.w(TAG, "⏰ TimeOut (0x01)")
                        Constants.BLEState.Disconnect -> Log.w(TAG, "❌ Disconnected (0x03)")
                        Constants.BLEState.Disconnecting -> Log.i(TAG, "🔌 Disconnecting (0x04)")
                        Constants.BLEState.Connecting -> Log.i(TAG, "🔗 Connecting (0x05)")
                        Constants.BLEState.Connected -> Log.i(TAG, "✓ Connected (0x06)")
                        Constants.BLEState.ServicesDiscovered -> Log.i(TAG, "📋 Services Discovered (0x07)")
                        Constants.BLEState.CharacteristicDiscovered -> Log.i(TAG, "📝 Characteristic Discovered (0x08)")
                        Constants.BLEState.CharacteristicNotification -> Log.i(TAG, "🔔 Notification Enabled (0x09)")
                        Constants.BLEState.ReadWriteOK -> Log.i(TAG, "✓✓✓ ReadWriteOK - READY! (0x0a)")
                        else -> Log.d(TAG, "Unknown state: $code")
                    }
                }
            })
            Log.i(TAG, "✓ Registered BLE state change listener")
            
            // 4. Register device-to-app data callback
            YCBTClient.deviceToApp(object : BleDeviceToAppDataResponse {
                override fun onDataResponse(dataType: Int, dataMap: HashMap<*, *>?) {
                    Log.i(TAG, "📥 Device data: type=$dataType")
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
