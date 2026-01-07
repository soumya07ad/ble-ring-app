package com.fitness.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlin.math.pow
import kotlin.math.sqrt
import com.fitness.app.MockData

// Data classes for API responses
data class StepData(
    val steps: Int,
    val goal: Int,
    val progress: Float,
    val timestamp: Long
)

data class CalorieData(
    val calories: Int,
    val goal: Int,
    val progress: Float,
    val timestamp: Long
)

data class HeartRateData(
    val currentBPM: Int,
    val averageBPM: Int,
    val minBPM: Int,
    val maxBPM: Int,
    val timestamp: Long
)

data class Timer(
    val id: String,
    val name: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val type: String // "workout", "rest", "custom"
)

data class DailySummary(
    val date: String,
    val totalSteps: Int,
    val totalCalories: Int,
    val averageHeartRate: Int,
    val activeTime: Int, // in minutes
    val waterIntake: Int, // in cups
    val sleepDuration: Int // in minutes
)

data class Workout(
    val id: String,
    val name: String,
    val type: String, // "running", "cycling", "yoga", "strength", etc.
    val duration: Int, // in minutes
    val caloriesBurned: Int,
    val distance: Float, // in km
    val intensity: String, // "low", "medium", "high"
    val date: String,
    val notes: String = ""
)

data class UserPreferences(
    val theme: String = "light",
    val units: String = "metric", // "metric" or "imperial"
    val dailyStepGoal: Int = 10000,
    val dailyCalorieGoal: Int = 750,
    val notificationsEnabled: Boolean = true,
    val syncInterval: Int = 15 // minutes
)

// Main Fitness API class
class FitnessAPI(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("fitness_app", Context.MODE_PRIVATE)
    
    // Step Counting API
    private val _stepData = mutableStateOf(StepData(MockData.steps, 10000, (MockData.steps.toFloat()/10000), System.currentTimeMillis()))
    val stepData: State<StepData> = _stepData
    
    fun getSteps(): StepData {
        val steps = MockData.steps
        val goal = 10000
        val progress = steps.toFloat() / goal
        return StepData(steps, goal, progress, System.currentTimeMillis()).also {
            _stepData.value = it
        }
    }
    
    fun setSteps(steps: Int) {
        val goal = sharedPreferences.getInt("step_goal", 10000)
        val progress = steps.toFloat() / goal
        _stepData.value = StepData(steps, goal, progress, System.currentTimeMillis())
        sharedPreferences.edit().putInt("daily_steps", steps).apply()
    }
    
    fun incrementSteps(count: Int = 1) {
        val current = sharedPreferences.getInt("daily_steps", 0)
        setSteps(current + count)
    }
    
    // Calorie Calculation API
    private val _calorieData = mutableStateOf(CalorieData(MockData.calories, 750, (MockData.calories.toFloat()/750), System.currentTimeMillis()))
    val calorieData: State<CalorieData> = _calorieData
    
    fun getCalories(): CalorieData {
        val calories = MockData.calories
        val goal = 750
        val progress = calories.toFloat() / goal
        return CalorieData(calories, goal, progress, System.currentTimeMillis()).also {
            _calorieData.value = it
        }
    }
    
    fun setCalories(calories: Int) {
        val goal = sharedPreferences.getInt("calorie_goal", 750)
        val progress = calories.toFloat() / goal
        _calorieData.value = CalorieData(calories, goal, progress, System.currentTimeMillis())
        sharedPreferences.edit().putInt("daily_calories", calories).apply()
    }
    
    fun addCalories(calories: Int) {
        val current = sharedPreferences.getInt("daily_calories", 0)
        setCalories(current + calories)
    }
    
    fun calculateCaloriesBurned(
        activityType: String,
        durationMinutes: Int,
        weight: Int = 75 // kg
    ): Int {
        // MET (Metabolic Equivalent) based formula
        val met = when (activityType.lowercase()) {
            "walking" -> 3.5
            "jogging" -> 8.0
            "running" -> 12.0
            "cycling" -> 8.0
            "swimming" -> 10.0
            "yoga" -> 3.0
            "strength" -> 6.0
            else -> 5.0
        }
        return ((met * weight * durationMinutes) / 60).toInt()
    }
    
    // Heart Rate API
    private val _heartRateData = mutableStateOf(
        HeartRateData(MockData.heartRate, MockData.heartRate, MockData.heartRate - 10, MockData.heartRate + 15, System.currentTimeMillis())
    )
    val heartRateData: State<HeartRateData> = _heartRateData
    
    fun getHeartRate(): HeartRateData {
        val currentBPM = MockData.heartRate
        val avgBPM = MockData.heartRate
        val minBPM = MockData.heartRate - 10
        val maxBPM = MockData.heartRate + 15
        return HeartRateData(currentBPM, avgBPM, minBPM, maxBPM, System.currentTimeMillis()).also {
            _heartRateData.value = it
        }
    }
    
    fun setHeartRate(currentBPM: Int) {
        val avgBPM = sharedPreferences.getInt("average_heart_rate", MockData.heartRate)
        val minBPM = sharedPreferences.getInt("min_heart_rate", MockData.heartRate - 10)
        val maxBPM = sharedPreferences.getInt("max_heart_rate", MockData.heartRate + 15)
        
        val newMin = minOf(minBPM, currentBPM)
        val newMax = maxOf(maxBPM, currentBPM)
        val newAvg = ((avgBPM + currentBPM) / 2)
        
        _heartRateData.value = HeartRateData(currentBPM, newAvg, newMin, newMax, System.currentTimeMillis())
        sharedPreferences.edit().apply {
            putInt("current_heart_rate", currentBPM)
            putInt("average_heart_rate", newAvg)
            putInt("min_heart_rate", newMin)
            putInt("max_heart_rate", newMax)
        }.apply()
    }
    
    // Timer Management API
    private val _timers = mutableStateOf<List<Timer>>(emptyList())
    val timers: State<List<Timer>> = _timers
    
    fun createTimer(name: String, totalSeconds: Int, type: String = "custom"): Timer {
        val timer = Timer(
            id = System.currentTimeMillis().toString(),
            name = name,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            isRunning = false,
            type = type
        )
        _timers.value = _timers.value + timer
        return timer
    }
    
    fun startTimer(timerId: String) {
        _timers.value = _timers.value.map { timer ->
            if (timer.id == timerId) timer.copy(isRunning = true) else timer
        }
    }
    
    fun pauseTimer(timerId: String) {
        _timers.value = _timers.value.map { timer ->
            if (timer.id == timerId) timer.copy(isRunning = false) else timer
        }
    }
    
    fun updateTimer(timerId: String, remainingSeconds: Int) {
        _timers.value = _timers.value.map { timer ->
            if (timer.id == timerId) timer.copy(remainingSeconds = remainingSeconds) else timer
        }
    }
    
    fun deleteTimer(timerId: String) {
        _timers.value = _timers.value.filter { it.id != timerId }
    }
    
    fun getTimers(): List<Timer> = _timers.value
    
    // Daily Summary API (Local)
    fun getDailySummary(date: String = getTodayDate()): DailySummary {
        val steps = MockData.steps
        val calories = MockData.calories
        val avgHeartRate = MockData.heartRate
        val activeTime = MockData.workoutMinutes
        val waterIntake = 6
        val sleepDuration = (MockData.sleepHours * 60).toInt()
        
        return DailySummary(date, steps, calories, avgHeartRate, activeTime, waterIntake, sleepDuration)
    }
    
    fun saveDailySummary(summary: DailySummary) {
        sharedPreferences.edit().apply {
            putInt("daily_steps", summary.totalSteps)
            putInt("daily_calories", summary.totalCalories)
            putInt("average_heart_rate", summary.averageHeartRate)
            putInt("active_time_${summary.date}", summary.activeTime)
            putInt("water_intake_${summary.date}", summary.waterIntake)
            putInt("sleep_duration_${summary.date}", summary.sleepDuration)
        }.apply()
    }
    
    fun updateDailySummaryField(date: String, field: String, value: Int) {
        sharedPreferences.edit().putInt("${field}_${date}", value).apply()
    }
    
    // Offline Workouts API
    private val _workouts = mutableStateOf<List<Workout>>(MockData.dailyWorkouts)
    val workouts: State<List<Workout>> = _workouts
    
    fun addWorkout(
        name: String,
        type: String,
        duration: Int,
        caloriesBurned: Int,
        distance: Float,
        intensity: String,
        notes: String = ""
    ): Workout {
        val workout = Workout(
            id = System.currentTimeMillis().toString(),
            name = name,
            type = type,
            duration = duration,
            caloriesBurned = caloriesBurned,
            distance = distance,
            intensity = intensity,
            date = getTodayDate(),
            notes = notes
        )
        _workouts.value = _workouts.value + workout
        addCalories(caloriesBurned)
        return workout
    }
    
    fun getWorkouts(): List<Workout> = _workouts.value
    
    fun getWorkoutsByDate(date: String): List<Workout> {
        return _workouts.value.filter { it.date == date }
    }
    
    fun deleteWorkout(workoutId: String) {
        _workouts.value = _workouts.value.filter { it.id != workoutId }
    }
    
    fun getWorkoutStats(): Map<String, Any> {
        val allWorkouts = _workouts.value
        return mapOf(
            "totalWorkouts" to allWorkouts.size,
            "totalCalories" to allWorkouts.sumOf { it.caloriesBurned },
            "totalDistance" to allWorkouts.map { it.distance }.sum(),
            "totalDuration" to allWorkouts.sumOf { it.duration },
            "favoriteType" to (allWorkouts.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key ?: "N/A")
        )
    }
    
    // Local Workout Progress API
    fun getWorkoutProgress(type: String): Map<String, Any> {
        val typeWorkouts = _workouts.value.filter { it.type == type }
        return mapOf(
            "count" to typeWorkouts.size,
            "totalTime" to typeWorkouts.sumOf { it.duration },
            "totalCalories" to typeWorkouts.sumOf { it.caloriesBurned },
            "averageIntensity" to getAverageIntensity(typeWorkouts),
            "lastWorkout" to (typeWorkouts.maxByOrNull { it.date }?.date ?: "Never")
        )
    }
    
    fun getPersonalRecords(): Map<String, Any> {
        val allWorkouts = _workouts.value
        return mapOf(
            "longestWorkout" to (allWorkouts.maxByOrNull { it.duration }?.duration ?: 0),
            "maxCaloriesBurned" to (allWorkouts.maxByOrNull { it.caloriesBurned }?.caloriesBurned ?: 0),
            "longestDistance" to (allWorkouts.maxByOrNull { it.distance }?.distance ?: 0f),
            "mostIntenseWorkout" to (allWorkouts.filter { it.intensity == "high" }.size)
        )
    }
    
    // Local Preferences API
    private val _preferences = mutableStateOf(getUserPreferences())
    val preferences: State<UserPreferences> = _preferences
    
    fun getUserPreferences(): UserPreferences {
        return UserPreferences(
            theme = sharedPreferences.getString("theme", "light") ?: "light",
            units = sharedPreferences.getString("units", "metric") ?: "metric",
            dailyStepGoal = sharedPreferences.getInt("step_goal", 10000),
            dailyCalorieGoal = sharedPreferences.getInt("calorie_goal", 750),
            notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true),
            syncInterval = sharedPreferences.getInt("sync_interval", 15)
        )
    }
    
    fun updatePreferences(preferences: UserPreferences) {
        sharedPreferences.edit().apply {
            putString("theme", preferences.theme)
            putString("units", preferences.units)
            putInt("step_goal", preferences.dailyStepGoal)
            putInt("calorie_goal", preferences.dailyCalorieGoal)
            putBoolean("notifications_enabled", preferences.notificationsEnabled)
            putInt("sync_interval", preferences.syncInterval)
        }.apply()
        _preferences.value = preferences
    }
    
    fun setTheme(theme: String) {
        val current = _preferences.value
        updatePreferences(current.copy(theme = theme))
    }
    
    fun setUnits(units: String) {
        val current = _preferences.value
        updatePreferences(current.copy(units = units))
    }
    
    fun setDailyStepGoal(goal: Int) {
        val current = _preferences.value
        updatePreferences(current.copy(dailyStepGoal = goal))
        sharedPreferences.edit().putInt("step_goal", goal).apply()
    }
    
    fun setDailyCalorieGoal(goal: Int) {
        val current = _preferences.value
        updatePreferences(current.copy(dailyCalorieGoal = goal))
        sharedPreferences.edit().putInt("calorie_goal", goal).apply()
    }
    
    // Helper functions
    private fun getTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
    
    private fun getAverageIntensity(workouts: List<Workout>): String {
        if (workouts.isEmpty()) return "N/A"
        val highCount = workouts.count { it.intensity == "high" }
        val mediumCount = workouts.count { it.intensity == "medium" }
        
        return when {
            highCount > workouts.size / 2 -> "High"
            mediumCount > 0 -> "Medium"
            else -> "Low"
        }
    }
    
    // Meditation & Mindfulness API
    private val meditationTimer = MeditationTimer(context)
    
    val meditations: List<MeditationSession>
        get() = meditationTimer.meditations
    
    fun getMeditationSession(): ActiveMeditation? {
        return meditationTimer.activeMeditation.value
    }
    
    fun startMeditation(sessionId: String) {
        val session = meditationTimer.getMeditationById(sessionId)
        if (session != null) {
            meditationTimer.startMeditation(session)
        }
    }
    
    fun pauseMeditation() {
        meditationTimer.pauseMeditation()
    }
    
    fun resumeMeditation() {
        meditationTimer.resumeMeditation()
    }
    
    fun stopMeditation() {
        meditationTimer.stopMeditation()
    }
    
    fun formatMeditationTime(seconds: Int): String {
        return meditationTimer.formatTime(seconds)
    }
    
    // Set meditation callbacks for UI updates
    fun setMeditationCallbacks(
        onTimeUpdate: (ActiveMeditation) -> Unit,
        onSessionComplete: (MeditationSession, Int) -> Unit,
        onSessionStop: () -> Unit
    ) {
        meditationTimer.onTimeUpdate = onTimeUpdate
        meditationTimer.onSessionComplete = onSessionComplete
        meditationTimer.onSessionStop = onSessionStop
    }
    
    // Get meditation statistics
    fun getTotalMeditationMinutes(): Int {
        return meditationTimer.totalMeditationMinutes.value
    }
    
    fun getMeditationSessionsCompleted(): Int {
        return meditationTimer.sessionsCompleted.value
    }
}
