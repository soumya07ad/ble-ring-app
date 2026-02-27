package com.fitness.app

/**
 * Data class for discovered Bluetooth device
 */
data class BluetoothDevice(
    val name: String,
    val address: String,
    val signalStrength: Int = -50 // dBm
)

/**
 * Data class for paired smart ring
 */
data class SmartRing(
    val name: String,
    val macAddress: String,
    val batteryLevel: Int? = null,  // null = unknown, 0-100 = actual percentage
    val isConnected: Boolean = false,
    val lastSync: String = "Just now"
)
