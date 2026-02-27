package com.fitness.app.domain.repository

import com.fitness.app.domain.model.ActivityStreakData
import com.fitness.app.domain.model.MilestoneData

interface IStreakRepository {
    suspend fun markActivityCompleted(activityType: String, date: String)
    suspend fun getAllActivityStreaks(): List<ActivityStreakData>
    suspend fun getLongestCurrentStreak(): Pair<String, Int>
    fun getMilestoneProgress(currentStreak: Int): List<MilestoneData>
}
