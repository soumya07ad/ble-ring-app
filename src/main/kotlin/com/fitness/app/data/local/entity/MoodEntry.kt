package com.fitness.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val emotion: String,
    val score: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String // ISO format: "yyyy-MM-dd"
) {
    fun getLocalDate(): LocalDate {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
