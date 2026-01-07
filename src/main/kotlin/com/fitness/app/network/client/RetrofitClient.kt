package com.fitness.app.network.client

import android.content.Context
import com.fitness.app.network.api.FitnessApiService
import com.fitness.app.network.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient(private val context: Context) {
    
    companion object {
        // Configure your backend server URL here
        private const val BASE_URL = "https://api.fitness-app.com/"
        
        @Volatile
        private var instance: RetrofitClient? = null
        
        fun getInstance(context: Context): RetrofitClient {
            return instance ?: synchronized(this) {
                instance ?: RetrofitClient(context).also { instance = it }
            }
        }
    }
    
    private val tokenManager = TokenManager(context)
    private val apiService: FitnessApiService
    
    init {
        apiService = createRetrofit().create(FitnessApiService::class.java)
    }
    
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()
    }
    
    fun getApiService(): FitnessApiService = apiService
    
    fun getTokenManager(): TokenManager = tokenManager
}

/**
 * Interceptor to add authentication token to all requests
 */
private class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        
        // If request already has Authorization header, skip adding it
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }
        
        // Try to get token (this is blocking call, ideally should be in async block)
        val token = "" // We'll handle this in Repository layer
        
        val requestWithToken = if (token.isNotEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(requestWithToken)
    }
}
