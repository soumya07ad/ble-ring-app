package com.fitness.app.domain.repository

import com.fitness.app.core.util.Result
import com.fitness.app.domain.model.ConnectionStatus
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.RingHealthData
import com.fitness.app.domain.model.ScanStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for Ring operations
 * Defines the contract for data layer implementations
 */
interface IRingRepository {
    
    /**
     * Observe connection status changes
     */
    val connectionStatus: StateFlow<ConnectionStatus>
    
    /**
     * Observe scan status changes
     */
    val scanStatus: StateFlow<ScanStatus>
    
    /**
     * Observe ring health data updates
     */
    val ringData: StateFlow<RingHealthData>
    
    /**
     * Initialize BLE components
     */
    fun initialize()
    
    /**
     * Start scanning for nearby ring devices
     * @param durationSeconds How long to scan
     */
    suspend fun startScan(durationSeconds: Int = 6): Result<List<Ring>>
    
    /**
     * Stop current scan
     */
    fun stopScan()
    
    /**
     * Connect to a ring device by MAC address
     * @param macAddress BLE MAC address
     * @param deviceName Optional device name
     */
    suspend fun connect(macAddress: String, deviceName: String? = null): Result<Ring>
    
    /**
     * Disconnect from currently connected ring
     */
    suspend fun disconnect(): Result<Unit>
    
    /**
     * Get current battery level
     */
    suspend fun getBattery(): Result<Int>
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean
    
    /**
     * Get connected ring info or null
     */
    fun getConnectedRing(): Ring?
}
