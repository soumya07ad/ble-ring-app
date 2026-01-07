package com.fitness.app

import com.fitness.app.Workout

object MockData {
    const val steps = 5002
    const val calories = 1000
    const val workoutMinutes = 100
    const val heartRate = 80
    const val sleepHours = 11

    val dailyWorkouts = listOf(
        Workout(
            id = "1",
            name = "Morning Yoga",
            type = "yoga",
            duration = 30,
            caloriesBurned = 150,
            distance = 0f,
            intensity = "low",
            date = "2025-11-17"
        ),
        Workout(
            id = "2",
            name = "Full Body HIIT",
            type = "strength",
            duration = 45,
            caloriesBurned = 350,
            distance = 0f,
            intensity = "high",
            date = "2025-11-17"
        ),
        Workout(
            id = "3",
            name = "Evening Walk",
            type = "walking",
            duration = 20,
            caloriesBurned = 100,
            distance = 2f,
            intensity = "low",
            date = "2025-11-17"
        )
    )

    val meditationSessions = listOf(
        "Morning Calm",
        "Breathing Exercise",
        "Sleep Meditation"
    )

    val wellnessRingStatus = "Connected"
    const val wellnessRingBattery = 85
}
