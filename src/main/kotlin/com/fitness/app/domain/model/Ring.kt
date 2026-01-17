package com.fitness.app.domain.model

/**
 * Domain model representing a BLE Ring device
 * Pure Kotlin class with no Android dependencies
 */
data class Ring(
    val macAddress: String,
    val name: String,
    val rssi: Int = -100,
    val isConnected: Boolean = false
) {
    /**
     * Signal quality based on RSSI
     */
    val signalQuality: SignalQuality
        get() = when {
            rssi > -60 -> SignalQuality.EXCELLENT
            rssi > -70 -> SignalQuality.GOOD
            rssi > -80 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    
    /**
     * Check if this is a valid ring device
     */
    val isValidRing: Boolean
        get() = name.contains("R", ignoreCase = true) || 
                name.contains("Ring", ignoreCase = true)
}

/**
 * Signal quality levels for UI display
 */
enum class SignalQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}
