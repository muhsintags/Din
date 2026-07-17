package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ReadingHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {
    @Query("SELECT * FROM reading_histories ORDER BY id DESC")
    fun getAllReadingHistory(): Flow<List<ReadingHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHistory(history: ReadingHistory): Long

    @Query("SELECT * FROM reading_histories WHERE bookTitle = :bookTitle LIMIT 1")
    suspend fun getHistoryByBookTitle(bookTitle: String): ReadingHistory?

    @Query("DELETE FROM reading_histories WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("DELETE FROM reading_histories")
    suspend fun clearReadingHistory()
}
