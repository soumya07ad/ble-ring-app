package com.fitness.app

import android.app.Application
import android.util.Log
import com.fitness.app.core.di.AppContainer
import com.manridy.sdk_mrd2019.Manridy

/**
 * Application class for one-time initialization
 * 
 * Manridy MRD SDK APPROACH:
 * 1. Simple single-line initialization: Manridy.init(context)
 * 2. BLE operations handled through BleAdapter wrapper
 * 3. Data parsing via Manridy.getMrdSend() and Manridy.getMrdRead()
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
        // CRITICAL: Initialize Manridy MRD SDK
        // ══════════════════════════════════════════════════════════
        initializeManridySDK()
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ App started - Manridy MRD SDK mode")
        Log.i(TAG, "✓ SDK handles: R9 Ring detection & data")
        Log.i(TAG, "═══════════════════════════════════")
    }
    
    /**
     * Initialize Manridy MRD SDK - working SDK for R9 rings!
     */
    private fun initializeManridySDK() {
        try {
            // ═══════════════════════════════════════════════════════
            // STEP 1: Initialize MRD SDK (single line!)
            // ═══════════════════════════════════════════════════════
            Manridy.init(applicationContext)
            Log.i(TAG, "✓ Manridy.init(context)")
            
            Log.i(TAG, "═══════════════════════════════════")
            Log.i(TAG, "✓ Manridy MRD SDK FULLY INITIALIZED!")
            Log.i(TAG, "✓ Ready to detect fresh R9 rings!")
            Log.i(TAG, "═══════════════════════════════════")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize Manridy SDK: ${e.message}", e)
        }
    }
}
