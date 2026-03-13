package com.fitness.app.domain.model

data class Emotion(
    val name: String,
    val emoji: String,
    val score: Int
)

data class MeditationItem(
    val id: String,
    val title: String,
    val duration: String,
    val durationMinutes: Int,
    val emoji: String
)

data class ActiveTimer(
    val meditationId: String,
    val title: String,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val isRunning: Boolean = true,
    val isCompleted: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds else 0f
}
