package com.fitness.app.data.repository

import com.fitness.app.data.local.dao.MoodDao
import com.fitness.app.data.local.entity.MoodEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class MoodDayAggregate(
    val date: String,
    val dateLabel: String,
    val avgScore: Float,
    val entries: List<MoodEntry>
)

class MoodRepository(private val dao: MoodDao) {

    suspend fun insertMood(emotion: String, score: Int) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val entry = MoodEntry(emotion = emotion, score = score, date = today)
        dao.insertMood(entry)
    }

    suspend fun getDailyAverage(date: LocalDate): Float {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val moods = dao.getMoodsByDate(dateString)
        if (moods.isEmpty()) return 0f
        return moods.map { it.score }.average().toFloat()
    }

    suspend fun getWeeklyAverage(): Float {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)
        return getAverageScoreBetween(startDate, endDate)
    }

    suspend fun getMonthlyAverage(): Float {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(29)
        return getAverageScoreBetween(startDate, endDate)
    }

    private suspend fun getAverageScoreBetween(start: LocalDate, end: LocalDate): Float {
        val startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val moods = dao.getMoodsBetween(startStr, endStr)
        if (moods.isEmpty()) return 0f
        return moods.map { it.score }.average().toFloat()
    }

    suspend fun getChartData(days: Int): List<MoodDayAggregate> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays((days - 1).toLong())
        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val moods = dao.getMoodsBetween(startStr, endStr)
        val grouped = moods.groupBy { it.date }

        val formatter = DateTimeFormatter.ofPattern("E") // Short day name, e.g., Mon
        
        return (0 until days).map { i ->
            val date = startDate.plusDays(i.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dayMoods = grouped[dateStr] ?: emptyList()
            val avg = if (dayMoods.isNotEmpty()) dayMoods.map { it.score }.average().toFloat() else 0f
            MoodDayAggregate(
                date = dateStr,
                dateLabel = date.format(formatter),
                avgScore = avg,
                entries = dayMoods
            )
        }
    }

    suspend fun getBestAndWorstDays(days: Int): Pair<MoodDayAggregate?, MoodDayAggregate?> {
        val data = getChartData(days).filter { it.entries.isNotEmpty() }
        if (data.isEmpty()) return Pair(null, null)
        
        val best = data.maxByOrNull { it.avgScore }
        val worst = data.minByOrNull { it.avgScore }
        return Pair(best, worst)
    }
}
