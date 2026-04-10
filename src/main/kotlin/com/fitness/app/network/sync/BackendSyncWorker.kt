package com.fitness.app.network.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitness.app.data.local.db.AppDatabase
import com.fitness.app.network.client.RetrofitClient
import com.fitness.app.network.models.HealthSyncRequest
import com.fitness.app.network.models.JournalSyncEntry
import com.fitness.app.network.models.JournalSyncRequest
import com.fitness.app.network.repository.FitnessRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BackendSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Fetch dependencies directly since we are in a worker and may not have Hilt/Dagger injected yet
            val roomDatabase = androidx.room.Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "fitness_app_database"
            ).build()
            
            val retrofitClient = RetrofitClient.getInstance(applicationContext)
            val fitnessRepository = FitnessRepository(retrofitClient.getApiService(), retrofitClient.getTokenManager())
            
            val dailyFitnessDao = roomDatabase.dailyFitnessDao()
            val journalDao = roomDatabase.journalDao()

            android.util.Log.i("FitnessApplication", "BackendSyncWorker started")
            val latestHealthRecord = dailyFitnessDao.getLast30DaysSync("default_user").firstOrNull()
            
            // Sync Health
            if (latestHealthRecord != null) {
                android.util.Log.i("FitnessApplication", "Syncing health for date: ${latestHealthRecord.date}")
                val metric = com.fitness.app.network.models.HealthMetric(
                    steps = latestHealthRecord.steps,
                    distance = latestHealthRecord.distanceMeters,
                    calories = latestHealthRecord.calories,
                    recordedAt = latestHealthRecord.date
                )
                val healthRequest = HealthSyncRequest(
                    metrics = listOf(metric)
                )
                val response = fitnessRepository.syncHealth(healthRequest)
                android.util.Log.i("FitnessApplication", "Health sync response: $response")
            } else {
                android.util.Log.i("FitnessApplication", "No historical health data to sync")
            }
            
            // Sync Journals
            val allJournals = journalDao.getAllEntriesSync()
            if (allJournals.isNotEmpty()) {
                android.util.Log.i("FitnessApplication", "Syncing ${allJournals.size} journal entries")
                val journalRequest = JournalSyncRequest(
                    entries = allJournals.map { 
                        JournalSyncEntry(
                            title = it.emotion, 
                            content = it.message ?: "", 
                            date = it.date
                        ) 
                    }
                )
                val response = fitnessRepository.syncJournal(journalRequest)
                android.util.Log.i("FitnessApplication", "Journal sync response: $response")
            } else {
                android.util.Log.i("FitnessApplication", "No journal entries to sync")
            }

            android.util.Log.i("FitnessApplication", "BackendSyncWorker finished successfully")
            Result.success()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
