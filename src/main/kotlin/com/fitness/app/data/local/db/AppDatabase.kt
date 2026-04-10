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

import com.fitness.app.data.local.dao.DailyFitnessDao
import com.fitness.app.data.local.entity.DailyFitnessRecord

@Database(entities = [SleepEntry::class, CoachMessageEntity::class, StreakEntry::class, MoodEntry::class, JournalEntry::class, DailyFitnessRecord::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun coachDao(): CoachDao
    abstract fun streakDao(): StreakDao
    abstract fun moodDao(): MoodDao
    abstract fun journalDao(): JournalDao
    abstract fun dailyFitnessDao(): DailyFitnessDao
}
