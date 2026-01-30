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
 * Represents SDK BLE connection states
 * Carries the Ring object when connected
 */
sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Connecting : BleConnectionState()
    data class Connected(val ring: com.fitness.app.domain.model.Ring) : BleConnectionState()
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
    val heartRateMeasuring: Boolean = false,  // true when HR measurement in progress
    val bloodPressureSystolic: Int = 0,       // mmHg (high) - bphp
    val bloodPressureDiastolic: Int = 0,      // mmHg (low) - bplp
    val bloodPressureHeartRate: Int = 0,      // bpm during BP measurement - bphr
    val bloodPressureMeasuring: Boolean = false,
    val spO2: Float = 0f,          // blood oxygen % (Float: 99.5)
    val spO2Measuring: Boolean = false,
    val stress: Int = 0,         // stress level (0-100)
    val steps: Int = 0,
    val distance: Int = 0,       // meters
    val calories: Int = 0,       // kcal
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

/**
 * Measurement timer state for timed vital sign measurements
 */
data class MeasurementTimer(
    val isActive: Boolean = false,
    val measurementType: MeasurementType = MeasurementType.NONE,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 30
) {
    /**
     * Progress as percentage (0.0 to 1.0)
     */
    val progressPercent: Float get() = if (totalSeconds > 0) {
        (totalSeconds - remainingSeconds) / totalSeconds.toFloat()
    } else 0f
}

/**
 * Types of measurements that can be timed
 */
enum class MeasurementType {
    NONE,
    HEART_RATE,
    BLOOD_PRESSURE,
    SPO2,
    STRESS;
    
    /**
     * Display name for UI
     */
    val displayName: String
        get() = when (this) {
            HEART_RATE -> "Heart Rate"
            BLOOD_PRESSURE -> "Blood Pressure"
            SPO2 -> "Blood Oxygen (SpO2)"
            STRESS -> "Stress Level"
            NONE -> ""
        }
}
