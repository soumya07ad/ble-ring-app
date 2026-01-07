package com.fitness.app.network.api

import com.fitness.app.network.models.*
import retrofit2.Response
import retrofit2.http.*

interface FitnessApiService {

    // ============= Authentication Endpoints =============
    
    @POST("auth/send-otp")
    suspend fun sendOtp(
        @Body request: OtpRequest
    ): Response<ApiResponse<OtpResponse>>
    
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>
    
    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Header("Authorization") token: String
    ): Response<LoginResponse>
    
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Unit>>
    
    // ============= User Profile Endpoints =============
    
    @GET("users/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserProfile>>
    
    @PUT("users/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<UserProfile>>
    
    // ============= Health Data Endpoints =============
    
    @GET("health/steps")
    suspend fun getSteps(
        @Header("Authorization") token: String,
        @Query("date") date: String? = null
    ): Response<ApiResponse<StepDataResponse>>
    
    @POST("health/steps")
    suspend fun logSteps(
        @Header("Authorization") token: String,
        @Body data: StepDataResponse
    ): Response<ApiResponse<StepDataResponse>>
    
    @GET("health/calories")
    suspend fun getCalories(
        @Header("Authorization") token: String,
        @Query("date") date: String? = null
    ): Response<ApiResponse<CalorieDataResponse>>
    
    @POST("health/calories")
    suspend fun logCalories(
        @Header("Authorization") token: String,
        @Body data: CalorieDataResponse
    ): Response<ApiResponse<CalorieDataResponse>>
    
    @GET("health/heart-rate")
    suspend fun getHeartRate(
        @Header("Authorization") token: String
    ): Response<ApiResponse<HeartRateResponse>>
    
    @POST("health/heart-rate")
    suspend fun logHeartRate(
        @Header("Authorization") token: String,
        @Body data: HeartRateResponse
    ): Response<ApiResponse<HeartRateResponse>>
    
    // ============= Workout Endpoints =============
    
    @GET("workouts")
    suspend fun getWorkouts(
        @Header("Authorization") token: String,
        @Query("date") date: String? = null
    ): Response<ApiResponse<List<WorkoutResponse>>>
    
    @POST("workouts")
    suspend fun logWorkout(
        @Header("Authorization") token: String,
        @Body request: LogWorkoutRequest
    ): Response<ApiResponse<WorkoutResponse>>
    
    @DELETE("workouts/{id}")
    suspend fun deleteWorkout(
        @Header("Authorization") token: String,
        @Path("id") workoutId: String
    ): Response<ApiResponse<Unit>>
    
    @GET("workouts/stats")
    suspend fun getWorkoutStats(
        @Header("Authorization") token: String
    ): Response<ApiResponse<WorkoutStatsResponse>>
    
    @GET("workouts/personal-records")
    suspend fun getPersonalRecords(
        @Header("Authorization") token: String
    ): Response<ApiResponse<PersonalRecordsResponse>>
    
    // ============= Daily Summary Endpoints =============
    
    @GET("health/daily-summary")
    suspend fun getDailySummary(
        @Header("Authorization") token: String,
        @Query("date") date: String? = null
    ): Response<ApiResponse<DailySummaryResponse>>
    
    @POST("health/daily-summary")
    suspend fun saveDailySummary(
        @Header("Authorization") token: String,
        @Body summary: DailySummaryResponse
    ): Response<ApiResponse<DailySummaryResponse>>
    
    // ============= User Preferences Endpoints =============
    
    @GET("users/preferences")
    suspend fun getPreferences(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserPreferencesResponse>>
    
    @PUT("users/preferences")
    suspend fun updatePreferences(
        @Header("Authorization") token: String,
        @Body request: UpdatePreferencesRequest
    ): Response<ApiResponse<UserPreferencesResponse>>
    
    // ============= Timer Endpoints =============
    
    @GET("timers")
    suspend fun getTimers(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<TimerResponse>>>
    
    @POST("timers")
    suspend fun createTimer(
        @Header("Authorization") token: String,
        @Body timer: TimerResponse
    ): Response<ApiResponse<TimerResponse>>
    
    @PUT("timers/{id}")
    suspend fun updateTimer(
        @Header("Authorization") token: String,
        @Path("id") timerId: String,
        @Body timer: TimerResponse
    ): Response<ApiResponse<TimerResponse>>
    
    @DELETE("timers/{id}")
    suspend fun deleteTimer(
        @Header("Authorization") token: String,
        @Path("id") timerId: String
    ): Response<ApiResponse<Unit>>
}
