package com.fitness.app.presentation.navigation

sealed class Screen(val route: String, val label: String, val emoji: String) {
    object Dashboard : Screen("dashboard", "Dashboard", "🏠")
    object Sleep : Screen("sleep", "Sleep", "😴")
    object Wellness : Screen("wellness", "Wellness", "💎") 
    object Streaks : Screen("streaks", "Streaks", "🔥")
    object Coach : Screen("coach", "AURA", "🤖")
    object Settings : Screen("settings", "Settings", "⚙️")
    object Journal : Screen("journal", "Journal", "📔")
    object FitnessHistory : Screen("fitnessHistory", "Fitness History", "📊")

    // Meditation sub-screens
    object MorningCalm : Screen("meditation/morning_calm", "Morning Calm", "🌅")
    object BreathingExercise : Screen("meditation/breathing", "Breathing Exercise", "🌬️")
    object SleepMeditation : Screen("meditation/sleep", "Sleep Meditation", "🌙")
    object MeditationTimer : Screen("meditation/timer/{exerciseId}/{category}", "Timer", "⏱️") {
        fun createRoute(exerciseId: String, category: String) = "meditation/timer/$exerciseId/$category"
    }

    companion object {
        val bottomNavItems = listOf(Dashboard, Sleep, Wellness, Streaks)
    }
}
