package com.fitness.app

import android.content.Context
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fitness.app.network.client.RetrofitClient
import com.fitness.app.network.repository.FitnessRepository
import com.fitness.app.MockData
import com.fitness.app.network.sync.SyncWorker
import com.fitness.app.ui.theme.FitnessAppTheme

class MainActivity : ComponentActivity() {
    
    companion object {
        lateinit var fitnessAPI: FitnessAPI
        lateinit var fitnessRepository: FitnessRepository
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Clear SharedPreferences to always use MockData
        val sharedPreferences = getSharedPreferences("fitness_app", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        
        // Initialize FitnessAPI (local)
        fitnessAPI = FitnessAPI(this)
        
        // Initialize Network layer
        val retrofitClient = RetrofitClient.getInstance(this)
        fitnessRepository = FitnessRepository(
            retrofitClient.getApiService(),
            retrofitClient.getTokenManager()
        )
        
        // Start background sync
        SyncWorker.scheduleSyncWorker(this)
        
        setContent {
            FitnessAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationFlow()
                }
            }
        }
    }
}

@Composable
fun AppNavigationFlow() {
    val userLoggedIn by AppState.userLoggedIn
    val setupComplete by AppState.setupComplete

    when {
        !userLoggedIn -> {
            // Show WebView with login flow
            FitnessWebView(onLoginSuccess = {
                AppState.setUserLoggedIn(true)
                // Reset setup when user logs in (so they see setup screen)
                AppState.setSetupComplete(false)
            })
        }
        !setupComplete -> {
            // Show Smart Ring Setup after login (using new MVVM architecture)
            com.fitness.app.presentation.ring.screens.RingSetupScreen(
                onSetupComplete = {
                    AppState.setSetupComplete(true)
                },
                onSkip = {
                    AppState.setSetupComplete(true)
                }
            )
        }
        else -> {
            // Show full app with dashboard
            FitnessAppWithStress()
        }
    }
}

@Composable
fun FitnessWebView(onLoginSuccess: () -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                // Add JavaScript bridge for login callback
                addJavascriptInterface(LoginBridge(onLoginSuccess), "LoginBridge")
                loadUrl("file:///android_asset/index.html?page=login")
            }
        }
    )
}

@Composable
fun FitnessAppWithStress() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                addJavascriptInterface(PageBridge(), "PageBridge")
                loadUrl("file:///android_asset/index.html?page=dashboard")
            }
        }
    )
}

// Bridge class for communication between WebView and Kotlin
class LoginBridge(private val onLoginSuccess: () -> Unit) {
    @android.webkit.JavascriptInterface
    fun notifyLoginSuccess() {
        onLoginSuccess()
    }
}

class PageBridge {
    @android.webkit.JavascriptInterface
    fun getPageState(): String {
        return "dashboard"
    }
    
    // ============= NETWORK API CALLS =============
    
    @android.webkit.JavascriptInterface
    fun sendOtp(phoneNumber: String): String {
        // This should be called asynchronously in real app
        return """{"status":"pending","message":"OTP sent to $phoneNumber"}"""
    }
    
    @android.webkit.JavascriptInterface
    fun loginWithOtp(phoneNumber: String, otp: String): String {
        // Call API: MainActivity.fitnessRepository.login(phoneNumber, otp)
        return """{"success":true,"message":"Logged in","token":"sample_token"}"""
    }
    
    @android.webkit.JavascriptInterface
    fun getUserProfile(): String {
        // Call API: MainActivity.fitnessRepository.getUserProfile()
        return """{"id":"123","name":"User","email":"user@example.com","phone":"1234567890"}"""
    }
    
    @android.webkit.JavascriptInterface
    fun getRemoteSteps(): String {
        // Call API: MainActivity.fitnessRepository.getSteps()
        return """{"steps":${MockData.steps},"goal":10000,"progress":${MockData.steps.toFloat()/10000}}"""
    }
    
    @android.webkit.JavascriptInterface
    fun logRemoteSteps(steps: Int): String {
        // Call API: MainActivity.fitnessRepository.logSteps()
        return """{"success":true,"message":"Steps logged"}"""
    }
    
    @android.webkit.JavascriptInterface
    fun getRemoteCalories(): String {
        // Call API: MainActivity.fitnessRepository.getCalories()
        return """​{"calories":${MockData.calories},"goal":750,"progress":${MockData.calories.toFloat()/750}}​"""
    }
    
    @android.webkit.JavascriptInterface
    fun getRemoteHeartRate(): String {
        // Call API: MainActivity.fitnessRepository.getHeartRate()
        return """{"bpm":${MockData.heartRate},"avg":${MockData.heartRate},"min":${MockData.heartRate - 10},"max":${MockData.heartRate + 15}}"""
    }
    
    @android.webkit.JavascriptInterface
    fun logRemoteWorkout(name: String, type: String, duration: Int, calories: Int): String {
        // Call API: MainActivity.fitnessRepository.logWorkout()
        return """{"success":true,"message":"Workout logged","caloriesBurned":$calories}"""
    }
    
    @android.webkit.JavascriptInterface
    fun getRemoteWorkouts(): String {
        // Call API: MainActivity.fitnessRepository.getWorkouts()
        return """[{"id":"1","name":"Morning Run","type":"running","duration":30,"caloriesBurned":350,"distance":5.2,"intensity":"high","date":"2025-11-14"}]"""
    }
    
    @android.webkit.JavascriptInterface
    fun getRemoteWorkoutStats(): String {
        // Call API: MainActivity.fitnessRepository.getWorkoutStats()
        return """{"totalWorkouts":5,"totalCalories":1500,"totalDistance":25.0,"totalDuration":150}"""
    }
    
    @android.webkit.JavascriptInterface
    fun getRemoteDailySummary(): String {
        // Call API: MainActivity.fitnessRepository.getDailySummary()
        return """{"date":"2025-11-14","steps":${MockData.steps},"calories":${MockData.calories},"avgHeartRate":${MockData.heartRate},"activeTime":${MockData.workoutMinutes},"waterIntake":6,"sleepDuration":450}"""
    }
    
    @android.webkit.JavascriptInterface
    fun getRemotePreferences(): String {
        // Call API: MainActivity.fitnessRepository.getPreferences()
        return """{"theme":"light","units":"metric","stepGoal":10000,"calorieGoal":750,"notificationsEnabled":true,"syncInterval":15}"""
    }
    
    @android.webkit.JavascriptInterface
    fun updateRemotePreferences(theme: String, units: String, stepGoal: Int, calorieGoal: Int): String {
        // Call API: MainActivity.fitnessRepository.updatePreferences()
        return """{"success":true,"message":"Preferences updated"}"""
    }
    
    // ============= LOCAL API CALLS (Original) =============
    
    // Step Counting API
    @android.webkit.JavascriptInterface
    fun getSteps(): String {
        val stepData = MainActivity.fitnessAPI.getSteps()
        return """{"steps":${stepData.steps},"goal":${stepData.goal},"progress":${stepData.progress}}"""
    }
    
    @android.webkit.JavascriptInterface
    fun incrementSteps(count: Int) {
        MainActivity.fitnessAPI.incrementSteps(count)
    }
    
    @android.webkit.JavascriptInterface
    fun setSteps(steps: Int) {
        MainActivity.fitnessAPI.setSteps(steps)
    }
    
    // Calorie API
    @android.webkit.JavascriptInterface
    fun getCalories(): String {
        val calorieData = MainActivity.fitnessAPI.getCalories()
        return """{"calories":${calorieData.calories},"goal":${calorieData.goal},"progress":${calorieData.progress}}"""
    }
    
    @android.webkit.JavascriptInterface
    fun addCalories(calories: Int) {
        MainActivity.fitnessAPI.addCalories(calories)
    }
    
    @android.webkit.JavascriptInterface
    fun setCalories(calories: Int) {
        MainActivity.fitnessAPI.setCalories(calories)
    }
    
    @android.webkit.JavascriptInterface
    fun calculateCaloriesBurned(activityType: String, durationMinutes: Int, weight: Int = 75): Int {
        return MainActivity.fitnessAPI.calculateCaloriesBurned(activityType, durationMinutes, weight)
    }
    
    // Heart Rate API
    @android.webkit.JavascriptInterface
    fun getHeartRate(): String {
        val hrData = MainActivity.fitnessAPI.getHeartRate()
        return """{"bpm":${hrData.currentBPM},"avg":${hrData.averageBPM},"min":${hrData.minBPM},"max":${hrData.maxBPM}}"""
    }
    
    @android.webkit.JavascriptInterface
    fun setHeartRate(bpm: Int) {
        MainActivity.fitnessAPI.setHeartRate(bpm)
    }
    
    // Timer API
    @android.webkit.JavascriptInterface
    fun getTimers(): String {
        val timers = MainActivity.fitnessAPI.getTimers()
        val timerJson = timers.map { 
            """{"id":"${it.id}","name":"${it.name}","total":${it.totalSeconds},"remaining":${it.remainingSeconds},"running":${it.isRunning},"type":"${it.type}"}"""
        }.joinToString(",")
        return "[$timerJson]"
    }
    
    @android.webkit.JavascriptInterface
    fun createTimer(name: String, seconds: Int, type: String) {
        MainActivity.fitnessAPI.createTimer(name, seconds, type)
    }
    
    @android.webkit.JavascriptInterface
    fun startTimer(timerId: String) {
        MainActivity.fitnessAPI.startTimer(timerId)
    }
    
    @android.webkit.JavascriptInterface
    fun pauseTimer(timerId: String) {
        MainActivity.fitnessAPI.pauseTimer(timerId)
    }
    
    @android.webkit.JavascriptInterface
    fun updateTimer(timerId: String, remaining: Int) {
        MainActivity.fitnessAPI.updateTimer(timerId, remaining)
    }
    
    @android.webkit.JavascriptInterface
    fun deleteTimer(timerId: String) {
        MainActivity.fitnessAPI.deleteTimer(timerId)
    }
    
    // Daily Summary API
    @android.webkit.JavascriptInterface
    fun getDailySummary(date: String = ""): String {
        val summary = MainActivity.fitnessAPI.getDailySummary(if (date.isEmpty()) java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) else date)
        return """{"steps":${summary.totalSteps},"calories":${summary.totalCalories},"avgHR":${summary.averageHeartRate},"activeTime":${summary.activeTime},"water":${summary.waterIntake},"sleep":${summary.sleepDuration}}"""
    }
    
    @android.webkit.JavascriptInterface
    fun saveDailySummary(stepsJson: String) {
        try {
            // Parse JSON from JavaScript
            val parts = stepsJson.split(",")
            // Simple parsing for demo purposes
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Offline Workouts API
    @android.webkit.JavascriptInterface
    fun getWorkouts(): String {
        val workouts = MainActivity.fitnessAPI.getWorkouts()
        val workoutJson = workouts.map {
            """{"id":"${it.id}","name":"${it.name}","type":"${it.type}","duration":${it.duration},"calories":${it.caloriesBurned},"distance":${it.distance},"intensity":"${it.intensity}","date":"${it.date}"}"""
        }.joinToString(",")
        return "[$workoutJson]"
    }
    
    @android.webkit.JavascriptInterface
    fun addWorkout(name: String, type: String, duration: Int, calories: Int, distance: Float, intensity: String) {
        MainActivity.fitnessAPI.addWorkout(name, type, duration, calories, distance, intensity)
    }
    
    @android.webkit.JavascriptInterface
    fun deleteWorkout(workoutId: String) {
        MainActivity.fitnessAPI.deleteWorkout(workoutId)
    }
    
    @android.webkit.JavascriptInterface
    fun getWorkoutStats(): String {
        val stats = MainActivity.fitnessAPI.getWorkoutStats()
        return """{"total":${stats["totalWorkouts"]},"calories":${stats["totalCalories"]},"distance":${stats["totalDistance"]},"duration":${stats["totalDuration"]}}"""
    }
    
    // Workout Progress API
    @android.webkit.JavascriptInterface
    fun getWorkoutProgress(type: String): String {
        val progress = MainActivity.fitnessAPI.getWorkoutProgress(type)
        return """{"count":${progress["count"]},"time":${progress["totalTime"]},"calories":${progress["totalCalories"]}}"""
    }
    
    @android.webkit.JavascriptInterface
    fun getPersonalRecords(): String {
        val records = MainActivity.fitnessAPI.getPersonalRecords()
        return """{"longestWorkout":${records["longestWorkout"]},"maxCalories":${records["maxCaloriesBurned"]},"longestDistance":${records["longestDistance"]}}"""
    }
    
    // Preferences API
    @android.webkit.JavascriptInterface
    fun getPreferences(): String {
        val prefs = MainActivity.fitnessAPI.getUserPreferences()
        return """{"theme":"${prefs.theme}","units":"${prefs.units}","stepGoal":${prefs.dailyStepGoal},"calorieGoal":${prefs.dailyCalorieGoal},"notifications":${prefs.notificationsEnabled},"syncInterval":${prefs.syncInterval}}"""
    }
    
    @android.webkit.JavascriptInterface
    fun setTheme(theme: String) {
        MainActivity.fitnessAPI.setTheme(theme)
    }
    
    @android.webkit.JavascriptInterface
    fun setUnits(units: String) {
        MainActivity.fitnessAPI.setUnits(units)
    }
    
    @android.webkit.JavascriptInterface
    fun setDailyStepGoal(goal: Int) {
        MainActivity.fitnessAPI.setDailyStepGoal(goal)
    }
    
    @android.webkit.JavascriptInterface
    fun setDailyCalorieGoal(goal: Int) {
        MainActivity.fitnessAPI.setDailyCalorieGoal(goal)
    }
    
    // ============= MEDITATION TIMER API =============
    
    @android.webkit.JavascriptInterface
    fun startMeditation(sessionId: String) {
        MainActivity.fitnessAPI.startMeditation(sessionId)
    }
    
    @android.webkit.JavascriptInterface
    fun pauseMeditation() {
        MainActivity.fitnessAPI.pauseMeditation()
    }
    
    @android.webkit.JavascriptInterface
    fun resumeMeditation() {
        MainActivity.fitnessAPI.resumeMeditation()
    }
    
    @android.webkit.JavascriptInterface
    fun stopMeditation() {
        MainActivity.fitnessAPI.stopMeditation()
    }
    
    @android.webkit.JavascriptInterface
    fun getMeditationState(): String {
        val meditation = MainActivity.fitnessAPI.getMeditationSession()
        return if (meditation != null) {
            """{"id":"${meditation.session.id}","name":"${meditation.session.name}","remaining":${meditation.remainingSeconds},"total":${meditation.totalSeconds},"running":${meditation.isRunning},"completed":${meditation.isCompleted},"progress":${meditation.progress}}"""
        } else {
            """{"id":"","name":"","remaining":0,"total":0,"running":false,"completed":false,"progress":0}"""
        }
    }
    
    @android.webkit.JavascriptInterface
    fun onMeditationComplete(sessionId: String, durationSeconds: Int) {
        // Called when meditation timer completes
        // Can be used to track meditation stats, show notifications, etc.
    }
    
    @android.webkit.JavascriptInterface
    fun getTotalMeditationMinutes(): Int {
        return MainActivity.fitnessAPI.getTotalMeditationMinutes()
    }
    
    @android.webkit.JavascriptInterface
    fun getMeditationSessionsCompleted(): Int {
        return MainActivity.fitnessAPI.getMeditationSessionsCompleted()
    }
}

