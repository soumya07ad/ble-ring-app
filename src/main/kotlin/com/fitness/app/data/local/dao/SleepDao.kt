package com.fitness.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitness.app.data.local.entity.SleepEntry

@Dao
interface SleepDao {

    // Ensures we only have one entry per date using REPLACE if there's a conflict
    // Alternatively, we could query and update, but since we will index 'date' as unique, replace works.
    // However, if we don't index 'date' as unique, we can insert based on date logic manually or 
    // simply let Room replace if we supply the exact primary key.
    // The requirement says: @Insert(onConflict = OnConflictStrategy.REPLACE).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: SleepEntry)

    @Query("SELECT * FROM sleep_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): SleepEntry?

    // Gets the last N entries chronologically up to a specific date? 
    // The requirement says 'getLast7Days(fromDate: String)' and 'all queries must be date-based'.
    // We can order by date DESC and get 7, or get dates >= (fromDate - 7 days).
    @Query("SELECT * FROM sleep_entries WHERE date <= :fromDate ORDER BY date DESC LIMIT 7")
    suspend fun getLast7Days(fromDate: String): List<SleepEntry>

    @Query("SELECT * FROM sleep_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getEntriesBetween(startDate: String, endDate: String): List<SleepEntry>
}
