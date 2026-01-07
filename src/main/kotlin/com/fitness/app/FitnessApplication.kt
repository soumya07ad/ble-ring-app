package com.fitness.app

import android.app.Application
import com.fitness.app.ble.BleManager

/**
 * Application class for one-time initialization
 */
class FitnessApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize BLE SDK once at app startup
        BleManager.getInstance(this).initialize()
    }
}
