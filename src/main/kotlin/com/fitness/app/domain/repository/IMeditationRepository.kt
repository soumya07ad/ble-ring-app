package com.fitness.app.domain.repository

import com.fitness.app.domain.model.MeditationSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for meditation sessions and statistics.
 */
interface IMeditationRepository {
    /**
     * Get all available meditation sessions.
     */
    fun getSessions(): List<MeditationSession>

    /**
     * Get a session by its unique ID.
     */
    fun getSessionById(id: String): MeditationSession?

    /**
     * Get sessions by category.
     */
    fun getSessionsByCategory(category: String): List<MeditationSession>

    /**
     * Get total meditation minutes completed.
     */
    fun getTotalMinutes(): Flow<Int>

    /**
     * Get total number of sessions completed.
     */
    fun getSessionsCompletedCount(): Flow<Int>

    /**
     * Track a completed meditation session.
     */
    suspend fun trackCompletedSession(session: MeditationSession, minutes: Int)
}
