package com.fitness.app.domain.model

/**
 * Represents the BLE scan status
 */
sealed class ScanStatus {
    /**
     * Not currently scanning
     */
    object Idle : ScanStatus()
    
    /**
     * Currently scanning for devices
     */
    object Scanning : ScanStatus()
    
    /**
     * Scan completed with results
     */
    data class DevicesFound(val devices: List<Ring>) : ScanStatus()
    
    /**
     * Scan failed with an error
     */
    data class Error(val message: String) : ScanStatus()
    
    /**
     * Check if currently scanning
     */
    val isScanning: Boolean get() = this is Scanning
    
    /**
     * Check if idle
     */
    val isIdle: Boolean get() = this is Idle
    
    /**
     * Get devices if available
     */
    fun getDevicesOrEmpty(): List<Ring> = (this as? DevicesFound)?.devices ?: emptyList()
}
