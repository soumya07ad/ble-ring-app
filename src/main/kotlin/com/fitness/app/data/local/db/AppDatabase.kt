package com.fitness.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fitness.app.data.local.dao.SleepDao
import com.fitness.app.data.local.dao.CoachDao
import com.fitness.app.data.local.dao.StreakDao
import com.fitness.app.data.local.entity.SleepEntry
import com.fitness.app.data.local.entity.CoachMessageEntity
import com.fitness.app.data.local.entity.StreakEntry

@Database(entities = [SleepEntry::class, CoachMessageEntity::class, StreakEntry::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun coachDao(): CoachDao
    abstract fun streakDao(): StreakDao
}
