package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_highlights")
data class NoteHighlight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookTitle: String,
    val quoteText: String,
    val userReflection: String?, // Null if it's only a highlight
    val dateText: String,
    val type: String // "Note" or "Highlight"
)
