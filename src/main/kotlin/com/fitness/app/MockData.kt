package com.fitness.app

import com.fitness.app.Workout
import com.fitness.app.domain.model.ConnectionStatus
import com.fitness.app.domain.model.FirmwareInfo
import com.fitness.app.domain.model.Ring
import com.fitness.app.domain.model.RingHealthData
import com.fitness.app.domain.model.ScanStatus
import com.fitness.app.domain.model.SleepData
import com.fitness.app.presentation.ring.PermissionUiState
import com.fitness.app.presentation.ring.RingUiState

object MockData {
    const val steps = 5002
    const val calories = 1000
    const val workoutMinutes = 100
    const val heartRate = 80
    const val sleepHours = 11

    val dailyWorkouts = listOf(
        Workout(
            id = "1",
            name = "Morning Yoga",
            type = "yoga",
            duration = 30,
            caloriesBurned = 150,
            distance = 0f,
            intensity = "low",
            date = "2025-11-17"
        ),
        Workout(
            id = "2",
            name = "Full Body HIIT",
            type = "strength",
            duration = 45,
            caloriesBurned = 350,
            distance = 0f,
            intensity = "high",
            date = "2025-11-17"
        ),
        Workout(
            id = "3",
            name = "Evening Walk",
            type = "walking",
            duration = 20,
            caloriesBurned = 100,
            distance = 2f,
            intensity = "low",
            date = "2025-11-17"
        )
    )

    val meditationSessions = listOf(
        "Morning Calm",
        "Breathing Exercise",
        "Sleep Meditation"
    )

    val wellnessRingStatus = "Connected"
    const val wellnessRingBattery = 85
}

// ═══════════════════════════════════════════════════════════════════════
// PREVIEW DATA — ViewModel-free state factories for @Preview composables
// ═══════════════════════════════════════════════════════════════════════

object PreviewData {

    // ── Mock domain objects ──────────────────────────────────────────

    val mockRing = Ring(
        macAddress = "AA:BB:CC:DD:EE:FF",
        name = "Smart Ring R9",
        rssi = -55,
        isConnected = true
    )

    val mockDevices = listOf(
        Ring(macAddress = "AA:BB:CC:DD:EE:FF", name = "Smart Ring R9", rssi = -45, isConnected = false),
        Ring(macAddress = "11:22:33:44:55:66", name = "Ring Pro", rssi = -65, isConnected = false),
        Ring(macAddress = "77:88:99:AA:BB:CC", name = "Health Ring X1", rssi = -82, isConnected = false)
    )

    val mockHealthData = RingHealthData(
        battery = 62,
        isCharging = false,
        heartRate = 72,
        spO2 = 98f,
        stress = 35,
        steps = 8432,
        distance = 5200,
        calories = 520,
        sleepData = SleepData(totalMinutes = 450, deepMinutes = 120, lightMinutes = 280, awakeMinutes = 50, quality = 85),
        firmwareInfo = FirmwareInfo(type = "MRD", version = "2.1.4")
    )

    // ── Full UI states ──────────────────────────────────────────────

    val connectedState = RingUiState(
        permissionState = PermissionUiState.Granted,
        connectionStatus = ConnectionStatus.Connected(mockRing),
        connectedRing = mockRing,
        ringData = mockHealthData
    )

    val disconnectedState = RingUiState(
        permissionState = PermissionUiState.Granted,
        connectionStatus = ConnectionStatus.Disconnected,
        connectedRing = null,
        ringData = RingHealthData()
    )

    val scanningState = RingUiState(
        permissionState = PermissionUiState.Granted,
        scanStatus = ScanStatus.Scanning,
        scannedDevices = mockDevices
    )

    val connectingState = RingUiState(
        permissionState = PermissionUiState.Granted,
        connectionStatus = ConnectionStatus.Connecting
    )

    val loadingState = RingUiState(
        permissionState = PermissionUiState.Granted,
        isLoading = true
    )

    val highStressState = RingUiState(
        permissionState = PermissionUiState.Granted,
        connectionStatus = ConnectionStatus.Connected(mockRing),
        connectedRing = mockRing,
        ringData = mockHealthData.copy(stress = 85, heartRate = 95)
    )

    val lowBatteryState = RingUiState(
        permissionState = PermissionUiState.Granted,
        connectionStatus = ConnectionStatus.Connected(mockRing),
        connectedRing = mockRing,
        ringData = mockHealthData.copy(battery = 12)
    )

    val permissionsNeededState = RingUiState(
        permissionState = PermissionUiState.NotRequested
    )
}
