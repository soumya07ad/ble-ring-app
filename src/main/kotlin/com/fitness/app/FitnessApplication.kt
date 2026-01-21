package com.fitness.app

import android.app.Application
import android.util.Log
import com.fitness.app.ble.NativeGattManager
import com.fitness.app.core.di.AppContainer
import com.yucheng.ycbtsdk.YCBTClient

/**
 * Application class for one-time initialization
 * 
 * HYBRID APPROACH:
 * - Native GATT for battery and steps
 * - YC SDK for heart rate measurement
 * 
 * MVVM: Initializes DI container for dependency injection
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
        
        // Initialize YC SDK for heart rate measurement
        try {
            YCBTClient.initClient(this, true)
            Log.i(TAG, "✓ YC SDK initialized for HR measurement")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init YC SDK: ${e.message}")
        }
        
        // Initialize Native GATT Manager (for battery and steps)
        NativeGattManager.getInstance(this).initialize()
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ App started - HYBRID mode")
        Log.i(TAG, "✓ Native BLE: Battery + Steps")
        Log.i(TAG, "✓ YC SDK: Heart Rate")
        Log.i(TAG, "═══════════════════════════════════")
    }
}

