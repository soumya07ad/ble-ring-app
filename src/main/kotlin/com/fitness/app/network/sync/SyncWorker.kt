package com.fitness.app.network.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fitness.app.MainActivity
import java.util.concurrent.TimeUnit

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Sync steps
            syncSteps()
            // Sync calories
            syncCalories()
            // Sync heart rate
            syncHeartRate()
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Retry after some time
            Result.retry()
        }
    }
    
    private suspend fun syncSteps() {
        try {
            val stepData = MainActivity.fitnessAPI.getSteps()
            // Sync to backend
            MainActivity.fitnessRepository.logSteps(
                com.fitness.app.network.models.StepDataResponse(
                    steps = stepData.steps,
                    goal = stepData.goal,
                    progress = stepData.progress,
                    date = getTodayDate()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun syncCalories() {
        try {
            val calorieData = MainActivity.fitnessAPI.getCalories()
            MainActivity.fitnessRepository.logCalories(
                com.fitness.app.network.models.CalorieDataResponse(
                    calories = calorieData.calories,
                    goal = calorieData.goal,
                    progress = calorieData.progress,
                    date = getTodayDate()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun syncHeartRate() {
        try {
            val hrData = MainActivity.fitnessAPI.getHeartRate()
            MainActivity.fitnessRepository.logHeartRate(
                com.fitness.app.network.models.HeartRateResponse(
                    currentBPM = hrData.currentBPM,
                    averageBPM = hrData.averageBPM,
                    minBPM = hrData.minBPM,
                    maxBPM = hrData.maxBPM,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
    
    companion object {
        
        /**
         * Schedule background sync every 2 hours
         */
        fun scheduleSyncWorker(context: Context) {
            val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                2, // interval
                TimeUnit.HOURS
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "health_data_sync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        }
        
        /**
         * Cancel background sync
         */
        fun cancelSyncWorker(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("health_data_sync")
        }
    }
}
