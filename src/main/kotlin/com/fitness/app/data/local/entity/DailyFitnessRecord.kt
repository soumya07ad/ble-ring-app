package com.fitness.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_fitness_records")
data class DailyFitnessRecord(
    @PrimaryKey
    val date: String, // format: "yyyy-MM-dd"
    val steps: Int,
    val distanceMeters: Double,
    val calories: Double,
    val userId: String = "default_user" // placeholder for multi-user support
)
