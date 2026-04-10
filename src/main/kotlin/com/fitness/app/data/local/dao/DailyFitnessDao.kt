package com.fitness.app.data.local.dao

import androidx.room.*
import com.fitness.app.data.local.entity.DailyFitnessRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyFitnessDao {
    @Query("SELECT * FROM daily_fitness_records WHERE userId = :userId ORDER BY date DESC LIMIT 30")
    fun getLast30Days(userId: String): Flow<List<DailyFitnessRecord>>

    @Query("SELECT * FROM daily_fitness_records WHERE userId = :userId AND date >= :fromDate ORDER BY date DESC")
    fun getLast7Days(userId: String, fromDate: String): Flow<List<DailyFitnessRecord>>

    @Query("SELECT * FROM daily_fitness_records WHERE userId = :userId ORDER BY date DESC LIMIT 30")
    suspend fun getLast30DaysSync(userId: String): List<DailyFitnessRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyFitnessRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DailyFitnessRecord>)
}
