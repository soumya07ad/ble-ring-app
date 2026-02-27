package com.fitness.app.data.repository

import com.fitness.app.data.local.dao.SleepDao
import com.fitness.app.data.local.entity.SleepEntry
import com.fitness.app.domain.repository.SleepDayUiModel
import com.fitness.app.domain.repository.SleepRepository
import com.fitness.app.domain.repository.WeeklySleepStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SleepRepositoryImpl(private val sleepDao: SleepDao) : SleepRepository {

    override suspend fun logSleep(date: String, hours: Double) {
        val entry = SleepEntry(
            date = date,
            sleepHours = hours
        )
        sleepDao.insertOrUpdate(entry)
    }

    override suspend fun getWeeklySleepStats(fromDate: String): WeeklySleepStats {
        val endDate = LocalDate.parse(fromDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val startDate = endDate.minusDays(6) // 7 days inclusive: today and 6 past days

        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Raw DB entries
        val rawEntries = sleepDao.getEntriesBetween(startStr, endStr)
        val entryMap = rawEntries.associateBy { it.date }

        // Create the structured list filling missing days with 0h
        val last7Days = mutableListOf<SleepDayUiModel>()
        
        var total = 0.0
        var best = 0.0
        var worst = Double.MAX_VALUE
        var validNights = 0
        var consistencyCount = 0

        for (i in 0..6) {
            val currentDate = startDate.plusDays(i.toLong())
            val dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dayName = currentDate.dayOfWeek.name.take(3)
                .lowercase()
                .replaceFirstChar { it.uppercase() } // "Mon", "Tue"

            val entry = entryMap[dateStr]
            val hours = entry?.sleepHours ?: 0.0

            last7Days.add(
                SleepDayUiModel(
                    date = dateStr,
                    dayName = dayName,
                    hours = hours
                )
            )

            if (hours > 0) {
                total += hours
                if (hours > best) best = hours
                if (hours < worst) worst = hours
                validNights++
                if (hours in 7.0..9.0) consistencyCount++ // 7-9h count
            }
        }

        val average = if (validNights > 0) total / validNights else 0.0
        if (worst == Double.MAX_VALUE) worst = 0.0

        return WeeklySleepStats(
            last7Days = last7Days,
            average = average,
            bestNight = best,
            worstNight = worst,
            weeklyTotal = total,
            consistencyCount = consistencyCount
        )
    }
}
