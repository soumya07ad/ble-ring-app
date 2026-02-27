package com.fitness.app.data.repository

import com.fitness.app.data.local.dao.CoachDao
import com.fitness.app.data.local.entity.CoachMessageEntity
import com.fitness.app.domain.repository.ICoachRepository
import kotlinx.coroutines.flow.Flow

class CoachRepositoryImpl(
    private val coachDao: CoachDao
) : ICoachRepository {

    override fun getAllSessions(): Flow<List<CoachMessageEntity>> {
        return coachDao.getAllSessions()
    }

    override fun getMessagesBySession(sessionId: String): Flow<List<CoachMessageEntity>> {
        return coachDao.getMessagesBySession(sessionId)
    }

    override suspend fun saveMessage(text: String, isUser: Boolean, sessionId: String) {
        coachDao.insertMessage(
            CoachMessageEntity(
                text = text,
                isUser = isUser,
                sessionId = sessionId
            )
        )
    }

    override suspend fun clearHistory() {
        coachDao.deleteAllMessages()
    }

    override suspend fun deleteSession(sessionId: String) {
        coachDao.deleteSession(sessionId)
    }
}
