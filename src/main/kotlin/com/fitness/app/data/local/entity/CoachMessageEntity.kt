package com.fitness.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "coach_messages",
    indices = [androidx.room.Index(value = ["sessionId"])]
)
data class CoachMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
