package com.fitness.app.domain.repository

import com.fitness.app.data.local.entity.SleepEntry

data class SleepDayUiModel(
    val date: String,      // ISO date or formatted like "Mon"
    val dayName: String,   // "Mon", "Tue"
    val hours: Double
)

data class WeeklySleepStats(
    val last7Days: List<SleepDayUiModel>,
    val average: Double,
    val bestNight: Double,
    val worstNight: Double,
    val weeklyTotal: Double,
    val consistencyCount: Int
)

interface SleepRepository {
    suspend fun logSleep(date: String, hours: Double)
    suspend fun getWeeklySleepStats(fromDate: String): WeeklySleepStats
}
