package com.fitness.app.domain.model

/**
 * Domain model for a meditation session (legacy - used by repository layer)
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
 * Domain model for active meditation state (legacy)
 */
data class ActiveMeditation(
    val session: MeditationSession,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val isRunning: Boolean,
    val isCompleted: Boolean,
    val progress: Float // 0-1
)

/**
 * Domain model for a meditation exercise within a category
 */
data class MeditationExercise(
    val id: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val emoji: String,
    val category: String // "morning_calm", "breathing", "sleep"
)


/**
 * Pre-defined exercises for each meditation category
 */
object MeditationData {

    val morningCalm = listOf(
        MeditationExercise("mc_1", "Mindful Start", "Begin your day with clarity and intention", 5, "☀️", "morning_calm"),
        MeditationExercise("mc_2", "Gratitude Meditation", "Cultivate thankfulness for a positive mindset", 7, "🙏", "morning_calm"),
        MeditationExercise("mc_3", "Focus Booster", "Sharpen your concentration for the day ahead", 10, "🎯", "morning_calm")
    )

    val breathing = listOf(
        MeditationExercise("br_1", "Box Breathing", "Inhale, hold, exhale, hold — 4 seconds each", 5, "📦", "breathing"),
        MeditationExercise("br_2", "4-7-8 Breathing", "Relaxation technique for stress relief", 8, "🌊", "breathing"),
        MeditationExercise("br_3", "Relaxation Breathing", "Slow, deep breaths to calm your body", 10, "🍃", "breathing")
    )

    val sleep = listOf(
        MeditationExercise("sl_1", "Deep Sleep Relaxation", "Drift into restful, deep sleep", 15, "💤", "sleep"),
        MeditationExercise("sl_2", "Body Scan", "Progressive relaxation from head to toe", 10, "🧘", "sleep"),
        MeditationExercise("sl_3", "Calm Night Meditation", "Peaceful visualization for a restful night", 12, "🌌", "sleep")
    )

    fun getByCategory(category: String): List<MeditationExercise> = when (category) {
        "morning_calm" -> morningCalm
        "breathing" -> breathing
        "sleep" -> sleep
        else -> emptyList()
    }

    fun findExercise(id: String): MeditationExercise? =
        (morningCalm + breathing + sleep).find { it.id == id }

    fun categoryTitle(category: String): String = when (category) {
        "morning_calm" -> "Morning Calm"
        "breathing" -> "Breathing Exercise"
        "sleep" -> "Sleep Meditation"
        else -> "Meditation"
    }

    fun categoryEmoji(category: String): String = when (category) {
        "morning_calm" -> "🌅"
        "breathing" -> "🌬️"
        "sleep" -> "🌙"
        else -> "🧘"
    }

    fun categoryDescription(category: String): String = when (category) {
        "morning_calm" -> "Start your day with peace and positive energy"
        "breathing" -> "Control your breath to calm mind and body"
        "sleep" -> "Prepare your body for deep, restful sleep"
        else -> ""
    }
}
