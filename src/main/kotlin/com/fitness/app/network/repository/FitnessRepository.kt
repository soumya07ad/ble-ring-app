package com.fitness.app.network.repository

import com.fitness.app.network.api.FitnessApiService
import com.fitness.app.network.auth.TokenManager
import com.fitness.app.network.models.*
import okhttp3.MultipartBody
import retrofit2.Response

sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String, val code: Int = 0) : ApiResult<T>()
    class Loading<T> : ApiResult<T>()
}

class FitnessRepository(
    private val apiService: FitnessApiService,
    private val tokenManager: TokenManager
) {
    
    // ============= Authentication =============
    
    suspend fun sendOtp(phoneNumber: String): ApiResult<OtpResponse> {
        return safeApiCall {
            apiService.sendOtp(OtpRequest(phoneNumber))
        }
    }
    
    suspend fun login(phoneNumber: String, otp: String): ApiResult<LoginResponse> {
        return safeApiCall {
            apiService.login(LoginRequest(phoneNumber, otp))
        }.also { result ->
            if (result is ApiResult.Success) {
                tokenManager.saveToken(result.data.token)
                tokenManager.saveUserInfo(result.data.user.id, result.data.user.phoneNumber)
            }
        }
    }
    
    suspend fun logout(): ApiResult<Unit> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.logout("Bearer $token")
        }.also {
            if (it is ApiResult.Success) {
                tokenManager.clearToken()
            }
        }
    }
    
    // ============= User Profile =============
    
    suspend fun getUserProfile(): ApiResult<UserProfile> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getUserProfile("Bearer $token")
        }
    }
    
    suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<UserProfile> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.updateProfile("Bearer $token", request)
        }
    }
    
    // ============= Health Data =============
    
    suspend fun getSteps(date: String? = null): ApiResult<StepDataResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getSteps("Bearer $token", date)
        }
    }
    
    suspend fun logSteps(data: StepDataResponse): ApiResult<StepDataResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.logSteps("Bearer $token", data)
        }
    }
    
    suspend fun getCalories(date: String? = null): ApiResult<CalorieDataResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getCalories("Bearer $token", date)
        }
    }
    
    suspend fun logCalories(data: CalorieDataResponse): ApiResult<CalorieDataResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.logCalories("Bearer $token", data)
        }
    }
    
    suspend fun getHeartRate(): ApiResult<HeartRateResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getHeartRate("Bearer $token")
        }
    }
    
    suspend fun logHeartRate(data: HeartRateResponse): ApiResult<HeartRateResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.logHeartRate("Bearer $token", data)
        }
    }
    
    // ============= Workouts =============
    
    suspend fun getWorkouts(date: String? = null): ApiResult<List<WorkoutResponse>> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getWorkouts("Bearer $token", date)
        }
    }
    
    suspend fun logWorkout(request: LogWorkoutRequest): ApiResult<WorkoutResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.logWorkout("Bearer $token", request)
        }
    }
    
    suspend fun deleteWorkout(workoutId: String): ApiResult<Unit> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.deleteWorkout("Bearer $token", workoutId)
        }
    }
    
    suspend fun getWorkoutStats(): ApiResult<WorkoutStatsResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getWorkoutStats("Bearer $token")
        }
    }
    
    suspend fun getPersonalRecords(): ApiResult<PersonalRecordsResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getPersonalRecords("Bearer $token")
        }
    }
    
    // ============= Daily Summary =============
    
    suspend fun getDailySummary(date: String? = null): ApiResult<DailySummaryResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getDailySummary("Bearer $token", date)
        }
    }
    
    suspend fun saveDailySummary(summary: DailySummaryResponse): ApiResult<DailySummaryResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.saveDailySummary("Bearer $token", summary)
        }
    }
    
    // ============= User Preferences =============
    
    suspend fun getPreferences(): ApiResult<UserPreferencesResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getPreferences("Bearer $token")
        }
    }
    
    suspend fun updatePreferences(request: UpdatePreferencesRequest): ApiResult<UserPreferencesResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.updatePreferences("Bearer $token", request)
        }
    }
    
    // ============= Image Upload =============
    
    suspend fun uploadProfileImage(imagePart: MultipartBody.Part): ApiResult<ImageUploadResponse> {
        val token = getAuthToken()
        return safeApiCall {
            // This would need ImageUploadService integration
            throw Exception("Image upload endpoint not yet integrated")
        }
    }
    
    // ============= Timers =============
    
    suspend fun getTimers(): ApiResult<List<TimerResponse>> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.getTimers("Bearer $token")
        }
    }
    
    suspend fun createTimer(timer: TimerResponse): ApiResult<TimerResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.createTimer("Bearer $token", timer)
        }
    }
    
    suspend fun updateTimer(timerId: String, timer: TimerResponse): ApiResult<TimerResponse> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.updateTimer("Bearer $token", timerId, timer)
        }
    }
    
    suspend fun deleteTimer(timerId: String): ApiResult<Unit> {
        val token = getAuthToken()
        return safeApiCall {
            apiService.deleteTimer("Bearer $token", timerId)
        }
    }
    
    // ============= Helper Functions =============
    
    /**
     * Safe API call wrapper with error handling
     */
    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<ApiResponse<T>>): ApiResult<T> {
        return try {
            val response = apiCall()
            when {
                response.isSuccessful && response.body() != null -> {
                    val apiResponse = response.body()!!
                    if (apiResponse.success && apiResponse.data != null) {
                        ApiResult.Success(apiResponse.data)
                    } else {
                        ApiResult.Error(apiResponse.message ?: "Unknown error", response.code())
                    }
                }
                response.code() == 401 -> {
                    // Token expired or invalid
                    tokenManager.clearToken()
                    ApiResult.Error("Unauthorized - Please login again", 401)
                }
                response.code() == 400 -> {
                    ApiResult.Error("Bad request", 400)
                }
                response.code() == 500 -> {
                    ApiResult.Error("Server error", 500)
                }
                else -> {
                    ApiResult.Error("Network error: ${response.message()}", response.code())
                }
            }
        } catch (e: java.io.IOException) {
            // Network connectivity error
            ApiResult.Error("No internet connection", -1)
        } catch (e: Exception) {
            ApiResult.Error("Error: ${e.message}", -1)
        }
    }
    
    /**
     * Get auth token from TokenManager
     */
    private suspend fun getAuthToken(): String {
        // This is a simplified version - in real app, you'd use Flow
        return "dummy_token" // Will be replaced with actual token from DataStore
    }
}
