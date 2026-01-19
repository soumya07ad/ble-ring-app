package com.fitness.app

import android.app.Application
import android.util.Log
import com.fitness.app.ble.NativeGattManager
import com.fitness.app.core.di.AppContainer

/**
 * Application class for one-time initialization
 * 
 * PURE NATIVE GATT VERSION - No SDK!
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
        
        // Initialize PURE NATIVE GATT Manager (no SDK!)
        NativeGattManager.getInstance(this).initialize()
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ App started with PURE NATIVE GATT")
        Log.i(TAG, "✓ NO SDK - 100% Native Android BLE")
        Log.i(TAG, "═══════════════════════════════════")
    }
}

