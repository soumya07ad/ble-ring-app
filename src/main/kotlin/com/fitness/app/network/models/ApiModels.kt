package com.fitness.app.network.models

import com.google.gson.annotations.SerializedName

// Authentication Models
data class LoginRequest(
    val phoneNumber: String,
    val otp: String
)

data class LoginResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("user")
    val user: User,
    @SerializedName("success")
    val success: Boolean = true
)

data class User(
    @SerializedName("id")
    val id: String,
    @SerializedName("phone")
    val phoneNumber: String,
    @SerializedName("name")
    val name: String = "User",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("profilePicture")
    val profilePicture: String = ""
)

// Profile Models
data class UserProfile(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phone")
    val phoneNumber: String,
    @SerializedName("age")
    val age: Int,
    @SerializedName("weight")
    val weight: Int, // kg
    @SerializedName("height")
    val height: Int, // cm
    @SerializedName("gender")
    val gender: String, // "M", "F", "Other"
    @SerializedName("profilePicture")
    val profilePicture: String,
    @SerializedName("createdAt")
    val createdAt: String
)

data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val age: Int,
    val weight: Int,
    val height: Int,
    val gender: String
)

// Health Data Models
data class StepDataResponse(
    @SerializedName("steps")
    val steps: Int,
    @SerializedName("goal")
    val goal: Int,
    @SerializedName("progress")
    val progress: Float,
    @SerializedName("date")
    val date: String
)

data class CalorieDataResponse(
    @SerializedName("calories")
    val calories: Int,
    @SerializedName("goal")
    val goal: Int,
    @SerializedName("progress")
    val progress: Float,
    @SerializedName("date")
    val date: String
)

data class HeartRateResponse(
    @SerializedName("currentBPM")
    val currentBPM: Int,
    @SerializedName("averageBPM")
    val averageBPM: Int,
    @SerializedName("minBPM")
    val minBPM: Int,
    @SerializedName("maxBPM")
    val maxBPM: Int,
    @SerializedName("timestamp")
    val timestamp: Long
)

// Workout Models
data class WorkoutResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String, // "running", "cycling", "yoga", etc.
    @SerializedName("duration")
    val duration: Int, // minutes
    @SerializedName("caloriesBurned")
    val caloriesBurned: Int,
    @SerializedName("distance")
    val distance: Float, // km
    @SerializedName("intensity")
    val intensity: String, // "low", "medium", "high"
    @SerializedName("date")
    val date: String,
    @SerializedName("notes")
    val notes: String = "",
    @SerializedName("heartRateData")
    val heartRateData: HeartRateData? = null
)

data class HeartRateData(
    @SerializedName("avgBPM")
    val avgBPM: Int,
    @SerializedName("maxBPM")
    val maxBPM: Int
)

data class LogWorkoutRequest(
    val name: String,
    val type: String,
    val duration: Int,
    val caloriesBurned: Int,
    val distance: Float,
    val intensity: String,
    val notes: String = ""
)

// Daily Summary Models
data class DailySummaryResponse(
    @SerializedName("date")
    val date: String,
    @SerializedName("steps")
    val steps: Int,
    @SerializedName("calories")
    val calories: Int,
    @SerializedName("avgHeartRate")
    val avgHeartRate: Int,
    @SerializedName("activeTime")
    val activeTime: Int, // minutes
    @SerializedName("waterIntake")
    val waterIntake: Int, // cups
    @SerializedName("sleepDuration")
    val sleepDuration: Int // minutes
)

// Preferences Models
data class UserPreferencesResponse(
    @SerializedName("theme")
    val theme: String,
    @SerializedName("units")
    val units: String, // "metric" or "imperial"
    @SerializedName("dailyStepGoal")
    val dailyStepGoal: Int,
    @SerializedName("dailyCalorieGoal")
    val dailyCalorieGoal: Int,
    @SerializedName("notificationsEnabled")
    val notificationsEnabled: Boolean,
    @SerializedName("syncInterval")
    val syncInterval: Int
)

data class UpdatePreferencesRequest(
    val theme: String,
    val units: String,
    val dailyStepGoal: Int,
    val dailyCalorieGoal: Int,
    val notificationsEnabled: Boolean,
    val syncInterval: Int
)

// Workout Statistics
data class WorkoutStatsResponse(
    @SerializedName("totalWorkouts")
    val totalWorkouts: Int,
    @SerializedName("totalCalories")
    val totalCalories: Int,
    @SerializedName("totalDistance")
    val totalDistance: Float,
    @SerializedName("totalDuration")
    val totalDuration: Int,
    @SerializedName("favoriteType")
    val favoriteType: String
)

// Personal Records
data class PersonalRecordsResponse(
    @SerializedName("longestWorkout")
    val longestWorkout: Int,
    @SerializedName("maxCaloriesBurned")
    val maxCaloriesBurned: Int,
    @SerializedName("longestDistance")
    val longestDistance: Float,
    @SerializedName("mostIntenseWorkout")
    val mostIntenseWorkout: Int
)

// Timer Models
data class TimerResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("totalSeconds")
    val totalSeconds: Int,
    @SerializedName("remainingSeconds")
    val remainingSeconds: Int,
    @SerializedName("isRunning")
    val isRunning: Boolean,
    @SerializedName("type")
    val type: String
)

// Generic API Response wrapper
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("message")
    val message: String = "",
    @SerializedName("error")
    val error: String? = null
)

// OTP Request/Response
data class OtpRequest(
    val phoneNumber: String
)

data class OtpResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)

// Error Response
data class ErrorResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("message")
    val message: String,
    @SerializedName("code")
    val code: Int = 400
)

// Image Upload Response
data class ImageUploadResponse(
    @SerializedName("imageUrl")
    val imageUrl: String,
    @SerializedName("fileName")
    val fileName: String
)
