package com.fitness.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "sleep_entries",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class SleepEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    // ISO format: "yyyy-MM-dd" e.g. "2024-05-12"
    // We enforce UNIQUE on the date directly or handle it in the DAO via queries
    // Since only one entry per date is allowed, we can make 'date' unique.
    val date: String,
    
    val sleepHours: Double,
    
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getLocalDate(): LocalDate {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
