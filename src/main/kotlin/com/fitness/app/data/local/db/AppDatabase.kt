package com.fitness.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fitness.app.data.local.dao.SleepDao
import com.fitness.app.data.local.entity.SleepEntry

@Database(entities = [SleepEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
}
