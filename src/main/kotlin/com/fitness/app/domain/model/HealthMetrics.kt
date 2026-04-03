package com.fitness.app.domain.model

/**
 * Domain model for step metrics
 */
data class StepMetrics(
    val steps: Int = 0,
    val goal: Int = 10000,
    val progress: Float = 0f
)

/**
 * Domain model for calorie metrics
 */
data class CalorieMetrics(
    val calories: Int = 0,
    val goal: Int = 2000,
    val progress: Float = 0f
)

/**
 * Domain model for heart rate metrics
 */
data class HeartRateMetrics(
    val currentBPM: Int = 0,
    val averageBPM: Int = 0,
    val minBPM: Int = 0,
    val maxBPM: Int = 0
)

/**
 * Domain model for daily health summary
 */
data class DailyHealthSummary(
    val date: String = "",
    val steps: Int = 0,
    val calories: Int = 0,
    val activeMinutes: Int = 0,
    val heartRateAvg: Int = 0,
    val sleepHours: Float = 0f
)

/**
 * Domain model for workout information
 */
data class WorkoutInfo(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val duration: Int = 0,
    val caloriesBurned: Int = 0,
    val distance: Float = 0f,
    val intensity: String = "",
    val date: String = "",
    val notes: String = ""
)

/**
 * Domain model for a single day's fitness history entry
 */
data class FitnessHistoryEntry(
    val date: String,
    val steps: Int,
    val distance: Int, // in meters
    val calories: Int
)
