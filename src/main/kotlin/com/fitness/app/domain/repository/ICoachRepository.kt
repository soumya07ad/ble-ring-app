package com.fitness.app.domain.repository

import com.fitness.app.data.local.entity.CoachMessageEntity
import kotlinx.coroutines.flow.Flow

interface ICoachRepository {
    fun getAllSessions(): Flow<List<CoachMessageEntity>>
    fun getMessagesBySession(sessionId: String): Flow<List<CoachMessageEntity>>
    suspend fun saveMessage(text: String, isUser: Boolean, sessionId: String)
    suspend fun clearHistory()
    suspend fun deleteSession(sessionId: String)
}
