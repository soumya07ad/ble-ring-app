package com.fitness.app.network.client

import android.content.Context
import com.fitness.app.network.api.FitnessApiService
import com.fitness.app.network.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.runBlocking

class RetrofitClient(private val context: Context) {
    
    companion object {
        // Localhost URL for Android Emulator (10.0.2.2 points to host machine's localhost)
        // For physical device testing over Wi-Fi, using the host machine's IPv4 address:
        private const val BASE_URL = "https://wellness-backend-a0gqd5jzq-soumya07ads-projects.vercel.app/"
        
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
        
        // Try to get token (this is blocking call)
        val token = runBlocking { tokenManager.getToken() } ?: ""
        
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
