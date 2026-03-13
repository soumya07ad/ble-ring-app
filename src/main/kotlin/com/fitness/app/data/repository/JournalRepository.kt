package com.fitness.app.data.repository

import com.fitness.app.data.local.dao.JournalDao
import com.fitness.app.data.local.entity.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class JournalRepository(private val dao: JournalDao) {

    suspend fun insertEntry(emotion: String, message: String?, audioPath: String?) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        dao.insertEntry(
            JournalEntry(
                emotion = emotion,
                message = message,
                audioPath = audioPath,
                date = today
            )
        )
    }

    fun getAllEntries(): Flow<List<JournalEntry>> = dao.getAllEntries()
}
