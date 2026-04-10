package com.fitness.app.data.source

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fitness.app.data.local.entity.DailyFitnessRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun fetchDailyHistory(days: Int = 30): List<DailyFitnessRecord> {
        val client = healthConnectClient ?: return emptyList()
        val records = mutableListOf<DailyFitnessRecord>()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val endTime = LocalDate.now()

        for (i in 0 until days) {
            val date = endTime.minusDays(i.toLong())
            val startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val nextDayStartTime = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val aggregateRequest = AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    DistanceRecord.DISTANCE_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(startTime, nextDayStartTime)
            )

            try {
                val response = client.aggregate(aggregateRequest)
                val steps = response[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0
                val distance = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                val calories = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

                if (steps > 0 || distance > 0 || calories > 0) {
                    records.add(
                        DailyFitnessRecord(
                            date = date.format(formatter),
                            steps = steps,
                            distanceMeters = distance,
                            calories = calories
                        )
                    )
                }
            } catch (e: Exception) {
                // Log error or handle
            }
        }
        return records
    }
}
