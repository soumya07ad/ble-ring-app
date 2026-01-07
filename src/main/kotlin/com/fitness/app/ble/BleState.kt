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
