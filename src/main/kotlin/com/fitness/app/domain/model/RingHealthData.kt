package com.fitness.app.domain.model

/**
 * Domain model representing health data from the ring
 * Pure Kotlin class with no Android dependencies
 */
data class RingHealthData(
    val battery: Int? = null,    // null = unknown, 0-100 = actual percentage
    val heartRate: Int = 0,
    val heartRateMeasuring: Boolean = false,  // true when measuring HR
    val bloodPressureSystolic: Int = 0,       // mmHg (high) - bphp
    val bloodPressureDiastolic: Int = 0,      // mmHg (low) - bplp
    val bloodPressureHeartRate: Int = 0,      // bpm during BP measurement - bphr
    val bloodPressureMeasuring: Boolean = false,
    val spO2: Float = 0f,           // blood oxygen % (Float: 99.5)
    val spO2Measuring: Boolean = false,
    val stress: Int = 0,          // stress level (0-100)
    val steps: Int = 0,
    val distance: Int = 0,       // meters
    val calories: Int = 0,       // kcal
    val deepSleep: Int = 0,      // minutes
    val lightSleep: Int = 0,     // minutes
    val lastUpdate: Long = 0L
) {
    /**
     * Total sleep in minutes
     */
    val totalSleepMinutes: Int get() = deepSleep + lightSleep
    
    /**
     * Formatted sleep duration string
     */
    val sleepFormatted: String get() {
        val hours = totalSleepMinutes / 60
        val mins = totalSleepMinutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }
    
    /**
     * Distance in kilometers
     */
    val distanceKm: Float get() = distance / 1000f
    
    /**
     * Check if battery data is available (not null)
     */
    val hasBatteryData: Boolean get() = battery != null
    
    /**
     * Check if heart rate data is available
     */
    val hasHeartRateData: Boolean get() = heartRate > 0
    
    /**
     * Check if step data is available
     */
    val hasStepData: Boolean get() = steps > 0
}
