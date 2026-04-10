package com.fitness.app.data.repository

import com.fitness.app.data.local.dao.DailyFitnessDao
import com.fitness.app.data.local.entity.DailyFitnessRecord
import com.fitness.app.data.source.HealthConnectManager
import com.fitness.app.data.source.PhoneStepDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FitnessHistoryRepository(
    private val healthConnectManager: HealthConnectManager,
    private val dailyFitnessDao: DailyFitnessDao,
    private val phoneStepDataSource: PhoneStepDataSource,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        // Start sync when repository is created
        syncWithHealthConnect()
        
        // Listen to phone sensor and update Room for today
        scope.launch {
            phoneStepDataSource.steps.collect { steps ->
                val today = LocalDate.now().format(formatter)
                val distance = steps * 0.762
                val calories = steps * 0.04
                
                dailyFitnessDao.insertOrUpdate(
                    DailyFitnessRecord(
                        date = today,
                        steps = steps,
                        distanceMeters = distance,
                        calories = calories
                    )
                )
            }
        }
    }

    fun getHistoryFlow(userId: String = "default_user"): Flow<List<DailyFitnessRecord>> {
        return dailyFitnessDao.getLast30Days(userId)
    }

    fun syncWithHealthConnect() {
        scope.launch {
            if (healthConnectManager.isAvailable() && healthConnectManager.hasPermissions()) {
                val historicalData = healthConnectManager.fetchDailyHistory(30)
                if (historicalData.isNotEmpty()) {
                    dailyFitnessDao.insertAll(historicalData)
                }
            }
        }
    }
}
