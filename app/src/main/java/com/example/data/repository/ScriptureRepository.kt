package com.example.data.repository

import com.example.data.db.NoteHighlightDao
import com.example.data.db.ReadingHistoryDao
import com.example.data.model.Book
import com.example.data.model.BookRepository
import com.example.data.model.NoteHighlight
import com.example.data.model.ReadingHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScriptureRepository(
    private val noteHighlightDao: NoteHighlightDao,
    private val readingHistoryDao: ReadingHistoryDao
) {
    val books: List<Book> = BookRepository.books

    val allNotesHighlights: Flow<List<NoteHighlight>> = noteHighlightDao.getAllNotesHighlights()
    val allReadingHistory: Flow<List<ReadingHistory>> = readingHistoryDao.getAllReadingHistory()

    private var notesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var historyListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun getUserId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }

    fun startFirestoreSync(userId: String, scope: kotlinx.coroutines.CoroutineScope) {
        stopFirestoreSync()

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // 1. Sync notes & highlights from Firestore to Room
        notesListener = db.collection("users").document(userId).collection("notes_highlights")
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener
                if (snapshots != null) {
                    for (dc in snapshots.documentChanges) {
                        val doc = dc.document
                        val id = doc.id.toIntOrNull() ?: continue
                        when (dc.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                val note = NoteHighlight(
                                    id = id,
                                    bookTitle = doc.getString("bookTitle") ?: "",
                                    quoteText = doc.getString("quoteText") ?: "",
                                    userReflection = doc.getString("userReflection"),
                                    dateText = doc.getString("dateText") ?: "",
                                    type = doc.getString("type") ?: "Highlight"
                                )
                                scope.launch {
                                    noteHighlightDao.insertNoteHighlight(note)
                                }
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                scope.launch {
                                    noteHighlightDao.deleteNoteHighlightById(id)
                                }
                            }
                        }
                    }
                }
            }

        // 2. Sync reading history from Firestore to Room
        historyListener = db.collection("users").document(userId).collection("reading_histories")
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener
                if (snapshots != null) {
                    for (dc in snapshots.documentChanges) {
                        val doc = dc.document
                        val id = doc.id.toIntOrNull() ?: continue
                        when (dc.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                val history = ReadingHistory(
                                    id = id,
                                    bookTitle = doc.getString("bookTitle") ?: "",
                                    subtitle = doc.getString("subtitle") ?: "",
                                    progressPercent = doc.getLong("progressPercent")?.toInt() ?: 0,
                                    dateText = doc.getString("dateText") ?: "",
                                    surahOrChapter = doc.getString("surahOrChapter"),
                                    pagesRead = doc.getLong("pagesRead")?.toInt() ?: 0,
                                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                                    contemplationMinutes = doc.getLong("contemplationMinutes")?.toInt() ?: 0
                                )
                                scope.launch {
                                    readingHistoryDao.insertOrUpdateHistory(history)
                                }
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                scope.launch {
                                    readingHistoryDao.deleteHistoryById(id)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun stopFirestoreSync() {
        notesListener?.remove()
        notesListener = null
        historyListener?.remove()
        historyListener = null
    }

    suspend fun insertNoteHighlight(noteHighlight: NoteHighlight) {
        val rowId = noteHighlightDao.insertNoteHighlight(noteHighlight)
        val userId = getUserId()
        if (userId != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val docId = if (noteHighlight.id != 0) noteHighlight.id.toString() else rowId.toString()
            val data = hashMapOf(
                "id" to docId.toInt(),
                "bookTitle" to noteHighlight.bookTitle,
                "quoteText" to noteHighlight.quoteText,
                "userReflection" to noteHighlight.userReflection,
                "dateText" to noteHighlight.dateText,
                "type" to noteHighlight.type
            )
            db.collection("users").document(userId)
                .collection("notes_highlights").document(docId)
                .set(data)
        }
    }

    suspend fun deleteNoteHighlight(id: Int) {
        noteHighlightDao.deleteNoteHighlightById(id)
        val userId = getUserId()
        if (userId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("notes_highlights").document(id.toString())
                .delete()
        }
    }

    suspend fun updateReadingProgress(
        bookTitle: String,
        subtitle: String,
        progress: Int,
        dateText: String,
        surahOrChapter: String? = null,
        pagesRead: Int = 0,
        isCompleted: Boolean = false,
        contemplationMinutes: Int = 0
    ) {
        val existing = readingHistoryDao.getHistoryByBookTitle(bookTitle)
        
        val totalPages = when {
            bookTitle.contains("Quran", ignoreCase = true) || bookTitle.contains("Kur'an", ignoreCase = true) -> 604
            bookTitle.contains("Torah", ignoreCase = true) || bookTitle.contains("Tevrat", ignoreCase = true) -> 1500
            bookTitle.contains("Gospel", ignoreCase = true) || bookTitle.contains("İncil", ignoreCase = true) -> 800
            else -> 100
        }

        val historyToSave = if (existing != null) {
            val isInitiationCall = pagesRead == 0 && contemplationMinutes == 0 && !isCompleted
            val finalPagesRead = existing.pagesRead + pagesRead
            val finalCompleted = isCompleted || existing.isCompleted
            
            val calculatedProgress = ((finalPagesRead.toFloat() / totalPages.toFloat()) * 100f).toInt().coerceIn(1, 100)
            val finalProgress = if (isInitiationCall) {
                existing.progressPercent
            } else {
                if (finalCompleted) 100 else calculatedProgress.coerceAtMost(99)
            }

            existing.copy(
                subtitle = subtitle,
                progressPercent = finalProgress,
                dateText = dateText,
                surahOrChapter = surahOrChapter ?: existing.surahOrChapter,
                pagesRead = finalPagesRead,
                isCompleted = finalCompleted,
                contemplationMinutes = existing.contemplationMinutes + contemplationMinutes
            )
        } else {
            val finalPagesRead = pagesRead
            val finalCompleted = isCompleted
            val calculatedProgress = ((finalPagesRead.toFloat() / totalPages.toFloat()) * 100f).toInt().coerceIn(1, 100)
            val finalProgress = if (finalCompleted) 100 else calculatedProgress.coerceAtMost(99)

            ReadingHistory(
                bookTitle = bookTitle,
                subtitle = subtitle,
                progressPercent = finalProgress,
                dateText = dateText,
                surahOrChapter = surahOrChapter,
                pagesRead = finalPagesRead,
                isCompleted = finalCompleted,
                contemplationMinutes = contemplationMinutes
            )
        }
        val rowId = readingHistoryDao.insertOrUpdateHistory(historyToSave)
        val userId = getUserId()
        if (userId != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val docId = if (historyToSave.id != 0) historyToSave.id.toString() else rowId.toString()
            val data = hashMapOf(
                "id" to docId.toInt(),
                "bookTitle" to historyToSave.bookTitle,
                "subtitle" to historyToSave.subtitle,
                "progressPercent" to historyToSave.progressPercent,
                "dateText" to historyToSave.dateText,
                "surahOrChapter" to (historyToSave.surahOrChapter ?: ""),
                "pagesRead" to historyToSave.pagesRead,
                "isCompleted" to historyToSave.isCompleted,
                "contemplationMinutes" to historyToSave.contemplationMinutes
            )
            db.collection("users").document(userId)
                .collection("reading_histories").document(docId)
                .set(data)
        }
    }

    suspend fun deleteHistory(id: Int) {
        readingHistoryDao.deleteHistoryById(id)
        val userId = getUserId()
        if (userId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("reading_histories").document(id.toString())
                .delete()
        }
    }

    suspend fun clearAllUserData() {
        readingHistoryDao.clearReadingHistory()
        noteHighlightDao.clearNotesHighlights()
    }

    // Since the user wants a real app where all new accounts start at 0, 
    // we keep this method empty so no mock data is automatically injected.
    suspend fun prepopulateIfEmpty() {
        // No mock prepopulation to ensure clean slate of 0
    }
}
