package com.fitness.app.network.models

import com.google.gson.annotations.SerializedName

// Gemini API Request Models
data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig")
    val generationConfig: GenerationConfig = GenerationConfig()
)

data class GeminiContent(
    @SerializedName("role")
    val role: String,
    @SerializedName("parts")
    val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text")
    val text: String
)

data class GenerationConfig(
    @SerializedName("temperature")
    val temperature: Double = 0.7,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int = 1024
)

// Gemini API Response Models
data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    @SerializedName("content")
    val content: GeminiContent?,
    @SerializedName("finishReason")
    val finishReason: String?
)

// Keep these aliases for backward compatibility with other code that may use them
typealias ChatRequest = GeminiRequest
typealias ChatResponse = GeminiResponse
typealias ChatMessage = GeminiContent
