package com.fitness.app.domain.model

data class ActivityStreakData(
    val activityType: String,
    val currentStreak: Int,
    val longestStreak: Int
)

data class MilestoneData(
    val target: Int,
    val label: String,
    val current: Int,
    val progress: Float
)
