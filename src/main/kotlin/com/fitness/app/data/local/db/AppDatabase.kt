package com.fitness.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fitness.app.data.local.dao.SleepDao
import com.fitness.app.data.local.dao.CoachDao
import com.fitness.app.data.local.dao.StreakDao
import com.fitness.app.data.local.dao.MoodDao
import com.fitness.app.data.local.dao.JournalDao
import com.fitness.app.data.local.entity.SleepEntry
import com.fitness.app.data.local.entity.CoachMessageEntity
import com.fitness.app.data.local.entity.StreakEntry
import com.fitness.app.data.local.entity.MoodEntry
import com.fitness.app.data.local.entity.JournalEntry

@Database(entities = [SleepEntry::class, CoachMessageEntity::class, StreakEntry::class, MoodEntry::class, JournalEntry::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun coachDao(): CoachDao
    abstract fun streakDao(): StreakDao
    abstract fun moodDao(): MoodDao
    abstract fun journalDao(): JournalDao
}
