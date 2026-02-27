package com.fitness.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitness.app.data.local.entity.StreakEntry

@Dao
interface StreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: StreakEntry)

    @Query("SELECT * FROM streak_entries WHERE activityType = :activityType ORDER BY date ASC")
    suspend fun getEntriesByActivity(activityType: String): List<StreakEntry>

    @Query("SELECT * FROM streak_entries ORDER BY date ASC")
    suspend fun getAllEntries(): List<StreakEntry>

    @Query("SELECT * FROM streak_entries WHERE activityType = :activityType AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getEntriesForActivityBetween(activityType: String, startDate: String, endDate: String): List<StreakEntry>

    @Query("SELECT DISTINCT activityType FROM streak_entries")
    suspend fun getTrackedActivities(): List<String>
}
