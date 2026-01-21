package com.fitness.app

import android.app.Application
import android.util.Log
import com.fitness.app.ble.NativeGattManager
import com.fitness.app.core.di.AppContainer

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
        
        // Initialize Native GATT Manager (Pure Native)
        NativeGattManager.getInstance(this).initialize()
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ App started - PURE NATIVE GATT mode")
        Log.i(TAG, "✓ Battery & Steps: Native Byte Analysis (0xF0/FEA1)")
        Log.i(TAG, "═══════════════════════════════════")
    }
}

