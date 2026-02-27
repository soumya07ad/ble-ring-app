package com.fitness.app.data.repository

import com.fitness.app.data.local.dao.StreakDao
import com.fitness.app.data.local.entity.StreakEntry
import com.fitness.app.domain.model.ActivityStreakData
import com.fitness.app.domain.model.MilestoneData
import com.fitness.app.domain.repository.IStreakRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakRepository(private val streakDao: StreakDao) : IStreakRepository {

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    override suspend fun markActivityCompleted(activityType: String, date: String) {
        streakDao.insertOrUpdate(StreakEntry(activityType = activityType, date = date))
    }

    override suspend fun getAllActivityStreaks(): List<ActivityStreakData> {
        val activities = listOf("Running", "Water Intake", "Meditation", "Gym", "Sleep", "Mood")
        return activities.map { activity ->
            val entries = streakDao.getEntriesByActivity(activity)
            val dates = entries.map { LocalDate.parse(it.date, fmt) }.sorted()
            ActivityStreakData(
                activityType = activity,
                currentStreak = calculateCurrentStreak(dates),
                longestStreak = calculateLongestStreak(dates)
            )
        }
    }

    override suspend fun getLongestCurrentStreak(): Pair<String, Int> {
        val all = getAllActivityStreaks()
        val best = all.maxByOrNull { it.currentStreak }
        return Pair(best?.activityType ?: "", best?.currentStreak ?: 0)
    }

    override fun getMilestoneProgress(currentStreak: Int): List<MilestoneData> {
        val targets = listOf(
            7 to "7 Day Streak",
            30 to "30 Day Streak",
            100 to "100 Day Streak",
            365 to "365 Day Streak"
        )
        return targets.map { (target, label) ->
            MilestoneData(
                target = target,
                label = label,
                current = currentStreak.coerceAtMost(target),
                progress = (currentStreak.toFloat() / target).coerceIn(0f, 1f)
            )
        }
    }

    private fun calculateCurrentStreak(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        var streak = 0
        var checkDate = LocalDate.now()

        if (sortedDates.last() != checkDate) {
            checkDate = checkDate.minusDays(1)
            if (sortedDates.last() != checkDate) return 0
        }

        val dateSet = sortedDates.toHashSet()
        while (dateSet.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        return streak
    }

    private fun calculateLongestStreak(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        var maxStreak = 1
        var current = 1
        for (i in 1 until sortedDates.size) {
            if (sortedDates[i] == sortedDates[i - 1].plusDays(1)) {
                current++
                if (current > maxStreak) maxStreak = current
            } else if (sortedDates[i] != sortedDates[i - 1]) {
                current = 1
            }
        }
        return maxStreak
    }
}
