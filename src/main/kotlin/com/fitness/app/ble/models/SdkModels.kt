/**
 * YCBT SDK Model Classes
 * 
 * These Kotlin data classes are based on the Java models from the official
 * YCBleSdkDemo app. Use these with Gson to parse SDK responses.
 * 
 * Source: android-sdk-master2/android-sdk-master/demo/YCBleSdkDemo/app/src/main/java/com/example/ycblesdkdemo/model/
 */

package com.fitness.app.ble.models

import com.google.gson.Gson

/**
 * Model for YCBTClient.getDeviceInfo() response
 * 
 * Usage:
 * ```kotlin
 * YCBTClient.getDeviceInfo(object : BleDataResponse {
 *     override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
 *         if (code == 0 && resultMap != null) {
 *             val gson = Gson()
 *             val info = gson.fromJson(resultMap.toString(), BandBaseInfo::class.java)
 *             val battery = info.data?.deviceBatteryValue?.toIntOrNull() ?: 0
 *             val version = info.data?.deviceVersion ?: ""
 *         }
 *     }
 * })
 * ```
 */
data class BandBaseInfo(
    val dataType: Int = 0,
    val code: Int = 0,
    val data: BandBaseInfoModel? = null
) {
    data class BandBaseInfoModel(
        val deviceBatteryValue: String? = null,  // Battery as STRING (e.g., "85")
        val deviceVersion: String? = null,        // Firmware version
        val deviceId: String? = null,             // Device ID
        val deviceBatteryState: String? = null    // Battery state (charging, etc.)
    )
    
    /**
     * Get battery as integer (0 if parsing fails)
     */
    fun getBatteryPercentage(): Int {
        return data?.deviceBatteryValue?.toIntOrNull() ?: 0
    }
}

/**
 * Model for ECG/PPG history list response
 * Used with YCBTClient.collectEcgList() and YCBTClient.collectPpgList()
 */
data class HistEcgResponse(
    val dataType: Int = 0,
    val code: Int = 0,
    val data: List<HisEcgModel> = emptyList()
) {
    data class HisEcgModel(
        val collectSendTime: Long = 0  // Timestamp of collection
    )
}

/**
 * Model for health history data response
 * Used with YCBTClient.healthHistoryData()
 */
data class HealthHistoryResponse(
    val dataType: Int = 0,
    val code: Int = 0,
    val data: List<HashMap<String, Any>> = emptyList()
)

/**
 * Connection event for state management
 * Based on demo's EventBus ConnectEvent
 */
data class ConnectionEvent(
    val state: Int = 0  // 0 = disconnected, 1 = connected and ready
)

/**
 * Real-time sport data
 * Received via YCBTClient.appRegisterRealDataCallBack()
 * when dataType == Constants.DATATYPE.Real_UploadSport
 */
data class RealTimeSportData(
    val sportStep: Int = 0,       // Steps count
    val sportDistance: Int = 0,    // Distance in meters
    val sportCalorie: Int = 0      // Calories burned
) {
    companion object {
        fun fromHashMap(map: HashMap<*, *>): RealTimeSportData {
            return RealTimeSportData(
                sportStep = (map["sportStep"] as? Int) ?: 0,
                sportDistance = (map["sportDistance"] as? Int) ?: 0,
                sportCalorie = (map["sportCalorie"] as? Int) ?: 0
            )
        }
    }
}

/**
 * Heart rate history data
 */
data class HeartRateHistoryItem(
    val heartStartTime: Long = 0,  // Timestamp
    val heartValue: Int = 0         // Heart rate BPM
)

/**
 * Sleep history data
 */
data class SleepHistoryItem(
    val startTime: Long = 0,
    val endTime: Long = 0,
    val deepSleep: Int = 0,    // Deep sleep in minutes
    val lightSleep: Int = 0    // Light sleep in minutes
)

/**
 * Blood oxygen data
 */
data class BloodOxygenItem(
    val startTime: Long = 0,
    val value: Int = 0  // SpO2 percentage
)

/**
 * Temperature data
 */
data class TemperatureItem(
    val startTime: Long = 0,
    val tempIntValue: Int = 0,    // Integer part
    val tempFloatValue: Int = 0   // Decimal part (e.g., 36.5 = int=36, float=5)
) {
    fun getTemperature(): Float {
        return "$tempIntValue.$tempFloatValue".toFloatOrNull() ?: 0f
    }
}
