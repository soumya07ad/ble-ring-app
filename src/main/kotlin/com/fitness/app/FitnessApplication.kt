package com.fitness.app

import android.app.Application
import android.util.Log
import com.fitness.app.ble.BleManager
import com.fitness.app.core.di.AppContainer
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.response.BleDeviceToAppDataResponse
import java.util.HashMap

/**
 * Application class for one-time initialization
 * 
 * DEMO APP PATTERN: Register global deviceToApp callback here
 * This receives data pushed by the ring without needing to request it
 * 
 * MVVM: Also initializes DI container for dependency injection
 */
class FitnessApplication : Application() {

    companion object {
        private const val TAG = "FitnessApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize MVVM DI Container
        AppContainer.initialize(this)
        Log.i(TAG, "✓ MVVM DI Container initialized")
        
        // Initialize BLE SDK once at app startup
        BleManager.getInstance(this).initialize()
        
        // CRITICAL: Register global deviceToApp callback
        // The demo app does this in MyApplication.java
        // This receives data pushed by the ring (battery, health data, etc.)
        registerDeviceToAppCallback()
    }
    
    /**
     * Register global callback for data pushed from ring to app
     * The ring may push battery updates and other data without us requesting it
     */
    private fun registerDeviceToAppCallback() {
        Log.i(TAG, "Registering global deviceToApp callback...")
        
        YCBTClient.deviceToApp(object : BleDeviceToAppDataResponse {
            override fun onDataResponse(dataType: Int, dataMap: HashMap<*, *>?) {
                Log.i(TAG, "═══════════════════════════════════")
                Log.i(TAG, "✓ DEVICE PUSHED DATA TO APP!")
                Log.i(TAG, "═══════════════════════════════════")
                Log.i(TAG, "dataType: $dataType")
                Log.i(TAG, "dataMap: $dataMap")
                
                if (dataMap != null) {
                    // Forward the data to BleManager for processing
                    BleManager.getInstance(this@FitnessApplication)
                        .handleDevicePushData(dataType, dataMap)
                }
            }
        })
        
        Log.i(TAG, "✓ deviceToApp callback registered")
    }
}
