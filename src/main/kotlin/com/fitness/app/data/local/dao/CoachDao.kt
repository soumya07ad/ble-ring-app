package com.fitness.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitness.app.data.local.entity.CoachMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CoachMessageEntity)

    @Query("SELECT * FROM coach_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: String): Flow<List<CoachMessageEntity>>

    @Query("""
        SELECT * FROM coach_messages 
        WHERE id IN (SELECT MIN(id) FROM coach_messages GROUP BY sessionId)
        ORDER BY timestamp DESC
    """)
    fun getAllSessions(): Flow<List<CoachMessageEntity>>

    @Query("SELECT * FROM coach_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<CoachMessageEntity>>

    @Query("DELETE FROM coach_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM coach_messages WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
