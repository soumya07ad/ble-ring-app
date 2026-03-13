package com.fitness.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitness.app.data.local.entity.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(entry: MoodEntry)

    @Query("SELECT * FROM mood_entries WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getMoodsByDate(date: String): List<MoodEntry>

    @Query("SELECT * FROM mood_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, timestamp ASC")
    suspend fun getMoodsBetween(startDate: String, endDate: String): List<MoodEntry>

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllMoods(): Flow<List<MoodEntry>>
}
