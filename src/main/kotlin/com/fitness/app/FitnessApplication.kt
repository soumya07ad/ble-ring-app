package com.fitness.app

import android.app.Application
import android.util.Log
import com.fitness.app.core.di.AppContainer

/**
 * Application class for one-time initialization
 * 
 * NATIVE BLE APPROACH (No SDK):
 * 1. No SDK initialization required
 * 2. BLE operations handled through NativeGattManager
 * 3. Data parsing via raw byte arrays and reverse-engineered protocol
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
        
        Log.i(TAG, "═══════════════════════════════════")
        Log.i(TAG, "✓ App started — Native BLE mode")
        Log.i(TAG, "✓ No SDK dependency")
        Log.i(TAG, "✓ Pure byte-array protocol")
        Log.i(TAG, "═══════════════════════════════════")

        setupBackgroundSync()
    }

    private fun setupBackgroundSync() {
        val constraints = androidx.work.Constraints.Builder()
            // Require ANY form of internet (Wi-Fi or Mobile Data) as per user request
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.fitness.app.network.sync.BackendSyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES // Minimum allowed periodic interval is 15 minutes
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BackendSyncWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        Log.i(TAG, "✓ Backend Sync Worker Scheduled")
    }
}
