package com.fitness.app.domain.model

/**
 * Domain model for a meditation session
 */
data class MeditationSession(
    val id: String,
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val icon: String,
    val category: String // "morning", "breathing", "sleep"
)

/**
 * Domain model for active meditation state
 */
data class ActiveMeditation(
    val session: MeditationSession,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val isRunning: Boolean,
    val isCompleted: Boolean,
    val progress: Float // 0-1
)
