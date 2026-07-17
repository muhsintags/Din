package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_histories")
data class ReadingHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookTitle: String,
    val subtitle: String,
    val progressPercent: Int,
    val dateText: String,
    val surahOrChapter: String? = null,
    val pagesRead: Int = 0,
    val isCompleted: Boolean = false,
    val contemplationMinutes: Int = 0
)
