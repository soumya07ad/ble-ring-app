package com.fitness.app.network.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import com.fitness.app.network.models.*

interface ImageUploadService {
    
    @Multipart
    @POST("users/upload-profile-image")
    suspend fun uploadProfileImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<ImageUploadResponse>>
}

data class ImageUploadResponse(
    val imageUrl: String,
    val fileName: String
)
