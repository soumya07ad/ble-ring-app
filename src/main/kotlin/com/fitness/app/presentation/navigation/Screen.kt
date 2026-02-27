package com.fitness.app.presentation.navigation

sealed class Screen(val route: String, val label: String, val emoji: String) {
    object Dashboard : Screen("dashboard", "Dashboard", "🏠")
    object Sleep : Screen("sleep", "Sleep", "😴")
    object Wellness : Screen("wellness", "Wellness", "💎") 
    object Streaks : Screen("streaks", "Streaks", "🔥")
    object Coach : Screen("coach", "Coach", "🤖")
    object Settings : Screen("settings", "Settings", "⚙️")

    companion object {
        val bottomNavItems = listOf(Dashboard, Sleep, Wellness, Streaks, Coach, Settings)
    }
}
