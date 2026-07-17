package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.NoteHighlight
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteHighlightDao {
    @Query("SELECT * FROM notes_highlights ORDER BY id DESC")
    fun getAllNotesHighlights(): Flow<List<NoteHighlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteHighlight(noteHighlight: NoteHighlight): Long

    @Query("DELETE FROM notes_highlights WHERE id = :id")
    suspend fun deleteNoteHighlightById(id: Int)

    @Query("DELETE FROM notes_highlights")
    suspend fun clearNotesHighlights()
}
