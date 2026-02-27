package com.fitness.app.presentation.dashboard

import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.RingHealthData
import com.fitness.app.domain.model.ConnectionStatus
import com.fitness.app.domain.model.DailyHealthSummary

/**
 * UI State for the Dashboard screen.
 * 
 * Combines ring health data with local fitness data into a single
 * state object for the dashboard to observe.
 */
data class DashboardUiState(
    // Ring connection
    val isConnected: Boolean = false,
    val connectedRing: Ring? = null,
    val batteryLevel: Int? = null,

    // Ring health metrics
    val heartRate: Int = 0,
    val spO2: Float = 0f,
    val steps: Int = 0,
    val distance: Int = 0,
    val calories: Int = 0,
    val stressLevel: Int = 0,

    // Daily summary
    val dailySummary: DailyHealthSummary = DailyHealthSummary(),

    // Loading / error
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val hasHeartRate: Boolean get() = heartRate > 0
    val hasSpO2: Boolean get() = spO2 > 0
    val hasSteps: Boolean get() = steps > 0
}
