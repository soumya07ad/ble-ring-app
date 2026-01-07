package com.fitness.app.network.utils

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    
    /**
     * Convert URI to File for multipart upload
     */
    fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "profile_${System.currentTimeMillis()}.jpg"
            val outputFile = File(context.cacheDir, fileName)
            
            outputFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Create MultipartBody.Part from file
     */
    fun fileToMultipart(file: File, partName: String = "image"): MultipartBody.Part {
        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestBody)
    }
    
    /**
     * Compress image file before upload
     */
    fun compressImage(file: File, maxSizeMB: Float = 2f): File {
        val maxSizeBytes = (maxSizeMB * 1024 * 1024).toLong()
        
        return if (file.length() > maxSizeBytes) {
            // In a real app, use bitmap compression
            // For now, just return original
            file
        } else {
            file
        }
    }
    
    /**
     * Convert Uri to MultipartBody.Part directly
     */
    fun uriToMultipart(context: Context, uri: Uri, partName: String = "image"): MultipartBody.Part? {
        val file = uriToFile(context, uri) ?: return null
        return fileToMultipart(file, partName)
    }
}
