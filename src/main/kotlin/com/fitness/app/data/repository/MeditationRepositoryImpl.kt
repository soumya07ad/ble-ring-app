package com.fitness.app.data.repository

import com.fitness.app.domain.model.MeditationSession
import com.fitness.app.domain.repository.IMeditationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of IMeditationRepository.
 * In a real app, this would be backed by Room or DataStore for persistence.
 */
class MeditationRepositoryImpl : IMeditationRepository {

    private val _totalMinutes = MutableStateFlow(0)
    private val _sessionsCompleted = MutableStateFlow(0)

    private val sessions = listOf(
        MeditationSession(
            id = "morning_calm",
            name = "Morning Calm",
            description = "Start your day with peace and clarity",
            durationSeconds = 300, // 5 minutes
            icon = "🌅",
            category = "morning"
        ),
        MeditationSession(
            id = "breathing_exercise",
            name = "Breathing Exercise",
            description = "Reduce stress with deep breathing techniques",
            durationSeconds = 600, // 10 minutes
            icon = "🌬️",
            category = "breathing"
        ),
        MeditationSession(
            id = "sleep_meditation",
            name = "Sleep Meditation",
            description = "Prepare your mind and body for restful sleep",
            durationSeconds = 900, // 15 minutes
            icon = "🌙",
            category = "sleep"
        )
    )

    override fun getSessions(): List<MeditationSession> = sessions

    override fun getSessionById(id: String): MeditationSession? = 
        sessions.find { it.id == id }

    override fun getSessionsByCategory(category: String): List<MeditationSession> = 
        sessions.filter { it.category == category }

    override fun getTotalMinutes(): Flow<Int> = _totalMinutes.asStateFlow()

    override fun getSessionsCompletedCount(): Flow<Int> = _sessionsCompleted.asStateFlow()

    override suspend fun trackCompletedSession(session: MeditationSession, minutes: Int) {
        _totalMinutes.value += minutes
        _sessionsCompleted.value += 1
    }
}
