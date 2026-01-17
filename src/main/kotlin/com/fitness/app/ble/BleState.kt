package com.fitness.app.ble

/**
 * Represents runtime Bluetooth permission states
 */
sealed class PermissionState {
    object NotRequested : PermissionState()
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
}

/**
 * Represents BLE connection states
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
    object Timeout : ConnectionState()
}

/**
 * Represents BLE scan states
 */
sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class DevicesFound(val devices: List<BleDevice>) : ScanState()
    data class Error(val message: String) : ScanState()
}

/**
 * Immutable BLE device model for UI
 * @param deviceName Human-readable device name
 * @param deviceMac MAC address (unique identifier)
 * @param rssi Signal strength in dBm
 */
data class BleDevice(
    val deviceName: String,
    val deviceMac: String,
    val rssi: Int
) {
    val signalQuality: SignalQuality
        get() = when {
            rssi > -60 -> SignalQuality.EXCELLENT
            rssi > -70 -> SignalQuality.GOOD
            rssi > -80 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
}

enum class SignalQuality {
    EXCELLENT, GOOD, FAIR, POOR
}

/**
 * Ring health data received from SDK
 * All data is updated via real-time callbacks
 */
data class RingData(
    val deviceName: String = "Ring",
    val macAddress: String = "",
    val battery: Int? = null,    // null = unknown, 0-100 = actual percentage
    val heartRate: Int = 0,
    val steps: Int = 0,
    val distance: Int = 0,       // meters
    val calories: Int = 0,       // kcal
    val spO2: Int = 0,          // blood oxygen %
    val deepSleep: Int = 0,     // minutes
    val lightSleep: Int = 0,    // minutes
    val lastUpdate: Long = 0L
) {
    val totalSleepMinutes: Int get() = deepSleep + lightSleep
    
    /** Returns battery display string: "85%" or "Unknown" */
    val batteryDisplay: String get() = battery?.let { "$it%" } ?: "Unknown"
    
    /** Returns true if battery is known */
    val hasBattery: Boolean get() = battery != null
    
    val sleepFormatted: String get() {
        val hours = totalSleepMinutes / 60
        val mins = totalSleepMinutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }
    
    val distanceKm: Float get() = distance / 1000f
}
