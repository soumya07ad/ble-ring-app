package com.fitness.app.core.di

import android.content.Context
import com.fitness.app.ble.NativeGattManager
import com.fitness.app.ble.SdkHeartRateManager
import com.fitness.app.data.repository.RingRepositoryImpl
import com.fitness.app.domain.repository.IRingRepository
import com.fitness.app.domain.usecase.ConnectRingUseCase
import com.fitness.app.domain.usecase.DisconnectRingUseCase
import com.fitness.app.domain.usecase.GetRingDataUseCase
import com.fitness.app.domain.usecase.ScanDevicesUseCase

/**
 * Manual Dependency Injection Container
 * 
 * Provides singleton instances of repositories and use cases.
 * This is a simple DI solution without Hilt/Dagger complexity.
 * 
 * Usage:
 * ```
 * val container = AppContainer.getInstance(context)
 * val useCase = container.connectRingUseCase
 * ```
 */
class AppContainer private constructor(context: Context) {
    
    // Native GATT Manager (for direct BLE access - battery, steps)
    val nativeGattManager: NativeGattManager by lazy {
        NativeGattManager.getInstance(context)
    }
    
    // SDK Heart Rate Manager (for HR measurement via YC SDK)
    val sdkHeartRateManager: SdkHeartRateManager by lazy {
        SdkHeartRateManager()
    }
    
    // Repository (singleton)
    val ringRepository: IRingRepository by lazy {
        RingRepositoryImpl.getInstance(context)
    }
    
    // Use Cases
    val scanDevicesUseCase: ScanDevicesUseCase by lazy {
        ScanDevicesUseCase(ringRepository)
    }
    
    val connectRingUseCase: ConnectRingUseCase by lazy {
        ConnectRingUseCase(ringRepository)
    }
    
    val disconnectRingUseCase: DisconnectRingUseCase by lazy {
        DisconnectRingUseCase(ringRepository)
    }
    
    val getRingDataUseCase: GetRingDataUseCase by lazy {
        GetRingDataUseCase(ringRepository)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null
        
        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
        
        /**
         * Initialize container (call from Application.onCreate)
         */
        fun initialize(context: Context) {
            getInstance(context)
        }
    }
}
