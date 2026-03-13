package com.fitness.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val emotion: String,
    val message: String? = null,
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val date: String // ISO format: "yyyy-MM-dd"
)
