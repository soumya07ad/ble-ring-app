package com.fitness.app.domain.model

data class CoachMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class CoachSession(
    val id: String,
    val firstMessage: String,
    val timestamp: Long
)
