package com.fitness.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streak_entries",
    indices = [
        Index(value = ["activityType", "date"], unique = true)
    ]
)
data class StreakEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val activityType: String,
    val date: String,  // yyyy-MM-dd
    val createdAt: Long = System.currentTimeMillis()
)
