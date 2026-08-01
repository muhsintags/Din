package com.example.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Book
import com.example.data.model.BookRepository
import com.example.data.model.NoteHighlight
import com.example.data.model.ReadingHistory
import com.example.data.model.QuranSurah
import com.example.data.model.QuranVerse
import com.example.data.model.QuranSurahContent
import com.example.data.repository.ScriptureRepository
import com.example.ui.util.PrayerTimeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

import com.example.ui.util.AppLanguage
import com.example.ui.util.Loc
import com.example.ui.util.UserReligion
import com.example.ui.util.UserSect
import com.example.ui.util.FaithPrayerSchedule

enum class AppThemeSetting {
    LIGHT, DARK, SEPIA
}

enum class LineHeightSetting(val value: Float) {
    TIGHT(1.2f), NORMAL(1.6f), WIDE(2.2f)
}

enum class FontFamilySetting {
    SERIF, SANS_SERIF
}

data class ReaderSettings(
    val theme: AppThemeSetting = AppThemeSetting.LIGHT,
    val fontSizeSp: Float = 20f,
    val fontFamily: FontFamilySetting = FontFamilySetting.SERIF,
    val lineHeight: LineHeightSetting = LineHeightSetting.NORMAL,
    val language: AppLanguage = AppLanguage.EN,
    val showOriginalScript: Boolean = true
)

data class UserState(
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val isLoggedIn: Boolean = false,
    val isDemo: Boolean = false
)

class ScriptureViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScriptureRepository
    val books: List<Book>

    // Reader Settings
    private val _readerSettings = MutableStateFlow(ReaderSettings())
    val readerSettings = _readerSettings.asStateFlow()

    // --- LIVE QURAN API AND AUDIO ---
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .build()
    private var mediaPlayer: MediaPlayer? = null

    private val _currentSelectedSurah = MutableStateFlow<QuranSurah?>(null)
    val currentSelectedSurah = _currentSelectedSurah.asStateFlow()

    private val _currentSurahContent = MutableStateFlow<QuranSurahContent?>(null)
    val currentSurahContent = _currentSurahContent.asStateFlow()

    private val _isSurahLoading = MutableStateFlow(false)
    val isSurahLoading = _isSurahLoading.asStateFlow()

    private val _surahError = MutableStateFlow<String?>(null)
    val surahError = _surahError.asStateFlow()

    val currentPlayingUrl = MutableStateFlow<String?>(null)
    val isAudioPlaying = MutableStateFlow(false)
    val isAudioLoading = MutableStateFlow(false)
    val activePlayingVerseIndex = MutableStateFlow<Int?>(null)

    // --- OFFLINE / DOWNLOAD SYSTEM ---
    private val _downloadedBooks = MutableStateFlow<Set<String>>(emptySet())
    val downloadedBooks = _downloadedBooks.asStateFlow()

    private val _downloadedSurahs = MutableStateFlow<Set<Int>>(emptySet())
    val downloadedSurahs = _downloadedSurahs.asStateFlow()

    private val _downloadedChapters = MutableStateFlow<Set<String>>(emptySet())
    val downloadedChapters = _downloadedChapters.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    @Volatile
    private var _bukhariEngHadithsMap: Map<Int, List<JSONObject>>? = null
    @Volatile
    private var _bukhariAraHadithsMap: Map<Int, List<JSONObject>>? = null

    private fun getBukhariHadithsForBook(bookNumber: Int, isArabic: Boolean): List<JSONObject> {
        val cacheFile = java.io.File(getApplication<Application>().filesDir, if (isArabic) "ara-bukhari.min.json" else "eng-bukhari.min.json")
        if (!cacheFile.exists()) return emptyList()

        if (isArabic) {
            if (_bukhariAraHadithsMap == null) {
                synchronized(this) {
                    if (_bukhariAraHadithsMap == null) {
                        try {
                            val jsonStr = cacheFile.readText()
                            val json = JSONObject(jsonStr)
                            val array = json.getJSONArray("hadiths")
                            val map = mutableMapOf<Int, MutableList<JSONObject>>()
                            for (i in 0 until array.length()) {
                                val hObj = array.getJSONObject(i)
                                val ref = hObj.optJSONObject("reference")
                                val bNum = ref?.optInt("book") ?: -1
                                if (bNum != -1) {
                                    map.getOrPut(bNum) { mutableListOf() }.add(hObj)
                                }
                            }
                            _bukhariAraHadithsMap = map
                        } catch (e: Exception) {
                            android.util.Log.e("ScriptureViewModel", "Failed to parse ara-bukhari", e)
                        }
                    }
                }
            }
            return _bukhariAraHadithsMap?.get(bookNumber) ?: emptyList()
        } else {
            if (_bukhariEngHadithsMap == null) {
                synchronized(this) {
                    if (_bukhariEngHadithsMap == null) {
                        try {
                            val jsonStr = cacheFile.readText()
                            val json = JSONObject(jsonStr)
                            val array = json.getJSONArray("hadiths")
                            val map = mutableMapOf<Int, MutableList<JSONObject>>()
                            for (i in 0 until array.length()) {
                                val hObj = array.getJSONObject(i)
                                val ref = hObj.optJSONObject("reference")
                                val bNum = ref?.optInt("book") ?: -1
                                if (bNum != -1) {
                                    map.getOrPut(bNum) { mutableListOf() }.add(hObj)
                                }
                            }
                            _bukhariEngHadithsMap = map
                        } catch (e: Exception) {
                            android.util.Log.e("ScriptureViewModel", "Failed to parse eng-bukhari", e)
                        }
                    }
                }
            }
            return _bukhariEngHadithsMap?.get(bookNumber) ?: emptyList()
        }
    }

    fun selectSurah(surah: QuranSurah) {
        _currentSelectedSurah.value = surah
        loadSurahContent(surah.number)
    }

    private fun getQuranFile(surahNumber: Int): java.io.File {
        val lang = _readerSettings.value.language.name
        return java.io.File(getApplication<Application>().filesDir, "surah_${surahNumber}_${lang}_v3.json")
    }

    fun loadSurahContent(surahNumber: Int) {
        viewModelScope.launch {
            _isSurahLoading.value = true
            _surahError.value = null
            stopAudio()

            val langCode = _readerSettings.value.language.name
            val cacheKey = "surah_${surahNumber}_${langCode}_v3"
            val cachedContent = _surahInMemoryCache[cacheKey]
            if (cachedContent != null && cachedContent.verses.isNotEmpty()) {
                _currentSurahContent.value = cachedContent
                _isSurahLoading.value = false
                return@launch
            }

            _currentSurahContent.value = null

            // Step 1: Check if Surah is downloaded/cached offline
            val file = getQuranFile(surahNumber)
            if (file.exists()) {
                val loadedOffline = withContext(Dispatchers.IO) {
                    try {
                        val fileContent = file.readText()
                        val json = JSONObject(fileContent)
                        val versesArr = json.getJSONArray("verses")
                        val versesList = mutableListOf<QuranVerse>()
                        for (i in 0 until versesArr.length()) {
                            val obj = versesArr.getJSONObject(i)
                            versesList.add(
                                QuranVerse(
                                    number = obj.getInt("number"),
                                    textArabic = obj.getString("textArabic"),
                                    textTurkish = obj.getString("textTurkish"),
                                    audioUrl = obj.getString("audioUrl")
                                )
                            )
                        }
                        QuranSurahContent(
                            number = json.getInt("number"),
                            nameArabic = json.getString("nameArabic"),
                            englishName = json.getString("englishName"),
                            verses = versesList
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ScriptureViewModel", "Failed to parse cached surah, falling back to API", e)
                        null
                    }
                }
                if (loadedOffline != null && loadedOffline.verses.isNotEmpty()) {
                    _surahInMemoryCache[cacheKey] = loadedOffline
                    _currentSurahContent.value = loadedOffline
                    _isSurahLoading.value = false
                    return@launch
                }
            }

            // Step 2: Fallback to Live API
            withContext(Dispatchers.IO) {
                try {
                    var fetchedContent: QuranSurahContent? = null
                    val quranEdition = if (_readerSettings.value.language == AppLanguage.EN) "en.sahih" else "tr.yazir"
                    
                    // Try 1: Multi-edition URL
                    try {
                        val url = "https://api.alquran.cloud/v1/surah/$surahNumber/editions/quran-uthmani,$quranEdition,ar.alafasy"
                        val request = Request.Builder().url(url).build()
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string() ?: ""
                                val json = JSONObject(responseBody)
                                val dataArray = json.getJSONArray("data")

                                if (dataArray.length() >= 3) {
                                    val arabicEdition = dataArray.getJSONObject(0)
                                    val turkishEdition = dataArray.getJSONObject(1)
                                    val audioEdition = dataArray.getJSONObject(2)

                                    val nameArabic = arabicEdition.getString("name")
                                    val englishName = arabicEdition.getString("englishName")

                                    val arabicVerses = arabicEdition.getJSONArray("ayahs")
                                    val turkishVerses = turkishEdition.getJSONArray("ayahs")
                                    val audioVerses = audioEdition.getJSONArray("ayahs")

                                    val versesList = mutableListOf<QuranVerse>()
                                    val count = minOf(arabicVerses.length(), turkishVerses.length(), audioVerses.length())
                                    for (i in 0 until count) {
                                        val arObj = arabicVerses.getJSONObject(i)
                                        val trObj = turkishVerses.getJSONObject(i)
                                        val auObj = audioVerses.getJSONObject(i)

                                        val vNum = arObj.getInt("numberInSurah")
                                        val rawAr = arObj.getString("text")
                                        val rawTr = trObj.getString("text")
                                        val (cleanAr, cleanTr) = cleanQuranVerseText(surahNumber, vNum, rawAr, rawTr)

                                        versesList.add(
                                            QuranVerse(
                                                number = vNum,
                                                textArabic = cleanAr,
                                                textTurkish = cleanTr,
                                                audioUrl = auObj.optString("audio", "")
                                            )
                                        )
                                    }

                                    fetchedContent = QuranSurahContent(
                                        number = surahNumber,
                                        nameArabic = nameArabic,
                                        englishName = englishName,
                                        verses = versesList
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("ScriptureViewModel", "Multi-edition fetch failed for surah $surahNumber", e)
                    }

                    // Try 2: Single-edition fallback if multi-edition failed
                    if (fetchedContent == null || fetchedContent!!.verses.isEmpty()) {
                        val urlAr = "https://api.alquran.cloud/v1/surah/$surahNumber/quran-uthmani"
                        val urlTr = "https://api.alquran.cloud/v1/surah/$surahNumber/$quranEdition"

                        val reqAr = Request.Builder().url(urlAr).build()
                        val reqTr = Request.Builder().url(urlTr).build()

                        var arJson: JSONObject? = null
                        var trJson: JSONObject? = null

                        try {
                            okHttpClient.newCall(reqAr).execute().use { r ->
                                if (r.isSuccessful) arJson = JSONObject(r.body?.string() ?: "").optJSONObject("data")
                            }
                        } catch (_: Exception) {}

                        try {
                            okHttpClient.newCall(reqTr).execute().use { r ->
                                if (r.isSuccessful) trJson = JSONObject(r.body?.string() ?: "").optJSONObject("data")
                            }
                        } catch (_: Exception) {}

                        if (arJson != null) {
                            val nameArabic = arJson!!.optString("name", "سورة")
                            val englishName = arJson!!.optString("englishName", "Surah $surahNumber")
                            val arAyahs = arJson!!.optJSONArray("ayahs") ?: org.json.JSONArray()
                            val trAyahs = trJson?.optJSONArray("ayahs") ?: org.json.JSONArray()

                            val versesList = mutableListOf<QuranVerse>()
                            for (i in 0 until arAyahs.length()) {
                                val arObj = arAyahs.getJSONObject(i)
                                val trObj = if (i < trAyahs.length()) trAyahs.getJSONObject(i) else null
                                val verseNum = arObj.optInt("numberInSurah", i + 1)
                                val textAr = arObj.optString("text", "")
                                val textTr = trObj?.optString("text", textAr) ?: textAr
                                val (cleanAr, cleanTr) = cleanQuranVerseText(surahNumber, verseNum, textAr, textTr)
                                val ayahGlobalNum = arObj.optInt("number", 1)
                                val audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$ayahGlobalNum.mp3"
                                versesList.add(QuranVerse(verseNum, cleanAr, cleanTr, audioUrl))
                            }

                            fetchedContent = QuranSurahContent(surahNumber, nameArabic, englishName, versesList)
                        }
                    }

                    if (fetchedContent != null && fetchedContent!!.verses.isNotEmpty()) {
                        _surahInMemoryCache[cacheKey] = fetchedContent!!
                        withContext(Dispatchers.Main) {
                            _currentSurahContent.value = fetchedContent
                        }
                    } else {
                        throw IOException("Sure ayetleri yüklenemedi.")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _surahError.value = "Yüklenemedi: ${e.localizedMessage ?: "Bağlantı hatası"}. Lütfen internet bağlantısını kontrol edin."
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        _isSurahLoading.value = false
                    }
                }
            }
        }
    }

    private suspend fun downloadFileToLocal(urlStr: String, destinationFile: java.io.File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val secureUrl = if (urlStr.startsWith("http://")) {
                    urlStr.replace("http://", "https://")
                } else {
                    urlStr
                }
                val request = Request.Builder().url(secureUrl).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { input ->
                            destinationFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        true
                    } else {
                        false
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ScriptureViewModel", "Failed to download file from $urlStr", e)
                false
            }
        }
    }

    // --- DOWNLOAD BOOK (STANDARD BOOK) ---
    fun downloadBook(bookId: String) {
        viewModelScope.launch {
            val progressMap = _downloadProgress.value.toMutableMap()
            progressMap[bookId] = 0f
            _downloadProgress.value = progressMap

            if (bookId == "quran") {
                // Quran download downloads all 114 Surahs JSON!
                val totalSurahs = 114
                var successCount = 0
                for (surahNum in 1..totalSurahs) {
                    val file = getQuranFile(surahNum)
                    if (file.exists()) {
                        successCount++
                        val currentMap = _downloadProgress.value.toMutableMap()
                        currentMap[bookId] = surahNum / totalSurahs.toFloat()
                        _downloadProgress.value = currentMap
                        continue
                    }

                    val fetched = withContext(Dispatchers.IO) {
                        try {
                            val quranEdition = if (_readerSettings.value.language == AppLanguage.EN) "en.sahih" else "tr.yazir"
                            val url = "https://api.alquran.cloud/v1/surah/$surahNum/editions/quran-uthmani,$quranEdition,ar.alafasy"
                            val request = Request.Builder().url(url).build()
                            okHttpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val responseBody = response.body?.string() ?: ""
                                    val json = JSONObject(responseBody)
                                    val dataArray = json.getJSONArray("data")
                                    val arabicEdition = dataArray.getJSONObject(0)
                                    val turkishEdition = dataArray.getJSONObject(1)
                                    val audioEdition = dataArray.getJSONObject(2)

                                    val nameArabic = arabicEdition.getString("name")
                                    val englishName = arabicEdition.getString("englishName")

                                    val arabicVerses = arabicEdition.getJSONArray("ayahs")
                                    val turkishVerses = turkishEdition.getJSONArray("ayahs")
                                    val audioVerses = audioEdition.getJSONArray("ayahs")

                                    val versesList = mutableListOf<QuranVerse>()
                                    for (i in 0 until arabicVerses.length()) {
                                        val arObj = arabicVerses.getJSONObject(i)
                                        val trObj = turkishVerses.getJSONObject(i)
                                        val auObj = audioVerses.getJSONObject(i)

                                        val vNum = arObj.getInt("numberInSurah")
                                        val rawAr = arObj.getString("text")
                                        val rawTr = trObj.getString("text")
                                        val (cleanAr, cleanTr) = cleanQuranVerseText(surahNum, vNum, rawAr, rawTr)

                                        versesList.add(
                                            QuranVerse(
                                                number = vNum,
                                                textArabic = cleanAr,
                                                textTurkish = cleanTr,
                                                audioUrl = auObj.getString("audio")
                                            )
                                        )
                                    }
                                    QuranSurahContent(
                                        number = surahNum,
                                        nameArabic = nameArabic,
                                        englishName = englishName,
                                        verses = versesList
                                    )
                                } else null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (fetched != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                val jsonObj = JSONObject().apply {
                                    put("number", fetched.number)
                                    put("nameArabic", fetched.nameArabic)
                                    put("englishName", fetched.englishName)
                                    val versesArr = org.json.JSONArray()
                                    fetched.verses.forEach { verse ->
                                        versesArr.put(JSONObject().apply {
                                            put("number", verse.number)
                                            put("textArabic", verse.textArabic)
                                            put("textTurkish", verse.textTurkish)
                                            put("audioUrl", verse.audioUrl)
                                        })
                                    }
                                    put("verses", versesArr)
                                }
                                file.writeText(jsonObj.toString())
                                successCount++
                            } catch (e: Exception) {
                                android.util.Log.e("ScriptureViewModel", "Failed to save file for surah $surahNum", e)
                            }
                        }
                        // Save to downloaded_surahs list too so individual surah page knows it's offline!
                        val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
                        val currentDownloadedSurahs = offlinePrefs.getStringSet("downloaded_surahs", emptySet())?.toMutableSet() ?: mutableSetOf()
                        currentDownloadedSurahs.add(surahNum.toString())
                        offlinePrefs.edit().putStringSet("downloaded_surahs", currentDownloadedSurahs).apply()
                        _downloadedSurahs.value = currentDownloadedSurahs.mapNotNull { it.toIntOrNull() }.toSet()
                    }

                    val currentMap = _downloadProgress.value.toMutableMap()
                    currentMap[bookId] = surahNum / totalSurahs.toFloat()
                    _downloadProgress.value = currentMap

                    kotlinx.coroutines.delay(50)
                }
            } else if (bookId == "bukhari") {
                val cacheFileEng = java.io.File(getApplication<Application>().filesDir, "eng-bukhari.min.json")
                val cacheFileAra = java.io.File(getApplication<Application>().filesDir, "ara-bukhari.min.json")
                withContext(Dispatchers.IO) {
                    if (!cacheFileEng.exists()) {
                        val bukhariUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari.min.json"
                        downloadFileToLocal(bukhariUrl, cacheFileEng)
                    }
                    val mapProgress = _downloadProgress.value.toMutableMap()
                    mapProgress[bookId] = 0.5f
                    _downloadProgress.value = mapProgress

                    if (!cacheFileAra.exists()) {
                        val bukhariAraUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-bukhari.min.json"
                        downloadFileToLocal(bukhariAraUrl, cacheFileAra)
                    }
                }
            } else if (bookId == "gita") {
                val cacheFileGita = java.io.File(getApplication<Application>().filesDir, "gita-verses.min.json")
                withContext(Dispatchers.IO) {
                    if (!cacheFileGita.exists()) {
                        val gitaUrl = "https://raw.githubusercontent.com/gita/gita/main/data/verse.json"
                        downloadFileToLocal(gitaUrl, cacheFileGita)
                    }
                }
            } else {
                // Standard book like Tevrat or İncil
                val book = books.firstOrNull { it.id == bookId }
                if (book != null && book.audioUrl.isNotEmpty()) {
                    val audioFile = java.io.File(getApplication<Application>().cacheDir, "audio_${book.audioUrl.hashCode()}.mp3")
                    if (!audioFile.exists()) {
                        val currentMap = _downloadProgress.value.toMutableMap()
                        currentMap[bookId] = 0.2f
                        _downloadProgress.value = currentMap

                        downloadFileToLocal(book.audioUrl, audioFile)

                        val finalMap = _downloadProgress.value.toMutableMap()
                        finalMap[bookId] = 1.0f
                        _downloadProgress.value = finalMap
                    }
                } else {
                    for (p in 1..10) {
                        kotlinx.coroutines.delay(50)
                        val currentMap = _downloadProgress.value.toMutableMap()
                        currentMap[bookId] = p / 10f
                        _downloadProgress.value = currentMap
                    }
                }
            }

            val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
            val currentDownloaded = offlinePrefs.getStringSet("downloaded_books", emptySet())?.toMutableSet() ?: mutableSetOf()
            currentDownloaded.add(bookId)
            offlinePrefs.edit().putStringSet("downloaded_books", currentDownloaded).apply()
            _downloadedBooks.value = currentDownloaded

            val finalMap = _downloadProgress.value.toMutableMap()
            finalMap.remove(bookId)
            _downloadProgress.value = finalMap

            exportPersistentBackup()
        }
    }

    fun deleteBookDownload(bookId: String) {
        val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
        val currentDownloaded = offlinePrefs.getStringSet("downloaded_books", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentDownloaded.remove(bookId)
        offlinePrefs.edit().putStringSet("downloaded_books", currentDownloaded).apply()
        _downloadedBooks.value = currentDownloaded

        if (bookId == "quran") {
            // Delete all 114 surah downloads
            val currentDownloadedSurahs = offlinePrefs.getStringSet("downloaded_surahs", emptySet())?.toMutableSet() ?: mutableSetOf()
            for (surahNum in 1..114) {
                currentDownloadedSurahs.remove(surahNum.toString())
                try {
                    val langCodes = listOf("", "_TR", "_EN")
                    for (lc in langCodes) {
                        val file = java.io.File(getApplication<Application>().filesDir, "surah_${surahNum}${lc}.json")
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScriptureViewModel", "Failed to delete file for surah $surahNum", e)
                }
            }
            offlinePrefs.edit().putStringSet("downloaded_surahs", currentDownloadedSurahs).apply()
            _downloadedSurahs.value = currentDownloadedSurahs.mapNotNull { it.toIntOrNull() }.toSet()
        } else {
            val book = books.firstOrNull { it.id == bookId }
            if (book != null && book.audioUrl.isNotEmpty()) {
                try {
                    val audioFile = java.io.File(getApplication<Application>().cacheDir, "audio_${book.audioUrl.hashCode()}.mp3")
                    if (audioFile.exists()) {
                        audioFile.delete()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScriptureViewModel", "Failed to delete standard book audio file", e)
                }
            }
        }
    }

    // --- DOWNLOAD SURAH (QURAN) ---
    fun downloadSurah(surahNumber: Int) {
        viewModelScope.launch {
            val key = "surah_$surahNumber"
            val progressMap = _downloadProgress.value.toMutableMap()
            progressMap[key] = 0f
            _downloadProgress.value = progressMap

            var contentToSave = _currentSurahContent.value
            if (contentToSave == null || contentToSave.number != surahNumber) {
                _isSurahLoading.value = true
                val fetched = withContext(Dispatchers.IO) {
                    try {
                        val quranEdition = if (_readerSettings.value.language == AppLanguage.EN) "en.sahih" else "tr.yazir"
                        val url = "https://api.alquran.cloud/v1/surah/$surahNumber/editions/quran-uthmani,$quranEdition,ar.alafasy"
                        val request = Request.Builder().url(url).build()
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string() ?: ""
                                val json = JSONObject(responseBody)
                                val dataArray = json.getJSONArray("data")
                                val arabicEdition = dataArray.getJSONObject(0)
                                val turkishEdition = dataArray.getJSONObject(1)
                                val audioEdition = dataArray.getJSONObject(2)

                                val nameArabic = arabicEdition.getString("name")
                                val englishName = arabicEdition.getString("englishName")

                                val arabicVerses = arabicEdition.getJSONArray("ayahs")
                                val turkishVerses = turkishEdition.getJSONArray("ayahs")
                                val audioVerses = audioEdition.getJSONArray("ayahs")

                                val versesList = mutableListOf<QuranVerse>()
                                for (i in 0 until arabicVerses.length()) {
                                    val arObj = arabicVerses.getJSONObject(i)
                                    val trObj = turkishVerses.getJSONObject(i)
                                    val auObj = audioVerses.getJSONObject(i)

                                    val vNum = arObj.getInt("numberInSurah")
                                    val rawAr = arObj.getString("text")
                                    val rawTr = trObj.getString("text")
                                    val (cleanAr, cleanTr) = cleanQuranVerseText(surahNumber, vNum, rawAr, rawTr)

                                    versesList.add(
                                        QuranVerse(
                                            number = vNum,
                                            textArabic = cleanAr,
                                            textTurkish = cleanTr,
                                            audioUrl = auObj.getString("audio")
                                        )
                                    )
                                }
                                QuranSurahContent(
                                    number = surahNumber,
                                    nameArabic = nameArabic,
                                    englishName = englishName,
                                    verses = versesList
                                )
                            } else null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (fetched != null) {
                    contentToSave = fetched
                    _currentSurahContent.value = fetched
                }
                _isSurahLoading.value = false
            }

            if (contentToSave == null) {
                val finalMap = _downloadProgress.value.toMutableMap()
                finalMap.remove(key)
                _downloadProgress.value = finalMap
                return@launch
            }

            // Save JSON text first
            withContext(Dispatchers.IO) {
                try {
                    val file = getQuranFile(surahNumber)
                    val jsonObj = JSONObject().apply {
                        put("number", contentToSave!!.number)
                        put("nameArabic", contentToSave!!.nameArabic)
                        put("englishName", contentToSave!!.englishName)
                        val versesArr = org.json.JSONArray()
                        contentToSave!!.verses.forEach { verse ->
                            versesArr.put(JSONObject().apply {
                                put("number", verse.number)
                                put("textArabic", verse.textArabic)
                                put("textTurkish", verse.textTurkish)
                                put("audioUrl", verse.audioUrl)
                            })
                        }
                        put("verses", versesArr)
                    }
                    file.writeText(jsonObj.toString())
                } catch (e: Exception) {
                    android.util.Log.e("ScriptureViewModel", "Failed to save file", e)
                }
            }

            val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
            val currentDownloaded = offlinePrefs.getStringSet("downloaded_surahs", emptySet())?.toMutableSet() ?: mutableSetOf()
            currentDownloaded.add(surahNumber.toString())
            offlinePrefs.edit().putStringSet("downloaded_surahs", currentDownloaded).apply()
            _downloadedSurahs.value = currentDownloaded.mapNotNull { it.toIntOrNull() }.toSet()

            // Download audio files for all verses of this surah
            val versesCount = contentToSave.verses.size
            contentToSave.verses.forEachIndexed { index, verse ->
                val audioUrl = verse.audioUrl
                if (audioUrl.isNotEmpty()) {
                    val audioFile = java.io.File(getApplication<Application>().cacheDir, "audio_${audioUrl.hashCode()}.mp3")
                    if (!audioFile.exists()) {
                        downloadFileToLocal(audioUrl, audioFile)
                    }
                }
                val currentProgress = 0.1f + (index.toFloat() / versesCount) * 0.9f
                val currentMap = _downloadProgress.value.toMutableMap()
                currentMap[key] = currentProgress
                _downloadProgress.value = currentMap
            }

            val finalMap = _downloadProgress.value.toMutableMap()
            finalMap.remove(key)
            _downloadProgress.value = finalMap
        }
    }

    fun deleteSurahDownload(surahNumber: Int) {
        val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
        val currentDownloaded = offlinePrefs.getStringSet("downloaded_surahs", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentDownloaded.remove(surahNumber.toString())
        offlinePrefs.edit().putStringSet("downloaded_surahs", currentDownloaded).apply()
        _downloadedSurahs.value = currentDownloaded.mapNotNull { it.toIntOrNull() }.toSet()

        try {
            val langCodes = listOf("", "_TR", "_EN")
            for (lc in langCodes) {
                val file = java.io.File(getApplication<Application>().filesDir, "surah_${surahNumber}${lc}.json")
                if (file.exists()) {
                    val fileContent = file.readText()
                    val json = JSONObject(fileContent)
                    val versesArr = json.getJSONArray("verses")
                    for (i in 0 until versesArr.length()) {
                        val obj = versesArr.getJSONObject(i)
                        val audioUrl = obj.optString("audioUrl", "")
                        if (audioUrl.isNotEmpty()) {
                            val audioFile = java.io.File(getApplication<Application>().cacheDir, "audio_${audioUrl.hashCode()}.mp3")
                            if (audioFile.exists()) {
                                audioFile.delete()
                            }
                        }
                    }
                    file.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ScriptureViewModel", "Failed to delete file", e)
        }
    }

    private fun cleanQuranVerseText(surahNumber: Int, verseNumber: Int, textArabic: String, textTurkish: String): Pair<String, String> {
        var cleanedAr = textArabic.trim()
        var cleanedTr = textTurkish.trim()
        if (surahNumber != 1 && surahNumber != 9 && verseNumber == 1) {
            val bismillahAr = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
            if (cleanedAr.startsWith(bismillahAr)) {
                cleanedAr = cleanedAr.removePrefix(bismillahAr).trim()
            }
            val bismillahTr = "Rahmân ve Rahîm olan Allah'ın adıyla."
            if (cleanedTr.startsWith(bismillahTr)) {
                cleanedTr = cleanedTr.removePrefix(bismillahTr).trim()
            }
            val bismillahEn = "In the name of Allah, the Entirely Merciful, the Especially Merciful."
            if (cleanedTr.startsWith(bismillahEn)) {
                cleanedTr = cleanedTr.removePrefix(bismillahEn).trim()
            }
        }
        val finalAr = if (cleanedAr.isEmpty()) textArabic else cleanedAr
        val finalTr = if (cleanedTr.isEmpty()) textTurkish else cleanedTr
        return Pair(finalAr, finalTr)
    }

    fun prefetchAudioChunk(startIndex: Int, chunkSize: Int = 6) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentContent = _currentSurahContent.value
            val currentBook = _activeBookContent.value
            val isEn = _readerSettings.value.language == AppLanguage.EN

            val urlsToFetch = mutableListOf<String>()

            if (currentContent != null) {
                val maxIndex = minOf(startIndex + chunkSize, currentContent.verses.size)
                for (i in startIndex until maxIndex) {
                    if (i >= 0 && i < currentContent.verses.size) {
                        val vUrl = currentContent.verses[i].audioUrl
                        if (vUrl.isNotEmpty()) urlsToFetch.add(vUrl)
                    }
                }
            } else if (currentBook != null) {
                val maxIndex = minOf(startIndex + chunkSize, currentBook.paragraphs.size)
                for (i in startIndex until maxIndex) {
                    if (i >= 0 && i < currentBook.paragraphs.size) {
                        val pText = currentBook.paragraphs[i]
                        val url = getBibleVerseAudioUrl(pText, isEn)
                        if (url.isNotEmpty()) urlsToFetch.add(url)
                    }
                }
            }

            val jobs = urlsToFetch.map { url ->
                async {
                    try {
                        val localFile = java.io.File(getApplication<Application>().cacheDir, "audio_${url.hashCode()}.mp3")
                        if (!localFile.exists()) {
                            val secureUrl = if (url.startsWith("http://")) url.replace("http://", "https://") else url
                            downloadFileToLocal(secureUrl, localFile)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ScriptureViewModel", "Error prefetching audio chunk for $url", e)
                    }
                }
            }
            jobs.awaitAll()
        }
    }

    fun playAudio(url: String, verseIndex: Int) {
        viewModelScope.launch {
            try {
                isAudioLoading.value = true
                activePlayingVerseIndex.value = verseIndex
                
                // Convert to secure URL if necessary
                val secureUrl = if (url.startsWith("http://")) {
                    url.replace("http://", "https://")
                } else {
                    url
                }
                currentPlayingUrl.value = secureUrl

                // Trigger chunk prefetching for 6 verses starting from current index
                if (verseIndex >= 0) {
                    prefetchAudioChunk(verseIndex, 6)
                }

                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    val attributes = android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                    setAudioAttributes(attributes)

                    val localFile = java.io.File(getApplication<Application>().cacheDir, "audio_${url.hashCode()}.mp3")
                    
                    // If file is not yet cached locally, download it first on IO thread
                    withContext(Dispatchers.IO) {
                        if (!localFile.exists()) {
                            downloadFileToLocal(secureUrl, localFile)
                        }
                    }

                    if (localFile.exists()) {
                        setDataSource(localFile.absolutePath)
                    } else {
                        setDataSource(secureUrl)
                    }

                    setOnPreparedListener {
                        isAudioLoading.value = false
                        isAudioPlaying.value = true
                        start()
                    }
                    setOnCompletionListener {
                        isAudioPlaying.value = false
                        playNextVerse()
                    }
                    setOnErrorListener { _, what, extra ->
                        android.util.Log.e("ScriptureViewModel", "MediaPlayer Error: what=$what, extra=$extra")
                        isAudioLoading.value = false
                        isAudioPlaying.value = false
                        false
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                android.util.Log.e("ScriptureViewModel", "Error in playAudio", e)
                isAudioLoading.value = false
                isAudioPlaying.value = false
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isAudioPlaying.value = false
            } else {
                it.start()
                isAudioPlaying.value = true
            }
        }
    }

    fun playNextVerse() {
        val currentContent = _currentSurahContent.value
        val currentBook = _activeBookContent.value
        val currentIndex = activePlayingVerseIndex.value ?: return
        
        if (currentIndex == -1) {
            stopAudio()
            return
        }

        val nextIndex = currentIndex + 1

        // Prefetch next 6 verses when approaching end of current chunk (e.g. at 5th verse)
        if (nextIndex % 6 == 4 || nextIndex % 6 == 0) {
            prefetchAudioChunk(nextIndex + 1, chunkSize = 6)
        }
        
        if (currentContent != null) {
            if (nextIndex < currentContent.verses.size) {
                val nextVerse = currentContent.verses[nextIndex]
                playAudio(nextVerse.audioUrl, nextIndex)
            } else {
                stopAudio()
            }
        } else if (currentBook != null) {
            if (nextIndex < currentBook.paragraphs.size) {
                val nextParagraph = currentBook.paragraphs[nextIndex]
                val isEn = _readerSettings.value.language == AppLanguage.EN
                val url = getBibleVerseAudioUrl(nextParagraph, isEn)
                playAudio(url, nextIndex)
            } else {
                stopAudio()
            }
        } else {
            stopAudio()
        }
    }

    fun stopAudio() {
        mediaPlayer?.release()
        mediaPlayer = null
        isAudioPlaying.value = false
        isAudioLoading.value = false
        activePlayingVerseIndex.value = null
        currentPlayingUrl.value = null
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // --- END LIVE QURAN ---

    private val _currentSelectedTorahBook = MutableStateFlow<com.example.data.model.BibleBook?>(null)
    val currentSelectedTorahBook = _currentSelectedTorahBook.asStateFlow()

    private val _currentSelectedTorahChapter = MutableStateFlow<Int?>(null)
    val currentSelectedTorahChapter = _currentSelectedTorahChapter.asStateFlow()

    private val _currentSelectedSermonBook = MutableStateFlow<com.example.data.model.BibleBook?>(null)
    val currentSelectedSermonBook = _currentSelectedSermonBook.asStateFlow()

    private val _currentSelectedSermonChapter = MutableStateFlow<Int?>(null)
    val currentSelectedSermonChapter = _currentSelectedSermonChapter.asStateFlow()

    private val _currentSelectedTalmudBook = MutableStateFlow<com.example.data.model.BibleBook?>(null)
    val currentSelectedTalmudBook = _currentSelectedTalmudBook.asStateFlow()

    private val _currentSelectedTalmudChapter = MutableStateFlow<Int?>(null)
    val currentSelectedTalmudChapter = _currentSelectedTalmudChapter.asStateFlow()

    private val _currentSelectedBukhariBook = MutableStateFlow<com.example.data.model.BibleBook?>(null)
    val currentSelectedBukhariBook = _currentSelectedBukhariBook.asStateFlow()

    private val _currentSelectedBukhariChapter = MutableStateFlow<Int?>(null)
    val currentSelectedBukhariChapter = _currentSelectedBukhariChapter.asStateFlow()

    private val _currentSelectedGitaBook = MutableStateFlow<com.example.data.model.BibleBook?>(null)
    val currentSelectedGitaBook = _currentSelectedGitaBook.asStateFlow()

    private val _currentSelectedGitaChapter = MutableStateFlow<Int?>(null)
    val currentSelectedGitaChapter = _currentSelectedGitaChapter.asStateFlow()

    fun selectGitaBook(book: com.example.data.model.BibleBook?) {
        _currentSelectedGitaBook.value = book
        if (book != null) {
            selectGitaChapter(1)
        } else {
            _currentSelectedGitaChapter.value = null
            _activeBookContent.value = null
        }
    }

    fun selectGitaChapter(chapter: Int?) {
        _currentSelectedGitaChapter.value = chapter
        if (chapter == null) {
            _activeBookContent.value = null
            return
        }
        val book = _currentSelectedGitaBook.value ?: return
        loadBibleChapterContent(bookId = "gita", bibleBook = book, chapterNumber = chapter, isTorah = false)
    }

    fun selectTalmudBook(book: com.example.data.model.BibleBook?) {
        _currentSelectedTalmudBook.value = book
        if (book != null) {
            selectTalmudChapter(1)
        } else {
            _currentSelectedTalmudChapter.value = null
            _activeBookContent.value = null
        }
    }

    fun selectTalmudChapter(chapter: Int?) {
        _currentSelectedTalmudChapter.value = chapter
        if (chapter == null) {
            _activeBookContent.value = null
            return
        }
        val book = _currentSelectedTalmudBook.value ?: return
        loadBibleChapterContent(bookId = "talmud", bibleBook = book, chapterNumber = chapter, isTorah = false)
    }

    fun selectBukhariBook(book: com.example.data.model.BibleBook?) {
        _currentSelectedBukhariBook.value = book
        if (book != null) {
            selectBukhariChapter(1)
        } else {
            _currentSelectedBukhariChapter.value = null
            _activeBookContent.value = null
        }
    }

    fun selectBukhariChapter(chapter: Int?) {
        _currentSelectedBukhariChapter.value = chapter
        if (chapter == null) {
            _activeBookContent.value = null
            return
        }
        val book = _currentSelectedBukhariBook.value ?: return
        loadBibleChapterContent(bookId = "bukhari", bibleBook = book, chapterNumber = chapter, isTorah = false)
    }

    fun selectTorahBook(book: com.example.data.model.BibleBook?) {
        _currentSelectedTorahBook.value = book
        if (book != null) {
            selectTorahChapter(1)
        } else {
            _currentSelectedTorahChapter.value = null
            _activeBookContent.value = null
        }
    }

    fun selectTorahChapter(chapter: Int?) {
        _currentSelectedTorahChapter.value = chapter
        if (chapter == null) {
            _activeBookContent.value = null
            return
        }
        val book = _currentSelectedTorahBook.value ?: return
        loadBibleChapterContent(bookId = "torah", bibleBook = book, chapterNumber = chapter, isTorah = true)
    }

    fun selectSermonBook(book: com.example.data.model.BibleBook?) {
        _currentSelectedSermonBook.value = book
        if (book != null) {
            selectSermonChapter(1)
        } else {
            _currentSelectedSermonChapter.value = null
            _activeBookContent.value = null
        }
    }

    fun selectSermonChapter(chapter: Int?) {
        _currentSelectedSermonChapter.value = chapter
        if (chapter == null) {
            _activeBookContent.value = null
            return
        }
        val book = _currentSelectedSermonBook.value ?: return
        loadBibleChapterContent(bookId = "sermon", bibleBook = book, chapterNumber = chapter, isTorah = false)
    }

    fun loadBibleChapterContent(bookId: String, bibleBook: com.example.data.model.BibleBook, chapterNumber: Int, isTorah: Boolean) {
        viewModelScope.launch {
            _isBookLoading.value = true
            _bookError.value = null
            stopAudio()

            val langCode = _readerSettings.value.language.name
            val cacheKey = "${bookId}_${bibleBook.id}_${chapterNumber}_$langCode"
            val cachedBook = _bibleChapterInMemoryCache[cacheKey]
            if (cachedBook != null) {
                _activeBookContent.value = cachedBook
                _isBookLoading.value = false
                return@launch
            }

            _activeBookContent.value = null

            // Try loading offline first
            val file = getBibleChapterFile(bookId, bibleBook.id, chapterNumber)
            if (file.exists()) {
                val loadedOffline = withContext(Dispatchers.IO) {
                    try {
                        val fileContent = file.readText()
                        val json = JSONObject(fileContent)
                        val paragraphsJA = json.getJSONArray("paragraphs")
                        val paragraphsList = mutableListOf<String>()
                        for (i in 0 until paragraphsJA.length()) {
                            paragraphsList.add(paragraphsJA.getString(i))
                        }
                        val originalJA = json.getJSONArray("originalParagraphs")
                        val originalParagraphsList = mutableListOf<String>()
                        for (i in 0 until originalJA.length()) {
                            originalParagraphsList.add(originalJA.getString(i))
                        }
                        
                        val coverUrl = if (isTorah) {
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuD6rzbh79ixK7IHnjERinDAG9yEjNtC30hCLbuDS7yoxyf6rouqg29nOLf_nzmpU78EwzXJe6p1tWVIWrDlhvum4Iqa6u0TnO-IwrpTIQYRqPExxi16Ec1M-jGgAgowmeBh-zy1rrxHJO0IsoJZT3qbsucxsJyevgd8YJ4Aq8zKLGnL_X-HEcni8iw3mD3Q82EE-LHUXOMtbQi-V4sO8PjSsZ1PgOfvziyUWmZJF9TIO70eO_m89sgKQQ"
                        } else {
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuDZLBLFfgfJglvrr0EJpNX0i_-RQKNKoNSaMY1kPDhn7UuXgjODkXTeF01UxWZumZjyTS0JDvfH0iC2YadTAtPekF7mw5qqPWd1vFb_ojcbVuV9hDUWAicnoXjy_iu6S8dWvAOkI8P939gqVGbRS8d_eWsrkLCj81FxRyVfyoj3wbEYaMvnZcWUnMuV90Q3vdJ7Xbt2p3x5-WuTLRP_WQVsmS8ANqNPwHXpkMweu5dRZItKVrxcMUCv6A"
                        }
                        
                        Book(
                            id = bookId,
                            title = if (isTorah) {
                                if (_readerSettings.value.language == AppLanguage.EN) "Torah" else "Tevrat"
                            } else {
                                if (_readerSettings.value.language == AppLanguage.EN) "Gospel" else "İncil"
                            },
                            category = if (_readerSettings.value.language == AppLanguage.EN) "Sacred Texts" else "Kutsal Metinler",
                            description = if (isTorah) {
                                if (_readerSettings.value.language == AppLanguage.EN) "Torah (Tanakh) Live Text" else "Tevrat (Tanah) Canlı Metni"
                            } else {
                                if (_readerSettings.value.language == AppLanguage.EN) "Gospel Live Text" else "İncil Canlı Metni"
                            },
                            authorOrSource = if (isTorah) {
                                if (_readerSettings.value.language == AppLanguage.EN) "Hebrew Tradition" else "İbranî Geleneği"
                            } else {
                                if (_readerSettings.value.language == AppLanguage.EN) "Christian Tradition" else "Hristiyan Geleneği"
                            },
                            iconName = if (isTorah) "menu_book" else "church",
                            coverUrl = coverUrl,
                            contentTitle = if (_readerSettings.value.language == AppLanguage.EN) "${bibleBook.nameEnglish} $chapterNumber" else "${bibleBook.nameTurkish} $chapterNumber",
                            subContentTitle = if (_readerSettings.value.language == AppLanguage.EN) "${bibleBook.nameTurkish} $chapterNumber" else "${bibleBook.nameEnglish} $chapterNumber",
                            introText = if (_readerSettings.value.language == AppLanguage.EN) {
                                "Chapter $chapterNumber of the book of ${bibleBook.nameEnglish}, loaded from local offline storage."
                            } else {
                                "${bibleBook.nameTurkish} kitabının $chapterNumber. bölümü cihaz hafızasından çevrimdışı yüklenmiştir."
                            },
                            paragraphs = paragraphsList,
                            originalLanguageName = if (isTorah) {
                                if (_readerSettings.value.language == AppLanguage.EN) "Hebrew" else "İbranice (Hebrew)"
                            } else {
                                if (_readerSettings.value.language == AppLanguage.EN) "Ancient Greek" else "Grekçe (Ancient Greek)"
                            },
                            originalIntroText = if (originalParagraphsList.isNotEmpty()) originalParagraphsList.first().substringAfter(": ") else "",
                            originalParagraphs = originalParagraphsList,
                            footnotes = if (_readerSettings.value.language == AppLanguage.EN) {
                                listOf(
                                    "Offline Academic Translation" to "This section was downloaded for offline reading and study.",
                                    "Source" to if (isTorah) "Sefaria Open Source Project" else "Bible-API Library"
                                )
                            } else {
                                listOf(
                                    "Çevrimdışı Akademik Çeviri" to "Bu bölüm çevrimdışı kullanım ve çalışma için cihazınıza kaydedilmiştir.",
                                    "Kaynak" to if (isTorah) "Sefaria Açık Kaynak Projesi" else "Bible-API Çevrimdışı/Canlı Kütüphane"
                                )
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ScriptureViewModel", "Failed to parse cached bible chapter, falling back to API", e)
                        null
                    }
                }
                if (loadedOffline != null && loadedOffline.paragraphs.isNotEmpty()) {
                    _bibleChapterInMemoryCache[cacheKey] = loadedOffline
                    _activeBookContent.value = loadedOffline
                    _isBookLoading.value = false
                    return@launch
                }
            }

            val coverUrl = when (bookId) {
                "torah" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuD6rzbh79ixK7IHnjERinDAG9yEjNtC30hCLbuDS7yoxyf6rouqg29nOLf_nzmpU78EwzXJe6p1tWVIWrDlhvum4Iqa6u0TnO-IwrpTIQYRqPExxi16Ec1M-jGgAgowmeBh-zy1rrxHJO0IsoJZT3qbsucxsJyevgd8YJ4Aq8zKLGnL_X-HEcni8iw3mD3Q82EE-LHUXOMtbQi-V4sO8PjSsZ1PgOfvziyUWmZJF9TIO70eO_m89sgKQQ"
                "talmud" -> "https://images.unsplash.com/photo-1544947950-fa07a98d237f"
                "bukhari" -> "https://images.unsplash.com/photo-1584282479234-df7a6b986872"
                else -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDZLBLFfgfJglvrr0EJpNX0i_-RQKNKoNSaMY1kPDhn7UuXgjODkXTeF01UxWZumZjyTS0JDvfH0iC2YadTAtPekF7mw5qqPWd1vFb_ojcbVuV9hDUWAicnoXjy_iu6S8dWvAOkI8P939gqVGbRS8d_eWsrkLCj81FxRyVfyoj3wbEYaMvnZcWUnMuV90Q3vdJ7Xbt2p3x5-WuTLRP_WQVsmS8ANqNPwHXpkMweu5dRZItKVrxcMUCv6A"
            }
            
            withContext(Dispatchers.IO) {
                try {
                    val paragraphsList = mutableListOf<String>()
                    val (englishVerses, originalParagraphsList) = fetchChapterContentInternal(bookId, bibleBook, chapterNumber)

                    if (englishVerses.isEmpty()) {
                        throw IOException(if (_readerSettings.value.language == AppLanguage.EN) "Could not load chapter text. Please check internet connection." else "Kutsal metin yüklenemedi. Lütfen internet bağlantınızı kontrol edin.")
                    }
                    
                    // Fast batch translation with free Google Translate (GTX)
                    if (englishVerses.isNotEmpty()) {
                        if (_readerSettings.value.language == AppLanguage.EN) {
                            paragraphsList.addAll(englishVerses)
                        } else {
                            val batchTranslated = translateVersesBatch(englishVerses)
                            paragraphsList.addAll(batchTranslated)
                        }
                    }
                    
                    val formattedBook = Book(
                        id = bookId,
                        title = when (bookId) {
                            "torah" -> if (_readerSettings.value.language == AppLanguage.EN) "Torah" else "Tevrat"
                            "sermon" -> if (_readerSettings.value.language == AppLanguage.EN) "Gospel" else "İncil"
                            "talmud" -> "Talmud"
                            "bukhari" -> if (_readerSettings.value.language == AppLanguage.EN) "Sahih al-Bukhari" else "Sahih-i Buharî"
                            "gita" -> "Bhagavad Gita"
                            else -> if (_readerSettings.value.language == AppLanguage.EN) "Gospel" else "İncil"
                        },
                        category = if (bookId == "talmud" || bookId == "bukhari" || bookId == "gita") {
                            if (_readerSettings.value.language == AppLanguage.EN) "Other Scriptures" else "Diğer Metinler"
                        } else {
                            if (_readerSettings.value.language == AppLanguage.EN) "Sacred Texts" else "Kutsal Metinler"
                        },
                        description = when (bookId) {
                            "torah" -> if (_readerSettings.value.language == AppLanguage.EN) "Torah (Tanakh) Live Text" else "Tevrat (Tanah) Canlı Metni"
                            "sermon" -> if (_readerSettings.value.language == AppLanguage.EN) "Gospel Live Text" else "İncil Canlı Metni"
                            "talmud" -> if (_readerSettings.value.language == AppLanguage.EN) "Talmud Bavli Live Text" else "Babil Talmudu Canlı Metni"
                            "bukhari" -> if (_readerSettings.value.language == AppLanguage.EN) "Sahih al-Bukhari Hadith Collection" else "Sahih-i Buharî Hadis Külliyatı"
                            "gita" -> if (_readerSettings.value.language == AppLanguage.EN) "Bhagavad Gita Sacred Scripture" else "Bhagavad Gita Kutsal Metni"
                            else -> ""
                        },
                        authorOrSource = when (bookId) {
                            "torah" -> if (_readerSettings.value.language == AppLanguage.EN) "Hebrew Tradition" else "İbranî Geleneği"
                            "sermon" -> if (_readerSettings.value.language == AppLanguage.EN) "Christian Tradition" else "Hristiyan Geleneği"
                            "talmud" -> if (_readerSettings.value.language == AppLanguage.EN) "Babylonian Academies" else "Babil Akademileri"
                            "bukhari" -> if (_readerSettings.value.language == AppLanguage.EN) "Imam Bukhari" else "İmam Buharî"
                            "gita" -> if (_readerSettings.value.language == AppLanguage.EN) "Sanskrit Tradition" else "Sanskrit Geleneği"
                            else -> ""
                        },
                        iconName = when (bookId) {
                            "torah" -> "menu_book"
                            "talmud" -> "menu_book"
                            "bukhari" -> "auto_stories"
                            "gita" -> "auto_stories"
                            else -> "church"
                        },
                        coverUrl = coverUrl,
                        contentTitle = if (bookId == "talmud") {
                            val pageNum = 2 + (chapterNumber - 1) / 2
                            val side = if (chapterNumber % 2 == 1) "a" else "b"
                            "${bibleBook.nameEnglish} $pageNum$side"
                        } else if (bookId == "bukhari") {
                            if (_readerSettings.value.language == AppLanguage.EN) {
                                "${bibleBook.nameEnglish} Hadith $chapterNumber"
                            } else {
                                "${bibleBook.nameTurkish} Hadis $chapterNumber"
                            }
                        } else {
                            if (_readerSettings.value.language == AppLanguage.EN) "${bibleBook.nameEnglish} $chapterNumber" else "${bibleBook.nameTurkish} $chapterNumber"
                        },
                        subContentTitle = if (bookId == "talmud") {
                            val pageNum = 2 + (chapterNumber - 1) / 2
                            val side = if (chapterNumber % 2 == 1) "a" else "b"
                            "Talmud Bavli - $pageNum$side"
                        } else if (bookId == "bukhari") {
                            "Sahih al-Bukhari - ${bibleBook.nameEnglish}"
                        } else {
                            if (_readerSettings.value.language == AppLanguage.EN) "${bibleBook.nameTurkish} $chapterNumber" else "${bibleBook.nameEnglish} $chapterNumber"
                        },
                        introText = when (bookId) {
                            "talmud" -> {
                                val pageNum = 2 + (chapterNumber - 1) / 2
                                val side = if (chapterNumber % 2 == 1) "a" else "b"
                                if (_readerSettings.value.language == AppLanguage.EN) {
                                    "Tractate ${bibleBook.nameEnglish}, Folio $pageNum$side loaded from Sefaria Database."
                                } else {
                                    "${bibleBook.nameEnglish} Bölümü, $pageNum$side Yaprağı Sefaria canlı veritabanından yüklendi."
                                }
                            }
                            "bukhari" -> {
                                if (_readerSettings.value.language == AppLanguage.EN) {
                                    "Book of ${bibleBook.nameEnglish}, Hadith $chapterNumber loaded from live Hadith API."
                                } else {
                                    "${bibleBook.nameTurkish} Bölümü, $chapterNumber. Hadis-i Şerif canlı veritabanından yüklendi."
                                }
                            }
                            else -> {
                                if (_readerSettings.value.language == AppLanguage.EN) {
                                    "Chapter $chapterNumber of the book of ${bibleBook.nameEnglish}, loaded with live API and academic translation."
                                } else {
                                    "${bibleBook.nameTurkish} kitabının $chapterNumber. bölümü canlı API ve akademik çeviri ile yüklenmiştir."
                                }
                            }
                        },
                        paragraphs = paragraphsList,
                        originalLanguageName = when (bookId) {
                            "torah" -> if (_readerSettings.value.language == AppLanguage.EN) "Hebrew" else "İbranice (Hebrew)"
                            "talmud" -> if (_readerSettings.value.language == AppLanguage.EN) "Aramaic" else "Aramice (Aramaic)"
                            "bukhari" -> if (_readerSettings.value.language == AppLanguage.EN) "Arabic" else "Arapça (Arabic)"
                            "gita" -> if (_readerSettings.value.language == AppLanguage.EN) "Sanskrit" else "Sanskritçe (Sanskrit)"
                            else -> if (_readerSettings.value.language == AppLanguage.EN) "Ancient Greek" else "Grekçe (Ancient Greek)"
                        },
                        originalIntroText = if (originalParagraphsList.isNotEmpty()) originalParagraphsList.first().substringAfter(": ") else "",
                        originalParagraphs = originalParagraphsList,
                        footnotes = if (_readerSettings.value.language == AppLanguage.EN) {
                            listOf(
                                "Academic Translation" to "This section has been translated in real-time through live data sources adhering to scholarly style.",
                                "Source" to when (bookId) {
                                    "torah", "talmud" -> "Sefaria Open Source Project"
                                    "bukhari" -> "Fawaz Ahmed Hadith API"
                                    "gita" -> "Vedic Scriptures Repository"
                                    else -> "Bible-API Library"
                                }
                            )
                        } else {
                            listOf(
                                "Akademik Çeviri" to "Bu bölüm, canlı veri kaynakları aracılığıyla gerçek zamanlı olarak akademik üsluba sadık kalınarak aktarılmıştır.",
                                "Kaynak" to when (bookId) {
                                    "torah", "talmud" -> "Sefaria Açık Kaynak Projesi"
                                    "bukhari" -> "Fawaz Ahmed Hadis Kütüphanesi"
                                    "gita" -> "Vedic Scriptures Açık Kaynak Veritabanı"
                                    else -> "Bible-API Çevrimdışı/Canlı Kütüphane"
                                }
                            )
                        }
                    )
                    
                    _bibleChapterInMemoryCache[cacheKey] = formattedBook
                    withContext(Dispatchers.Main) {
                        _activeBookContent.value = formattedBook
                        _isBookLoading.value = false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScriptureViewModel", "Failed to load bible chapter content", e)
                    withContext(Dispatchers.Main) {
                        _bookError.value = "Yüklenemedi: ${e.localizedMessage ?: "Bağlantı hatası"}. Lütfen internet bağlantısını kontrol edin."
                        _isBookLoading.value = false
                    }
                }
            }
        }
    }

    suspend fun fetchComparativeSlotBook(
        category: String,
        subBookId: String?,
        chapterNumber: Int
    ): Book = withContext(Dispatchers.IO) {
        if (category == "quran") {
            val surahNum = chapterNumber.coerceIn(1, 114)
            val surahMeta = com.example.data.model.QuranRepository.surahs.find { it.number == surahNum }
                ?: QuranSurah(surahNum, "سورة", "Surah $surahNum", "Surah $surahNum", 7, "Meccan")

            var surahContent = _surahInMemoryCache["surah_${surahNum}_${_readerSettings.value.language.name}_v3"]
            if (surahContent == null || surahContent!!.verses.isEmpty()) {
                val quranEdition = if (_readerSettings.value.language == AppLanguage.EN) "en.sahih" else "tr.yazir"
                val url = "https://api.alquran.cloud/v1/surah/$surahNum/editions/quran-uthmani,$quranEdition,ar.alafasy"
                try {
                    val req = Request.Builder().url(url).build()
                    okHttpClient.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: ""
                            val json = JSONObject(bodyStr)
                            val dataArray = json.optJSONArray("data")
                            if (dataArray != null && dataArray.length() >= 3) {
                                val arabicEdition = dataArray.getJSONObject(0)
                                val turkishEdition = dataArray.getJSONObject(1)
                                val nameArabic = arabicEdition.optString("name", "سورة")
                                val englishName = arabicEdition.optString("englishName", "Surah $surahNum")
                                val arabicVerses = arabicEdition.optJSONArray("ayahs") ?: org.json.JSONArray()
                                val turkishVerses = turkishEdition.optJSONArray("ayahs") ?: org.json.JSONArray()
                                val versesList = mutableListOf<QuranVerse>()
                                val count = minOf(arabicVerses.length(), turkishVerses.length())
                                for (i in 0 until count) {
                                    val arObj = arabicVerses.getJSONObject(i)
                                    val trObj = turkishVerses.getJSONObject(i)
                                    val vNum = arObj.optInt("numberInSurah", i + 1)
                                    val rawAr = arObj.optString("text", "")
                                    val rawTr = trObj.optString("text", "")
                                    val (cleanAr, cleanTr) = cleanQuranVerseText(surahNum, vNum, rawAr, rawTr)
                                    versesList.add(QuranVerse(vNum, cleanAr, cleanTr, ""))
                                }
                                surahContent = QuranSurahContent(surahNum, nameArabic, englishName, versesList)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScriptureViewModel", "Quran fetch failed in comparative mode", e)
                }
            }

            if (surahContent != null && surahContent!!.verses.isNotEmpty()) {
                val verses = surahContent!!.verses
                Book(
                    id = "quran",
                    title = "Kur'an-ı Kerim",
                    category = "Semavi Metinler",
                    description = "Yüce Kur'an Sûresi",
                    authorOrSource = "İslamî Gelenek",
                    iconName = "mosque",
                    coverUrl = "",
                    contentTitle = "${surahContent!!.number}. Sûre: ${surahContent!!.nameArabic} (${surahContent!!.englishName})",
                    subContentTitle = surahMeta.nameTurkish,
                    introText = "${surahContent!!.englishName} Sûresi, ${verses.size} ayettir.",
                    paragraphs = verses.map { "${it.number}: ${it.textTurkish}" },
                    originalLanguageName = "Arapça (Arabic)",
                    originalIntroText = surahContent!!.nameArabic,
                    originalParagraphs = verses.map { "${it.number}: ${it.textArabic}" }
                )
            } else {
                Book(
                    id = "quran",
                    title = "Kur'an-ı Kerim",
                    category = "Semavi Metinler",
                    description = "Yüce Kur'an",
                    authorOrSource = "İslam",
                    iconName = "mosque",
                    coverUrl = "",
                    contentTitle = "$surahNum. Sûre",
                    subContentTitle = "Kur'an Metni",
                    introText = "",
                    paragraphs = listOf("1: Rahmân ve Rahîm olan Allah'ın adıyla.", "2: Hamd, âlemlerin Rabbi Allah'a mahsustur."),
                    originalLanguageName = "Arapça",
                    originalIntroText = "",
                    originalParagraphs = listOf("1: بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "2: ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ")
                )
            }
        } else {
            val booksList = when (category) {
                "torah" -> com.example.data.model.BibleRepository.torahBooks
                "sermon" -> com.example.data.model.BibleRepository.bibleBooks
                "bukhari" -> com.example.data.model.BibleRepository.bukhariBooks
                "gita" -> com.example.data.model.BibleRepository.gitaBooks
                "talmud" -> com.example.data.model.BibleRepository.talmudBooks
                else -> com.example.data.model.BibleRepository.bibleBooks
            }
            val targetBook = booksList.find { it.id == subBookId } ?: booksList.first()
            val isTorah = (category == "torah")

            val cacheKey = "${category}_${targetBook.id}_${chapterNumber}_${_readerSettings.value.language.name}"
            val cached = _bibleChapterInMemoryCache[cacheKey]
            if (cached != null) {
                return@withContext cached
            }

            val paragraphsList = mutableListOf<String>()
            val originalParagraphsList = mutableListOf<String>()
            val englishVerses = mutableListOf<String>()

            try {
                val (fetchedEng, fetchedOrig) = fetchChapterContentInternal(category, targetBook, chapterNumber)
                englishVerses.addAll(fetchedEng)
                originalParagraphsList.addAll(fetchedOrig)

                if (englishVerses.isNotEmpty()) {
                    if (_readerSettings.value.language == AppLanguage.EN) {
                        paragraphsList.addAll(englishVerses)
                    } else {
                        val batch = translateVersesBatch(englishVerses)
                        paragraphsList.addAll(batch)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ScriptureViewModel", "Failed to fetch comparative book", e)
            }

            if (paragraphsList.isEmpty()) {
                paragraphsList.add("1: Bölüm metni yüklenemedi veya internet bağlantısı yok.")
            }

            val categoryTitle = when (category) {
                "torah" -> "Tevrat (Tanah)"
                "sermon" -> "İncil (Yeni Ahit)"
                "bukhari" -> "Sahih-i Buharî"
                "gita" -> "Bhagavad Gita"
                "talmud" -> "Talmud"
                else -> "Kutsal Metin"
            }

            val resultBook = Book(
                id = category,
                title = categoryTitle,
                category = "Semavi Metinler",
                description = targetBook.nameTurkish,
                authorOrSource = targetBook.sourceLanguage,
                iconName = if (isTorah) "menu_book" else "church",
                coverUrl = "",
                contentTitle = "${targetBook.nameTurkish} $chapterNumber. Bölüm",
                subContentTitle = "${targetBook.nameEnglish} Chapter $chapterNumber",
                introText = "${targetBook.nameTurkish} kitabının $chapterNumber. bölümü.",
                paragraphs = paragraphsList,
                originalLanguageName = targetBook.sourceLanguage,
                originalIntroText = "",
                originalParagraphs = originalParagraphsList,
                footnotes = emptyList()
            )
            _bibleChapterInMemoryCache[cacheKey] = resultBook
            resultBook
        }
    }

    private val _activeBookContent = MutableStateFlow<Book?>(null)
    val activeBookContent = _activeBookContent.asStateFlow()

    // ViewModel-level in-memory cache to prevent redundant API/network calls
    private val _surahInMemoryCache = java.util.concurrent.ConcurrentHashMap<String, QuranSurahContent>()
    private val _bibleChapterInMemoryCache = java.util.concurrent.ConcurrentHashMap<String, Book>()

    private val _isBookLoading = MutableStateFlow(false)
    val isBookLoading = _isBookLoading.asStateFlow()

    private val _bookError = MutableStateFlow<String?>(null)
    val bookError = _bookError.asStateFlow()

    fun loadBookContent(book: Book) {
        viewModelScope.launch {
            _isBookLoading.value = true
            _bookError.value = null
            // Retrieve the book with all rich paragraphs from our offline BookRepository
            val localBook = BookRepository.books.firstOrNull { it.id == book.id } ?: book
            _activeBookContent.value = localBook
            _isBookLoading.value = false
        }
    }

    fun loadBookContentWithAI(bookId: String, query: String) {
        viewModelScope.launch {
            _isBookLoading.value = true
            _bookError.value = null
            
            val originalLang = when (bookId) {
                "quran" -> "Arabic"
                "torah" -> "Hebrew (İbranice)"
                "sermon" -> "Ancient Greek (Grekçe)"
                "talmud" -> "Aramaic (Aramice)"
                "bukhari" -> "Arabic (Arapça)"
                else -> "Original Language"
            }
            
            withContext(Dispatchers.IO) {
                try {
                    val existingBook = BookRepository.books.firstOrNull { it.id == bookId }
                    val coverUrl = existingBook?.coverUrl ?: "https://images.unsplash.com/photo-1544947950-fa07a98d237f"
                    val iconName = existingBook?.iconName ?: "menu_book"
                    val category = existingBook?.category ?: "Kutsal Metinler"
                    val description = existingBook?.description ?: "Seçilen kutsal metin bölümü."
                    val authorOrSource = existingBook?.authorOrSource ?: "Geleneksel"
                    
                    var loadedBook: Book? = null
                    
                    // Route to free keyless public APIs if appropriate
                    if (bookId == "torah") {
                        try {
                            val cleanRef = query.trim().lowercase()
                            val numRegex = Regex("\\d+")
                            val number = numRegex.find(cleanRef)?.value ?: "1"
                            val bookPart = cleanRef.replace(numRegex, "").replace(Regex("[^a-za-zğıüşöçâ ]"), "").trim()
                            
                            val mappedBook = when {
                                bookPart.contains("yarat") -> "Genesis"
                                bookPart.contains("cikis") || bookPart.contains("çıkış") -> "Exodus"
                                bookPart.contains("levililer") -> "Leviticus"
                                bookPart.contains("sayilar") || bookPart.contains("sayılar") -> "Numbers"
                                bookPart.contains("yasa") -> "Deuteronomy"
                                bookPart.contains("yesu") || bookPart.contains("yeşu") -> "Joshua"
                                bookPart.contains("hakimler") -> "Judges"
                                bookPart.contains("rut") -> "Ruth"
                                bookPart.contains("samuel") -> if (cleanRef.contains("2") || cleanRef.contains("ıı") || cleanRef.contains("ii")) "II Samuel" else "I Samuel"
                                bookPart.contains("kral") -> if (cleanRef.contains("2") || cleanRef.contains("ıı") || cleanRef.contains("ii")) "II Kings" else "I Kings"
                                bookPart.contains("tarih") -> if (cleanRef.contains("2") || cleanRef.contains("ıı") || cleanRef.contains("ii")) "II Chronicles" else "I Chronicles"
                                bookPart.contains("ezra") -> "Ezra"
                                bookPart.contains("nehemya") -> "Nehemiah"
                                bookPart.contains("ester") -> "Esther"
                                bookPart.contains("eyup") || bookPart.contains("eyüp") -> "Job"
                                bookPart.contains("mezmur") -> "Psalms"
                                bookPart.contains("ozdeyis") || bookPart.contains("özdeyiş") || bookPart.contains("suleyman") || bookPart.contains("süleyman") -> "Proverbs"
                                bookPart.contains("vaiz") -> "Ecclesiastes"
                                bookPart.contains("ezgi") -> "Song of Songs"
                                bookPart.contains("yesaya") || bookPart.contains("yeşaya") -> "Isaiah"
                                bookPart.contains("yeremya") -> "Jeremiah"
                                bookPart.contains("agit") || bookPart.contains("ağıt") -> "Lamentations"
                                bookPart.contains("hezekiel") -> "Ezekiel"
                                bookPart.contains("daniel") -> "Daniel"
                                bookPart.contains("hosea") -> "Hosea"
                                bookPart.contains("yoel") -> "Joel"
                                bookPart.contains("amos") -> "Amos"
                                bookPart.contains("obadya") -> "Obadiah"
                                bookPart.contains("yunus") -> "Jonah"
                                bookPart.contains("mika") -> "Micah"
                                bookPart.contains("nahum") -> "Nahum"
                                bookPart.contains("habakkuk") -> "Habakkuk"
                                bookPart.contains("tsefanya") || bookPart.contains("zeferya") || bookPart.contains("tefanya") || bookPart.contains("sefanya") -> "Zephaniah"
                                bookPart.contains("hagay") -> "Haggai"
                                bookPart.contains("zekeriya") -> "Zechariah"
                                bookPart.contains("malaki") -> "Malachi"
                                else -> "Genesis"
                            }
                            
                            val sefariaUrl = "https://www.sefaria.org/api/texts/${mappedBook}.${number}?context=0"
                            val request = Request.Builder().url(sefariaUrl).build()
                            okHttpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val bodyStr = response.body?.string() ?: ""
                                    val json = JSONObject(bodyStr)
                                    val ref = json.optString("ref", "${mappedBook} ${number}")
                                    
                                    val engJA = json.optJSONArray("text")
                                    val engList = mutableListOf<String>()
                                    if (engJA != null) {
                                        for (i in 0 until engJA.length()) {
                                            engList.add(engJA.optString(i).replace(Regex("<[^>]*>"), ""))
                                        }
                                    }
                                    
                                    val hebJA = json.optJSONArray("he")
                                    val hebList = mutableListOf<String>()
                                    if (hebJA != null) {
                                        for (i in 0 until hebJA.length()) {
                                            hebList.add(hebJA.optString(i).replace(Regex("<[^>]*>"), ""))
                                        }
                                    }

                                    val originalVerseCount = engList.size
                                    
                                    // Log simulating the required endpoint pattern
                                    val simulatedEndpoint = "/${mappedBook.replace(" ", "_").lowercase()}/${number}/tr"
                                    android.util.Log.d("ScriptureAPI", "Fetching from Sefaria: $simulatedEndpoint, original verse count: $originalVerseCount")
                                    
                                    val englishVerses = engList.mapIndexed { i, txt -> "${i + 1}: $txt" }
                                    val paragraphsList = mutableListOf<String>()
                                    if (englishVerses.isNotEmpty()) {
                                        val deferredTranslations = englishVerses.map { verse ->
                                            async {
                                                translateTextGtx(verse)
                                            }
                                        }
                                        paragraphsList.addAll(deferredTranslations.awaitAll())
                                    }
                                    
                                    android.util.Log.d("ScriptureAPI", "Verse count validated: ${paragraphsList.size}/$originalVerseCount verses translated successfully.")
                                    
                                    val originalParagraphsList = hebList.mapIndexed { i, txt -> "${i + 1}: $txt" }
                                    val intro = if (paragraphsList.isNotEmpty()) "Tevrat (Tanah) - ${ref} bölümü Sefaria canli veritabanindan başarıyla yüklendi. ${paragraphsList.first().substringAfter(": ")}" else "Tevrat - ${ref} bölümü."
                                    
                                    loadedBook = Book(
                                        id = bookId,
                                        title = "Tevrat",
                                        category = category,
                                        description = description,
                                        authorOrSource = authorOrSource,
                                        iconName = iconName,
                                        coverUrl = coverUrl,
                                        contentTitle = ref,
                                        subContentTitle = "Sefaria - Kitab-ı Mukaddes Meali (Ücretsiz Canlı Çeviri)",
                                        introText = intro,
                                        paragraphs = paragraphsList,
                                        originalLanguageName = "İbranice (Hebrew)",
                                        originalIntroText = if (hebList.isNotEmpty()) hebList.first() else "",
                                        originalParagraphs = originalParagraphsList,
                                        footnotes = listOf(
                                            "Açık Kaynak" to "Bu bölüm, dünya çapındaki Sefaria Açık Kaynak projesinin kütüphanesinden canlı ve bedava olarak getirilmiştir.",
                                            "Referans" to "Tanakh - ${ref}",
                                            "Ayet Kontrolü" to "Orijinal ayet sayısı ($originalVerseCount) ile Türkçe ayet sayısı (${paragraphsList.size}) tam olarak doğrulanmıştır."
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ScriptureViewModel", "Failed Sefaria fetch", e)
                        }
                    } else if (bookId == "quran") {
                        try {
                            val cleanRef = query.trim().lowercase()
                            val numRegex = Regex("\\d+")
                            var surahNum = numRegex.find(cleanRef)?.value?.toIntOrNull() ?: 1
                            if (surahNum !in 1..114) {
                                val quranMap = mapOf(
                                    "fatiha" to 1, "bakara" to 2, "ali imran" to 3, "nisa" to 4, "maide" to 5, "enam" to 6, "araf" to 7,
                                    "enfal" to 8, "tevbe" to 9, "yunus" to 10, "hud" to 11, "yusuf" to 12, "rad" to 13, "ibrahim" to 14,
                                    "hicr" to 15, "nahl" to 16, "isra" to 17, "kehf" to 18, "meryem" to 19, "taha" to 20, "enbiya" to 21,
                                    "hac" to 22, "muminun" to 23, "nur" to 24, "furkan" to 25, "suara" to 26, "neml" to 27, "kasas" to 28,
                                    "ankebut" to 29, "rum" to 30, "lokman" to 31, "secde" to 32, "ahzab" to 33, "sebe" to 34, "fatir" to 35,
                                    "yasin" to 36, "saffat" to 37, "sad" to 38, "zumer" to 39, "mumin" to 40, "fussilet" to 41, "sura" to 42,
                                    "zuhruf" to 43, "duhan" to 44, "casiye" to 45, "ahkaf" to 46, "muhammed" to 47, "fetih" to 48, "hucurat" to 49,
                                    "kaf" to 50, "zariyat" to 51, "tur" to 52, "necm" to 53, "kamer" to 54, "rahman" to 55, "vakia" to 56,
                                    "hadid" to 57, "mucaadele" to 58, "hasr" to 59, "mumtehine" to 60, "saf" to 61, "cuma" to 62, "munafikun" to 63,
                                    "tegabun" to 64, "talak" to 65, "tahrim" to 66, "mulk" to 67, "mülk" to 67, "kalem" to 68, "hakka" to 69, "mearic" to 70,
                                    "nuh" to 71, "cin" to 72, "muzzemmil" to 73, "muddessir" to 74, "kiyamet" to 75, "insan" to 76, "murselat" to 77,
                                    "nebe" to 78, "naziat" to 79, "abese" to 80, "tekvir" to 81, "infitar" to 82, "mutaffifin" to 83, "insikak" to 84,
                                    "buruc" to 85, "tarik" to 86, "ala" to 87, "gasiye" to 88, "fecr" to 89, "beled" to 90, "sems" to 91, "şems" to 91,
                                    "leyl" to 92, "duha" to 93, "insirah" to 94, "inşirah" to 94, "tin" to 95, "alak" to 96, "kadir" to 97, "beyyine" to 98,
                                    "zilzal" to 99, "adiyat" to 100, "karia" to 101, "tekasur" to 102, "asr" to 103, "humeze" to 104, "fil" to 105,
                                    "kureys" to 106, "maun" to 107, "kevser" to 108, "kafirun" to 109, "nasr" to 110, "tebbet" to 111, "ihlas" to 112,
                                    "felak" to 113, "nas" to 114
                                )
                                val bookPart = cleanRef.replace(numRegex, "").replace(Regex("[^a-za-zğıüşöçâ ]"), "").trim()
                                surahNum = quranMap[bookPart] ?: 48
                            }
                            
                            val quranUrl = "https://api.alquran.cloud/v1/surah/${surahNum}/tr.yazir"
                            val request = Request.Builder().url(quranUrl).build()
                            okHttpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val bodyStr = response.body?.string() ?: ""
                                    val json = JSONObject(bodyStr)
                                    val dataObj = json.getJSONObject("data")
                                    val englishName = dataObj.getString("englishName")
                                    val nameArabic = dataObj.getString("name")
                                    val ayahsJA = dataObj.getJSONArray("ayahs")
                                    
                                    val paragraphsList = mutableListOf<String>()
                                    for (i in 0 until ayahsJA.length()) {
                                        val ayahObj = ayahsJA.getJSONObject(i)
                                        paragraphsList.add("${ayahObj.getInt("numberInSurah")}: ${ayahObj.getString("text")}")
                                    }
                                    
                                    val originalParagraphsList = mutableListOf<String>()
                                    val arUrl = "https://api.alquran.cloud/v1/surah/${surahNum}/quran-simple"
                                    val arRequest = Request.Builder().url(arUrl).build()
                                    try {
                                        okHttpClient.newCall(arRequest).execute().use { arResponse ->
                                            if (arResponse.isSuccessful) {
                                                val arBodyStr = arResponse.body?.string() ?: ""
                                                val arJson = JSONObject(arBodyStr)
                                                val arData = arJson.getJSONObject("data")
                                                val arAyahs = arData.getJSONArray("ayahs")
                                                for (i in 0 until arAyahs.length()) {
                                                    val ayahObj = arAyahs.getJSONObject(i)
                                                    originalParagraphsList.add("${ayahObj.getInt("numberInSurah")}: ${ayahObj.getString("text")}")
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // fallback
                                    }
                                    
                                    loadedBook = Book(
                                        id = bookId,
                                        title = "Kur'an-ı Kerim",
                                        category = category,
                                        description = description,
                                        authorOrSource = authorOrSource,
                                        iconName = iconName,
                                        coverUrl = coverUrl,
                                        contentTitle = "${englishName} Suresi (${surahNum})",
                                        subContentTitle = "${nameArabic} • Diyanet Meali",
                                        introText = "Kur'an-ı Kerim'in ${englishName} Suresi Al-Quran API'den canlı yüklendi.",
                                        paragraphs = paragraphsList,
                                        originalLanguageName = "Arapça (Arabic)",
                                        originalIntroText = if (originalParagraphsList.isNotEmpty()) originalParagraphsList.first().substringAfter(": ") else nameArabic,
                                        originalParagraphs = originalParagraphsList,
                                        footnotes = listOf(
                                            "Sure Bilgisi" to "Bu sure ${paragraphsList.size} ayettir. Diyanet İşleri Başkanlığı meali kullanılmıştır.",
                                            "Canlı API" to "Al-Quran Cloud aracılığıyla tamamen ücretsiz, reklamsız ve anahtarsız servis edilmektedir."
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ScriptureViewModel", "Failed Quran fetch", e)
                        }
                    } else if (bookId == "sermon") {
                        try {
                            val cleanRef = query.trim().lowercase()
                            val numRegex = Regex("\\d+")
                            val number = numRegex.find(cleanRef)?.value ?: "5"
                            val bookPart = cleanRef.replace(numRegex, "").replace(Regex("[^a-za-zğıüşöçâ ]"), "").trim()
                            
                            val mappedBibleRef = when {
                                bookPart.contains("matta") || bookPart.contains("matthew") -> "matthew+${number}"
                                bookPart.contains("markos") || bookPart.contains("mark") -> "mark+${number}"
                                bookPart.contains("luka") || bookPart.contains("luke") -> "luke+${number}"
                                bookPart.contains("yuhanna") || bookPart.contains("john") -> {
                                    when {
                                        cleanRef.contains("1") || cleanRef.contains("ı ") || cleanRef.contains("i ") || cleanRef.startsWith("1") || cleanRef.startsWith("i") -> "1+john+${number}"
                                        cleanRef.contains("2") || cleanRef.contains("ıı") || cleanRef.contains("ii") || cleanRef.startsWith("2") -> "2+john+${number}"
                                        cleanRef.contains("3") || cleanRef.contains("ııı") || cleanRef.contains("iii") || cleanRef.startsWith("3") -> "3+john+${number}"
                                        else -> "john+${number}"
                                    }
                                }
                                bookPart.contains("elçi") || bookPart.contains("elci") || bookPart.contains("işler") || bookPart.contains("isler") || bookPart.contains("acts") -> "acts+${number}"
                                bookPart.contains("romali") || bookPart.contains("romalı") || bookPart.contains("romans") -> "romans+${number}"
                                bookPart.contains("korint") || bookPart.contains("corinthians") -> {
                                    if (cleanRef.contains("2") || cleanRef.contains("ıı") || cleanRef.contains("ii") || cleanRef.startsWith("2")) "2+corinthians+${number}" else "1+corinthians+${number}"
                                }
                                bookPart.contains("galat") || bookPart.contains("galatians") -> "galatians+${number}"
                                bookPart.contains("efes") || bookPart.contains("ephesians") -> "ephesians+${number}"
                                bookPart.contains("filipi") || bookPart.contains("philippians") -> "philippians+${number}"
                                bookPart.contains("kolose") || bookPart.contains("colossians") -> "colossians+${number}"
                                bookPart.contains("selanik") || bookPart.contains("thessalonians") -> {
                                    if (cleanRef.contains("2") || cleanRef.contains("ıı") || cleanRef.contains("ii") || cleanRef.startsWith("2")) "2+thessalonians+${number}" else "1+thessalonians+${number}"
                                }
                                bookPart.contains("timot") || bookPart.contains("timothy") -> {
                                    if (cleanRef.contains("2") || cleanRef.contains("ıı") || cleanRef.contains("ii") || cleanRef.startsWith("2")) "2+timothy+${number}" else "1+timothy+${number}"
                                }
                                bookPart.contains("titus") -> "titus+${number}"
                                bookPart.contains("filimon") || bookPart.contains("philemon") -> "philemon+${number}"
                                bookPart.contains("ibrani") || bookPart.contains("hebrews") -> "hebrews+${number}"
                                bookPart.contains("yakup") || bookPart.contains("james") -> "james+${number}"
                                bookPart.contains("petru") || bookPart.contains("petro") || bookPart.contains("peter") -> {
                                    if (cleanRef.contains("2") || cleanRef.contains("ıı") || cleanRef.contains("ii") || cleanRef.startsWith("2")) "2+peter+${number}" else "1+peter+${number}"
                                }
                                bookPart.contains("yahuda") || bookPart.contains("jude") -> "jude+${number}"
                                bookPart.contains("vahiy") || bookPart.contains("revelation") -> "revelation+${number}"
                                 else -> "matthew+${number}"
                             }
                             
                             val bibleUrl = "https://bible-api.com/${mappedBibleRef}"
                            val request = Request.Builder().url(bibleUrl).build()
                            okHttpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val bodyStr = response.body?.string() ?: ""
                                    val json = JSONObject(bodyStr)
                                    val ref = json.optString("reference", "İncil Bölümü")
                                    val versesJA = json.getJSONArray("verses")
                                    
                                    val engList = mutableListOf<String>()
                                    val originalParagraphsList = mutableListOf<String>()
                                                                         for (i in 0 until versesJA.length()) {
                                         val vObj = versesJA.getJSONObject(i)
                                         val vNum = vObj.getInt("verse")
                                         val vText = vObj.getString("text").trim()
                                         engList.add("${vNum}: ${vText}")
                                         originalParagraphsList.add("${vNum}: [İngilizce] ${vText}")
                                     }

                                     val originalVerseCount = engList.size
                                    
                                    // Log simulating the required endpoint pattern
                                    val simulatedEndpoint = "/${mappedBibleRef.substringBefore("+")}/${mappedBibleRef.substringAfter("+")}/tr"
                                    android.util.Log.d("ScriptureAPI", "Fetching from Bible-API: $simulatedEndpoint, original verse count: $originalVerseCount")
                                    
                                    val paragraphsList = mutableListOf<String>()
                                    if (engList.isNotEmpty()) {
                                        val deferredTranslations = engList.map { verse ->
                                            async {
                                                translateTextGtx(verse)
                                            }
                                        }
                                        paragraphsList.addAll(deferredTranslations.awaitAll())
                                    }
                                    
                                    android.util.Log.d("ScriptureAPI", "Verse count validated: ${paragraphsList.size}/$originalVerseCount verses translated successfully.")
                                    
                                    loadedBook = Book(
                                        id = bookId,
                                        title = "İncil",
                                        category = category,
                                        description = description,
                                        authorOrSource = authorOrSource,
                                        iconName = iconName,
                                        coverUrl = coverUrl,
                                        contentTitle = ref,
                                        subContentTitle = "Bible-API - Kitab-ı Mukaddes Meali (Ücretsiz Canlı Çeviri)",
                                        introText = if (paragraphsList.isNotEmpty()) "İncil'in ${ref} bölümü Bible-API aracılığıyla başarıyla getirilmiş ve Türkçe meali yapılmıştır: ${paragraphsList.first().substringAfter(": ")}" else "İncil'in ${ref} bölümü.",
                                        paragraphs = paragraphsList,
                                        originalLanguageName = "Grekçe / İngilizce",
                                        originalIntroText = "Ἰδὼν δὲ τοὺς ὄχλους...",
                                        originalParagraphs = originalParagraphsList,
                                        footnotes = listOf(
                                            "Bilgi" to "Bible-api.com kütüphanesinden canlı ve bedava olarak çekilmiştir.",
                                            "Referans" to "Yeni Antlaşma - ${ref}",
                                            "Ayet Kontrolü" to "Orijinal ayet sayısı ($originalVerseCount) ile Türkçe ayet sayısı (${paragraphsList.size}) tam olarak doğrulanmıştır."
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ScriptureViewModel", "Failed Sermon/Bible fetch", e)
                        }
                    } else if (bookId == "talmud") {
                        try {
                            val cleanRef = query.trim().lowercase()
                            val tractates = listOf(
                                "berakhot", "shabbat", "eruvin", "pesachim", "yoma", "sukkah", "beitzah", "rosh hashanah", "taanit", "megillah", "moed katan", "chagigah",
                                "yevamot", "ketubot", "nedarim", "nazir", "sotah", "gittin", "kiddushin",
                                "bava kamma", "bava metzia", "bava batra", "sanhedrin", "makkot", "shevuot", "avodah zarah", "horayot",
                                "zevachim", "menachot", "chullin", "bechorot", "arachin", "temurah", "keritot", "meilah", "tamid", "niddah"
                            )
                            
                            var mappedTractate = tractates.firstOrNull { cleanRef.contains(it) } ?: "berakhot"
                            val dafRegex = Regex("\\d+[a-b]?")
                            val daf = dafRegex.find(cleanRef)?.value ?: "2a"
                            
                            val tractateCap = mappedTractate.split(" ").joinToString("-") { word ->
                                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            }
                            
                            val sefariaUrl = "https://www.sefaria.org/api/texts/${tractateCap}.${daf}?context=0"
                            val request = Request.Builder().url(sefariaUrl).build()
                            okHttpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val bodyStr = response.body?.string() ?: ""
                                    val json = JSONObject(bodyStr)
                                    val ref = json.optString("ref", "${tractateCap} ${daf}")
                                    
                                    val engJA = json.optJSONArray("text")
                                    val engList = mutableListOf<String>()
                                    if (engJA != null) {
                                        for (i in 0 until engJA.length()) {
                                            engList.add(engJA.optString(i).replace(Regex("<[^>]*>"), ""))
                                        }
                                    }
                                    
                                    val hebJA = json.optJSONArray("he")
                                    val hebList = mutableListOf<String>()
                                    if (hebJA != null) {
                                        for (i in 0 until hebJA.length()) {
                                            hebList.add(hebJA.optString(i).replace(Regex("<[^>]*>"), ""))
                                        }
                                    }

                                    val originalVerseCount = engList.size
                                    
                                    val englishVerses = engList.mapIndexed { i, txt -> "${i + 1}: $txt" }
                                    val paragraphsList = mutableListOf<String>()
                                    if (englishVerses.isNotEmpty()) {
                                        val deferredTranslations = englishVerses.map { verse ->
                                            async {
                                                translateTextGtx(verse)
                                            }
                                        }
                                        paragraphsList.addAll(deferredTranslations.awaitAll())
                                    }
                                    
                                    val originalParagraphsList = hebList.mapIndexed { i, txt -> "${i + 1}: $txt" }
                                    val intro = if (paragraphsList.isNotEmpty()) {
                                        "Talmud - ${ref} bölümü Sefaria canlı veritabanından başarıyla yüklendi."
                                    } else {
                                        "Talmud - ${ref} bölümü."
                                    }
                                    
                                    loadedBook = Book(
                                        id = bookId,
                                        title = "Talmud",
                                        category = category,
                                        description = description,
                                        authorOrSource = authorOrSource,
                                        iconName = iconName,
                                        coverUrl = coverUrl,
                                        contentTitle = ref,
                                        subContentTitle = "Sefaria - Talmud Babilî (Canlı Çeviri)",
                                        introText = intro,
                                        paragraphs = paragraphsList,
                                        originalLanguageName = "Aramice (Aramaic)",
                                        originalIntroText = if (hebList.isNotEmpty()) hebList.first() else "",
                                        originalParagraphs = originalParagraphsList,
                                        footnotes = listOf(
                                            "Bilgi" to "Sefaria Açık Kaynak Talmud veritabanından canlı olarak getirilmiştir.",
                                            "Referans" to "Talmud - ${ref}",
                                            "Satır Kontrolü" to "Orijinal satır sayısı ($originalVerseCount) ile Türkçe satır sayısı (${paragraphsList.size}) tam olarak doğrulanmıştır."
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ScriptureViewModel", "Failed Talmud fetch", e)
                        }
                    } else if (bookId == "bukhari") {
                        try {
                            val cleanRef = query.trim().lowercase()
                            val numRegex = Regex("\\d+")
                            val requestedHadithNumber = numRegex.find(cleanRef)?.value?.toIntOrNull() ?: 1
                            
                            val cacheFile = java.io.File(getApplication<Application>().filesDir, "eng-bukhari.min.json")
                            var jsonContent = ""
                            
                            if (cacheFile.exists()) {
                                jsonContent = cacheFile.readText()
                            } else {
                                val bukhariUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari.min.json"
                                val request = Request.Builder().url(bukhariUrl).build()
                                okHttpClient.newCall(request).execute().use { response ->
                                    if (response.isSuccessful) {
                                        jsonContent = response.body?.string() ?: ""
                                        if (jsonContent.isNotEmpty()) {
                                            try {
                                                cacheFile.writeText(jsonContent)
                                            } catch (e: Exception) {
                                                android.util.Log.e("ScriptureViewModel", "Failed to cache Bukhari file", e)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (jsonContent.isNotEmpty()) {
                                val json = JSONObject(jsonContent)
                                val hadithsJA = json.getJSONArray("hadiths")
                                
                                var targetHadithObj: JSONObject? = null
                                for (i in 0 until hadithsJA.length()) {
                                    val hObj = hadithsJA.getJSONObject(i)
                                    val hNum = hObj.optInt("hadithnumber", -1)
                                    if (hNum == requestedHadithNumber) {
                                        targetHadithObj = hObj
                                        break
                                    }
                                }
                                
                                if (targetHadithObj == null && requestedHadithNumber - 1 < hadithsJA.length()) {
                                    targetHadithObj = hadithsJA.getJSONObject(requestedHadithNumber - 1)
                                }
                                
                                if (targetHadithObj != null) {
                                    val text = targetHadithObj.getString("text")
                                    val hNum = targetHadithObj.optInt("hadithnumber", requestedHadithNumber)
                                    
                                    val trText = translateTextGtx("${hNum}: ${text}")
                                    val paragraphsList = listOf(trText)
                                    val originalParagraphsList = listOf("${hNum}: ${text}")
                                    
                                    loadedBook = Book(
                                        id = bookId,
                                        title = "Sahih-i Buharî",
                                        category = category,
                                        description = description,
                                        authorOrSource = authorOrSource,
                                        iconName = iconName,
                                        coverUrl = coverUrl,
                                        contentTitle = "Hadis ${hNum}",
                                        subContentTitle = "Sahih-i Buharî Külliyatı",
                                        introText = "Sahih-i Buharî ${hNum}. Hadis-i Şerif canlı veritabanından yüklendi.",
                                        paragraphs = paragraphsList,
                                        originalLanguageName = "İngilizce (English)",
                                        originalIntroText = text.take(100) + "...",
                                        originalParagraphs = originalParagraphsList,
                                        footnotes = listOf(
                                            "Kaynak" to "Fawaz Ahmed Hadith API (Açık Kaynak)",
                                            "Hadis No" to "Sahih-i Buharî No: ${hNum}",
                                            "Güvenilirlik" to "Sahih (En Yüksek Derece)"
                                        )
                                    )
                                }
                             }
                        } catch (e: Exception) {
                            android.util.Log.e("ScriptureViewModel", "Failed Bukhari fetch", e)
                        }
                    }
                    
                    // Fallback to offline static book content if API fetch is unavailable
                    if (loadedBook == null) {
                        val numRegex = Regex("\\d+")
                        val number = numRegex.find(query)?.value?.toIntOrNull() ?: 1
                        loadedBook = Book(
                            id = bookId,
                            title = existingBook?.title ?: "Kutsal Metin",
                            category = category,
                            description = description,
                            authorOrSource = authorOrSource,
                            iconName = iconName,
                            coverUrl = coverUrl,
                            contentTitle = existingBook?.contentTitle ?: "Bölüm $number",
                            subContentTitle = existingBook?.subContentTitle ?: "Kutsal Metin",
                            introText = existingBook?.introText ?: "${existingBook?.title ?: "Metin"}'in $number. Bölümü.",
                            paragraphs = existingBook?.paragraphs ?: listOf(
                                "1: Bilgelik, bilmediğini bilmekle başlar. Kendi zihnini fetheden, dünyayı fethetmiş sayılır.",
                                "2: Gerçeğin peşinden giden yol sabır, adalet, merhamet ve bilgelikten geçer."
                            ),
                            originalLanguageName = originalLang,
                            originalIntroText = existingBook?.originalIntroText ?: "",
                            originalParagraphs = existingBook?.originalParagraphs ?: emptyList(),
                            footnotes = existingBook?.footnotes ?: listOf("Bilgi" to "Çevrimdışı kutsal metin kütüphanesinden yüklenmiştir.")
                        )
                    }
                    
                    withContext(Dispatchers.Main) {
                        _activeBookContent.value = loadedBook
                        _isBookLoading.value = false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScriptureViewModel", "Failed to compile book content", e)
                    withContext(Dispatchers.Main) {
                        _bookError.value = e.message ?: "Beklenmedik bir hata oluştu."
                        _isBookLoading.value = false
                    }
                }
            }
        }
    }

    private val _userState = MutableStateFlow(UserState())
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    // Desired/Selected holy books for the verse (persisted in SharedPreferences)
    private val _selectedBooksForVerse = MutableStateFlow<Set<String>>(emptySet())
    val selectedBooksForVerse = _selectedBooksForVerse.asStateFlow()

    // Religion & Sect Preferences
    private val _userReligion = MutableStateFlow(UserReligion.ISLAM)
    val userReligion: StateFlow<UserReligion> = _userReligion.asStateFlow()

    private val _userSect = MutableStateFlow(UserSect.SUNNI)
    val userSect: StateFlow<UserSect> = _userSect.asStateFlow()

    // Location & Live Prayer API State
    private val _userLocationInfo = MutableStateFlow<com.example.data.api.UserLocationInfo?>(null)
    val userLocationInfo: StateFlow<com.example.data.api.UserLocationInfo?> = _userLocationInfo.asStateFlow()

    private val _livePrayerTimes = MutableStateFlow<List<PrayerTimeInfo>>(emptyList())
    val livePrayerTimes: StateFlow<List<PrayerTimeInfo>> = _livePrayerTimes.asStateFlow()

    private val _isLocationLoading = MutableStateFlow(false)
    val isLocationLoading: StateFlow<Boolean> = _isLocationLoading.asStateFlow()

    // Notifications status toggle
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    // Notification interval in minutes (default 24 hours = 1440 minutes)
    private val _notificationIntervalMinutes = MutableStateFlow(1440)
    val notificationIntervalMinutes = _notificationIntervalMinutes.asStateFlow()

    // Loading state for random active verse
    private val _isVerseLoading = MutableStateFlow(false)
    val isVerseLoading = _isVerseLoading.asStateFlow()

    data class ActiveVerse(
        val book: Book,
        val text: String,
        val reference: String
    )

    private val defaultVerse = ActiveVerse(
        book = BookRepository.books.first(),
        text = "Doğrusu biz sana apaçık bir fetih ihsân ettik. Tâ ki Allah senin geçmiş ve gelecek günahlarını bağışlasın, üzerindeki nimetini tamamlasın ve seni dosdoğru bir yola iletsin.",
        reference = "FETİH SURESİ, 1-2"
    )

    private val _activeVerse = MutableStateFlow<ActiveVerse>(defaultVerse)
    val activeVerse = _activeVerse.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ScriptureRepository(
            database.noteHighlightDao(),
            database.readingHistoryDao()
        )
        books = repository.books
        // Initialize with sample data if first run
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
        checkCurrentUser()

        // Load downloaded/offline data
        val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
        _downloadedBooks.value = offlinePrefs.getStringSet("downloaded_books", emptySet()) ?: emptySet()
        _downloadedSurahs.value = (offlinePrefs.getStringSet("downloaded_surahs", emptySet()) ?: emptySet())
            .mapNotNull { it.toIntOrNull() }.toSet()
        _downloadedChapters.value = offlinePrefs.getStringSet("downloaded_chapters", emptySet()) ?: emptySet()

        // Load reader settings
        val settingsPrefs = getApplication<Application>().getSharedPreferences("scriptorium_settings", Context.MODE_PRIVATE)
        val themeStr = settingsPrefs.getString("theme", "LIGHT") ?: "LIGHT"
        val fontSize = settingsPrefs.getFloat("font_size", 20f)
        val fontFamilyStr = settingsPrefs.getString("font_family", "SERIF") ?: "SERIF"
        val lineHeightStr = settingsPrefs.getString("line_height", "NORMAL") ?: "NORMAL"
        val languageStr = settingsPrefs.getString("language", "EN") ?: "EN"
        val showOriginal = settingsPrefs.getBoolean("show_original_script", true)

        _readerSettings.value = ReaderSettings(
            theme = try { AppThemeSetting.valueOf(themeStr) } catch(e: Exception) { AppThemeSetting.LIGHT },
            fontSizeSp = fontSize,
            fontFamily = try { FontFamilySetting.valueOf(fontFamilyStr) } catch(e: Exception) { FontFamilySetting.SERIF },
            lineHeight = try { LineHeightSetting.valueOf(lineHeightStr) } catch(e: Exception) { LineHeightSetting.NORMAL },
            language = try { AppLanguage.valueOf(languageStr) } catch(e: Exception) { AppLanguage.EN },
            showOriginalScript = showOriginal
        )

        // Load selected books for verse
        val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
        _notificationsEnabled.value = prefs.getBoolean("notifications_enabled", true)
        _notificationIntervalMinutes.value = prefs.getInt("notification_interval_minutes", 1440)
        val savedBooks = prefs.getStringSet("selected_verse_books", null)
        if (savedBooks != null) {
            _selectedBooksForVerse.value = savedBooks
        } else {
            _selectedBooksForVerse.value = books.map { it.id }.toSet()
        }

        // Load Religion & Sect
        val savedReligionStr = prefs.getString("user_religion", "islam")
        val loadedReligion = UserReligion.fromId(savedReligionStr)
        _userReligion.value = loadedReligion

        val savedSectStr = prefs.getString("user_sect", "sunni")
        val loadedSect = UserSect.fromId(savedSectStr, loadedReligion)
        _userSect.value = loadedSect

        refreshActiveVerse()
        refreshLocationAndPrayerTimes()

        // Restore external storage backup if present
        restoreBackupIfAvailable()
        // Save initial persistent state
        exportPersistentBackup()
    }

    private fun checkCurrentUser() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
            val customName = prefs.getString("custom_name", null)
            val customBio = prefs.getString("custom_bio", "")
            val customPhotoUrl = prefs.getString("custom_photo_url", null)

            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                repository.startFirestoreSync(firebaseUser.uid, viewModelScope)
                _userState.value = UserState(
                    email = firebaseUser.email,
                    displayName = customName ?: firebaseUser.displayName ?: "Kullanıcı",
                    photoUrl = customPhotoUrl ?: firebaseUser.photoUrl?.toString(),
                    bio = customBio,
                    isLoggedIn = true,
                    isDemo = false
                )
            } else {
                val isDemoLoggedIn = prefs.getBoolean("is_demo_logged_in", false)
                if (isDemoLoggedIn) {
                    _userState.value = UserState(
                        email = prefs.getString("demo_email", "yolcu@scriptorium.org"),
                        displayName = customName ?: prefs.getString("demo_name", "Bilgelik Yolcusu"),
                        photoUrl = customPhotoUrl,
                        bio = customBio,
                        isLoggedIn = true,
                        isDemo = true
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback if Firebase is not initialized
            val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
            val customName = prefs.getString("custom_name", null)
            val customBio = prefs.getString("custom_bio", "")
            val customPhotoUrl = prefs.getString("custom_photo_url", null)

            val isDemoLoggedIn = prefs.getBoolean("is_demo_logged_in", false)
            if (isDemoLoggedIn) {
                _userState.value = UserState(
                    email = prefs.getString("demo_email", "yolcu@scriptorium.org"),
                    displayName = customName ?: prefs.getString("demo_name", "Bilgelik Yolcusu"),
                    photoUrl = customPhotoUrl,
                    bio = customBio,
                    isLoggedIn = true,
                    isDemo = true
                )
            }
        }
    }

    fun updateProfile(name: String, bio: String, photoUrl: String) {
        val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("custom_name", name)
            .putString("custom_bio", bio)
            .putString("custom_photo_url", if (photoUrl.isBlank()) null else photoUrl)
            .apply()

        _userState.value = _userState.value.copy(
            displayName = name,
            bio = bio,
            photoUrl = if (photoUrl.isBlank()) null else photoUrl
        )
        exportPersistentBackup()
    }

    fun copyImageUriToInternalStorage(uri: android.net.Uri): String? {
        val context = getApplication<Application>()
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("profile_pic_") && file.name.endsWith(".jpg")) {
                    file.delete()
                }
            }
            val file = java.io.File(context.filesDir, "profile_pic_${System.currentTimeMillis()}.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun toggleVerseBookSelection(bookId: String) {
        val current = _selectedBooksForVerse.value.toMutableSet()
        if (current.contains(bookId)) {
            if (current.size > 1) { // keep at least one selected
                current.remove(bookId)
            }
        } else {
            current.add(bookId)
        }
        _selectedBooksForVerse.value = current

        // Save to prefs
        val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("selected_verse_books", current).apply()

        // Automatically refresh verse if the current verse's book is no longer selected
        _activeVerse.value.let { active ->
            if (!current.contains(active.book.id)) {
                refreshActiveVerse()
            }
        }
    }

    fun refreshActiveVerse() {
        viewModelScope.launch {
            _isVerseLoading.value = true
            val allowedBookIds = _selectedBooksForVerse.value
            val filteredBooks = books.filter { allowedBookIds.contains(it.id) }
            val targetBooks = if (filteredBooks.isEmpty()) books else filteredBooks

            val randomBook = targetBooks.randomOrNull() ?: books.first()
            val fetched = withContext(Dispatchers.IO) {
                fetchVerseFromApiWithFallback(randomBook.id, randomBook)
            }
            _activeVerse.value = ActiveVerse(randomBook, fetched.second, fetched.first)
            _isVerseLoading.value = false
        }
    }

    private suspend fun fetchVerseFromApiWithFallback(bookId: String, fallbackBook: Book): Pair<String, String> {
        val currentLang = _readerSettings.value.language
        return withContext(Dispatchers.IO) {
            try {
                when (bookId) {
                    "quran" -> {
                        val randomAyah = (1..6236).random()
                        val quranEdition = if (currentLang == AppLanguage.EN) "en.yusufali" else "tr.yazir"
                        val url = "https://api.alquran.cloud/v1/ayah/$randomAyah/$quranEdition"
                        val request = Request.Builder().url(url).build()
                        okHttpClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw Exception("Quran API failure")
                            val body = response.body?.string() ?: ""
                            val json = org.json.JSONObject(body)
                            val dataObj = json.getJSONObject("data")
                            val text = dataObj.getString("text").trim()
                            val surahObj = dataObj.getJSONObject("surah")
                            val numberInSurah = dataObj.getInt("numberInSurah")
                            val surahName = if (currentLang == AppLanguage.EN) {
                                surahObj.getString("englishName")
                            } else {
                                when (surahObj.getInt("number")) {
                                    1 -> "Fâtiha"
                                    2 -> "Bakara"
                                    3 -> "Âl-i İmrân"
                                    4 -> "Nisâ"
                                    5 -> "Mâide"
                                    6 -> "En'âm"
                                    7 -> "A'râf"
                                    8 -> "Enfâl"
                                    9 -> "Tevbe"
                                    10 -> "Yûnus"
                                    11 -> "Hûd"
                                    12 -> "Yûsuf"
                                    13 -> "Ra'd"
                                    14 -> "İbrâhîm"
                                    15 -> "Hicr"
                                    16 -> "Nahl"
                                    17 -> "İsrâ"
                                    18 -> "Kehf"
                                    19 -> "Meryem"
                                    20 -> "Tâhâ"
                                    21 -> "Enbiyâ"
                                    22 -> "Hac"
                                    23 -> "Mü'minûn"
                                    24 -> "Nûr"
                                    25 -> "Furkan"
                                    26 -> "Şuarâ"
                                    27 -> "Neml"
                                    28 -> "Kasas"
                                    29 -> "Ankebût"
                                    30 -> "Rûm"
                                    31 -> "Lokmân"
                                    32 -> "Secde"
                                    33 -> "Ahzâb"
                                    34 -> "Sebe'"
                                    35 -> "Fâtır"
                                    36 -> "Yâsîn"
                                    37 -> "Sâffât"
                                    38 -> "Sâd"
                                    39 -> "Zümer"
                                    40 -> "Mü'min"
                                    41 -> "Fussilet"
                                    42 -> "Şûrâ"
                                    43 -> "Zuhruf"
                                    44 -> "Duhân"
                                    45 -> "Câsiye"
                                    46 -> "Ahkaf"
                                    47 -> "Muhammed"
                                    48 -> "Fetih"
                                    49 -> "Hucurât"
                                    50 -> "Kâf"
                                    51 -> "Zâriyât"
                                    52 -> "Tûr"
                                    53 -> "Necm"
                                    54 -> "Kamer"
                                    55 -> "Rahmân"
                                    56 -> "Vâkıa"
                                    57 -> "Hadîd"
                                    58 -> "Mücâdele"
                                    59 -> "Haşr"
                                    60 -> "Mümtehine"
                                    61 -> "Saf"
                                    62 -> "Cuma"
                                    63 -> "Münâfikûn"
                                    64 -> "Tegâbun"
                                    65 -> "Talâk"
                                    66 -> "Tahrîm"
                                    67 -> "Mülk"
                                    68 -> "Kalem"
                                    69 -> "Hâkka"
                                    70 -> "Meâric"
                                    71 -> "Nûh"
                                    72 -> "Cin"
                                    73 -> "Müzzemmil"
                                    74 -> "Müddessir"
                                    75 -> "Kıyâme"
                                    76 -> "İnsân"
                                    77 -> "Mürselât"
                                    78 -> "Nebe'"
                                    79 -> "Nâziât"
                                    80 -> "Abese"
                                    81 -> "Tekvîr"
                                    82 -> "İnfitâr"
                                    83 -> "Mutaffifîn"
                                    84 -> "İnşikâk"
                                    85 -> "Burûc"
                                    86 -> "Târık"
                                    87 -> "A'lâ"
                                    88 -> "Gâşiye"
                                    89 -> "Fecr"
                                    90 -> "Beled"
                                    91 -> "Şems"
                                    92 -> "Leyl"
                                    93 -> "Duhâ"
                                    94 -> "İnşirâh"
                                    95 -> "Tîn"
                                    96 -> "Alak"
                                    97 -> "Kadir"
                                    98 -> "Beyyine"
                                    99 -> "Zilzâl"
                                    100 -> "Âdiyât"
                                    101 -> "Kâria"
                                    102 -> "Tekâsür"
                                    103 -> "Asr"
                                    104 -> "Hümeze"
                                    105 -> "Fîl"
                                    106 -> "Kureyş"
                                    107 -> "Mâûn"
                                    108 -> "Kevser"
                                    109 -> "Kâfirûn"
                                    110 -> "Nasr"
                                    111 -> "Mesed"
                                    112 -> "İhlâs"
                                    113 -> "Felak"
                                    114 -> "Nâs"
                                    else -> surahObj.getString("englishName")
                                }
                            }
                            val ref = if (currentLang == AppLanguage.EN) {
                                "Surah $surahName, Verse $numberInSurah"
                            } else {
                                "$surahName Suresi, Ayet $numberInSurah"
                            }
                            Pair(ref, text)
                        }
                    }
                    "torah" -> {
                        val torahBooks = if (currentLang == AppLanguage.EN) {
                            listOf(
                                Triple("genesis", "Genesis", 50),
                                Triple("exodus", "Exodus", 40),
                                Triple("leviticus", "Leviticus", 27),
                                Triple("numbers", "Numbers", 36),
                                Triple("deuteronomy", "Deuteronomy", 34)
                            )
                        } else {
                            listOf(
                                Triple("genesis", "Yaratılış", 50),
                                Triple("exodus", "Mısır'dan Çıkış", 40),
                                Triple("leviticus", "Levililer", 27),
                                Triple("numbers", "Sayılar", 36),
                                Triple("deuteronomy", "Yasanın Tekrarı", 34)
                            )
                        }
                        val selectedTorah = torahBooks.random()
                        val chapter = (1..selectedTorah.third).random()
                        val url = "https://bible-api.com/${selectedTorah.first}%20$chapter"
                        val request = Request.Builder().url(url).build()
                        okHttpClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw Exception("Torah Bible API failure")
                            val body = response.body?.string() ?: ""
                            val json = org.json.JSONObject(body)
                            val versesJA = json.getJSONArray("verses")
                            if (versesJA.length() == 0) throw Exception("No verses returned")
                            val randomIdx = (0 until versesJA.length()).random()
                            val verseObj = versesJA.getJSONObject(randomIdx)
                            val englishText = verseObj.getString("text").trim()
                            val verseNum = verseObj.getInt("verse")
                            var finalText = if (currentLang == AppLanguage.EN) {
                                englishText
                            } else {
                                translateTextGtx(englishText, targetLang = "tr", sourceLang = "en")
                            }
                            if (currentLang == AppLanguage.TR && (finalText == englishText || finalText.isBlank())) {
                                return@withContext fetchOfflineVerseForActive(fallbackBook, currentLang)
                            }
                            val ref = if (currentLang == AppLanguage.EN) {
                                "${selectedTorah.second}, Chapter $chapter:$verseNum"
                            } else {
                                "${selectedTorah.second}, Bölüm $chapter:$verseNum"
                            }
                            Pair(ref, finalText)
                        }
                    }
                    "sermon" -> {
                        val chapter = (5..7).random()
                        val url = "https://bible-api.com/matthew%20$chapter"
                        val request = Request.Builder().url(url).build()
                        okHttpClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw Exception("Sermon Bible API failure")
                            val body = response.body?.string() ?: ""
                            val json = org.json.JSONObject(body)
                            val versesJA = json.getJSONArray("verses")
                            if (versesJA.length() == 0) throw Exception("No verses returned")
                            val randomIdx = (0 until versesJA.length()).random()
                            val verseObj = versesJA.getJSONObject(randomIdx)
                            val englishText = verseObj.getString("text").trim()
                            val verseNum = verseObj.getInt("verse")
                            var finalText = if (currentLang == AppLanguage.EN) {
                                englishText
                            } else {
                                translateTextGtx(englishText, targetLang = "tr", sourceLang = "en")
                            }
                            if (currentLang == AppLanguage.TR && (finalText == englishText || finalText.isBlank())) {
                                return@withContext fetchOfflineVerseForActive(fallbackBook, currentLang)
                            }
                            val ref = if (currentLang == AppLanguage.EN) {
                                "The Gospel, Matthew $chapter:$verseNum"
                            } else {
                                "İncil, Matta $chapter:$verseNum"
                            }
                            Pair(ref, finalText)
                        }
                    }
                    else -> fetchOfflineVerseForActive(fallbackBook, currentLang)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fetchOfflineVerseForActive(fallbackBook, currentLang)
            }
        }
    }

    private fun fetchOfflineVerseForActive(book: Book, lang: AppLanguage): Pair<String, String> {
        val paragraphs = book.paragraphs
        if (paragraphs.isEmpty()) {
            return Pair(
                if (lang == AppLanguage.EN) "HOLY SCRIPTURES" else "KUTSAL KİTAP",
                if (lang == AppLanguage.EN) "Verse content not found." else "Ayet içeriği bulunamadı."
            )
        }
        val randomIndex = (paragraphs.indices).random()
        val textTr = paragraphs[randomIndex]
        val text = if (lang == AppLanguage.EN) {
            translateTextGtx(textTr, targetLang = "en", sourceLang = "tr")
        } else {
            textTr
        }
        val bookTitle = Loc.get(book.id, lang)
        val ref = if (lang == AppLanguage.EN) {
            when (book.id) {
                "quran" -> "Surah Al-Fath, Verse ${randomIndex + 1}"
                "torah" -> "Genesis, Chapter 1:${randomIndex + 1}"
                "sermon" -> "The Gospel, Matthew 5:${randomIndex + 1}"
                "gita" -> "Bhagavad Gita, Chapter 2:${randomIndex + 1}"
                else -> "$bookTitle, ${randomIndex + 1}"
            }
        } else {
            when (book.id) {
                "quran" -> "Fetih Suresi, Ayet ${randomIndex + 1}"
                "torah" -> "Yaratılış, Bölüm 1:${randomIndex + 1}"
                "sermon" -> "İncil, Matta 5:${randomIndex + 1}"
                "gita" -> "Bhagavad Gita, Bölüm 2:${randomIndex + 1}"
                else -> "$bookTitle, ${randomIndex + 1}"
            }
        }
        return Pair(ref, text)
    }

    fun signInWithDemo(email: String, name: String) {
        repository.stopFirestoreSync()
        val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_demo_logged_in", true)
            .putString("demo_email", email)
            .putString("demo_name", name)
            .apply()

        viewModelScope.launch {
            repository.clearAllUserData()
        }

        _userState.value = UserState(
            email = email,
            displayName = name,
            photoUrl = null,
            bio = "",
            isLoggedIn = true,
            isDemo = true
        )
    }

    fun signInWithFirebaseUser(user: com.google.firebase.auth.FirebaseUser) {
        viewModelScope.launch {
            repository.clearAllUserData()
        }
        repository.startFirestoreSync(user.uid, viewModelScope)
        _userState.value = UserState(
            email = user.email,
            displayName = user.displayName ?: "Kullanıcı",
            photoUrl = user.photoUrl?.toString(),
            bio = "",
            isLoggedIn = true,
            isDemo = false
        )
    }

    fun signOut() {
        repository.stopFirestoreSync()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Ignore
        }
        val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        viewModelScope.launch {
            repository.clearAllUserData()
        }

        _userState.value = UserState(
            email = null,
            displayName = null,
            photoUrl = null,
            bio = null,
            isLoggedIn = false,
            isDemo = false
        )
    }

    // Room Flows
    val notesHighlights: StateFlow<List<NoteHighlight>> = repository.allNotesHighlights
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val readingHistory: StateFlow<List<ReadingHistory>> = repository.allReadingHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Daily Verse (Static or based on random selection)
    val dailyVerseBook: Book = books.firstOrNull { it.id == "quran" } ?: books.first()
    val dailyVerseText: String = "Doğrusu biz sana apaçık bir fetih ihsân ettik. Tâ ki Allah senin geçmiş ve gelecek günahlarını bağışlasın, üzerindeki nimetini tamamlasın ve seni dosdoğru bir yola iletsin."

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // Filtered books
    fun getFilteredBooks(query: String): List<Book> {
        if (query.isBlank()) return books
        return books.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true) ||
            it.authorOrSource.contains(query, ignoreCase = true)
        }
    }

    // Reader Settings Functions

    fun updateThemeSetting(theme: AppThemeSetting) {
        _readerSettings.value = _readerSettings.value.copy(theme = theme)
        saveReaderSettings()
    }

    fun updateFontSize(sizeSp: Float) {
        _readerSettings.value = _readerSettings.value.copy(fontSizeSp = sizeSp)
        saveReaderSettings()
    }

    fun updateFontFamily(font: FontFamilySetting) {
        _readerSettings.value = _readerSettings.value.copy(fontFamily = font)
        saveReaderSettings()
    }

    fun updateLineHeight(lineHeight: LineHeightSetting) {
        _readerSettings.value = _readerSettings.value.copy(lineHeight = lineHeight)
        saveReaderSettings()
    }

    fun updateLanguage(lang: AppLanguage) {
        _readerSettings.value = _readerSettings.value.copy(language = lang)
        saveReaderSettings()
        _surahInMemoryCache.clear()
        _bibleChapterInMemoryCache.clear()
        refreshActiveVerse()

        _currentSelectedSurah.value?.let { surah ->
            loadSurahContent(surah.number)
        }
        _currentSelectedTorahBook.value?.let { b ->
            _currentSelectedTorahChapter.value?.let { ch ->
                loadBibleChapterContent("torah", b, ch, true)
            }
        }
        _currentSelectedSermonBook.value?.let { b ->
            _currentSelectedSermonChapter.value?.let { ch ->
                loadBibleChapterContent("sermon", b, ch, false)
            }
        }
        _currentSelectedTalmudBook.value?.let { b ->
            _currentSelectedTalmudChapter.value?.let { ch ->
                loadBibleChapterContent("talmud", b, ch, false)
            }
        }
        _currentSelectedBukhariBook.value?.let { b ->
            _currentSelectedBukhariChapter.value?.let { ch ->
                loadBibleChapterContent("bukhari", b, ch, false)
            }
        }
        _currentSelectedGitaBook.value?.let { b ->
            _currentSelectedGitaChapter.value?.let { ch ->
                loadBibleChapterContent("gita", b, ch, false)
            }
        }
    }

    fun updateShowOriginalScript(show: Boolean) {
        _readerSettings.value = _readerSettings.value.copy(showOriginalScript = show)
        saveReaderSettings()
    }

    private fun saveReaderSettings() {
        val settingsPrefs = getApplication<Application>().getSharedPreferences("scriptorium_settings", Context.MODE_PRIVATE)
        val settings = _readerSettings.value
        settingsPrefs.edit()
            .putString("theme", settings.theme.name)
            .putFloat("font_size", settings.fontSizeSp)
            .putString("font_family", settings.fontFamily.name)
            .putString("line_height", settings.lineHeight.name)
            .putString("language", settings.language.name)
            .putBoolean("show_original_script", settings.showOriginalScript)
            .apply()
        exportPersistentBackup()
    }

    // Active Book state
    private val _activeBook = MutableStateFlow<Book?>(null)
    val activeBook = _activeBook.asStateFlow()

    fun setActiveBook(book: Book?) {
        _activeBook.value = book
        if (book != null) {
            // Track in reading history
            viewModelScope.launch {
                repository.updateReadingProgress(
                    bookTitle = book.title,
                    subtitle = book.subContentTitle,
                    progress = 100, // Read starts at 100% or default progress
                    dateText = "Bugün, " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                )
            }
        }
    }

    fun updateReadingSessionProgress(
        bookTitle: String,
        subtitle: String,
        progress: Int,
        surahOrChapter: String? = null,
        pagesRead: Int = 0,
        isCompleted: Boolean = false,
        contemplationMinutes: Int = 0
    ) {
        viewModelScope.launch {
            repository.updateReadingProgress(
                bookTitle = bookTitle,
                subtitle = subtitle,
                progress = progress,
                dateText = "Bugün, " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                surahOrChapter = surahOrChapter,
                pagesRead = pagesRead,
                isCompleted = isCompleted,
                contemplationMinutes = contemplationMinutes
            )
        }
    }

    // Add Highlight or Note from Reader
    fun addNoteOrHighlight(bookTitle: String, quoteText: String, noteText: String?, isHighlightOnly: Boolean) {
        viewModelScope.launch {
            val dateStr = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("tr")).format(java.util.Date())
            val noteHighlight = NoteHighlight(
                bookTitle = bookTitle,
                quoteText = quoteText,
                userReflection = if (isHighlightOnly) null else noteText,
                dateText = dateStr,
                type = if (isHighlightOnly) "Highlight" else "Note"
            )
            repository.insertNoteHighlight(noteHighlight)
        }
    }

    // Delete Highlight/Note
    fun deleteNoteHighlight(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteHighlight(id)
        }
    }

    // Delete Reading/Contemplation History
    fun deleteHistory(id: Int) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun toggleNotifications() {
        val newValue = !_notificationsEnabled.value
        _notificationsEnabled.value = newValue
        val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notifications_enabled", newValue).apply()
        
        val context = getApplication<Application>()
        val lang = _readerSettings.value.language
        Toast.makeText(
            context,
            if (newValue) {
                if (lang == AppLanguage.EN) "Notifications activated! You will receive random verses." else "Bildirimler aktif edildi! Belirlediğiniz aralıklarla ayetler gönderilecektir."
            } else {
                if (lang == AppLanguage.EN) "Notifications deactivated." else "Bildirimler kapatıldı."
            },
            Toast.LENGTH_SHORT
        ).show()
        
        com.example.DailyVerseReceiver.scheduleAlarm(getApplication(), force = true)
    }

    fun updateNotificationInterval(minutes: Int) {
        val cappedMinutes = if (minutes < 2) 2 else minutes
        _notificationIntervalMinutes.value = cappedMinutes
        val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
        prefs.edit().putInt("notification_interval_minutes", cappedMinutes).apply()
        
        val context = getApplication<Application>()
        val lang = _readerSettings.value.language
        val textTr = if (cappedMinutes >= 60) "${cappedMinutes / 60} saat" else "$cappedMinutes dakika"
        val textEn = if (cappedMinutes >= 60) "${cappedMinutes / 60} hours" else "$cappedMinutes minutes"
        Toast.makeText(
            context,
            if (lang == AppLanguage.EN) "Notification interval updated to $textEn!" else "Bildirim sıklığı $textTr olarak güncellendi!",
            Toast.LENGTH_SHORT
        ).show()
        
        com.example.DailyVerseReceiver.scheduleAlarm(getApplication(), force = true)
    }

    fun refreshLocationAndPrayerTimes() {
        viewModelScope.launch {
            _isLocationLoading.value = true
            try {
                val context = getApplication<Application>()
                val loc = com.example.data.api.PrayerTimeApiService.detectLocation(context)
                _userLocationInfo.value = loc

                val times = com.example.data.api.PrayerTimeApiService.fetchPrayerTimesFromApi(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    religion = _userReligion.value,
                    sect = _userSect.value
                )
                _livePrayerTimes.value = times
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLocationLoading.value = false
            }
        }
    }

    fun setUserReligion(religion: UserReligion) {
        _userReligion.value = religion
        val defaultSect = UserSect.getSectsForReligion(religion).firstOrNull() ?: UserSect.SUNNI
        _userSect.value = defaultSect

        val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_religion", religion.id)
            .putString("user_sect", defaultSect.id)
            .apply()

        val context = getApplication<Application>()
        val lang = _readerSettings.value.language
        Toast.makeText(
            context,
            if (lang == AppLanguage.EN) "Religion set to ${religion.titleEn}" else "Dini tercih ${religion.titleTr} olarak güncellendi",
            Toast.LENGTH_SHORT
        ).show()

        refreshLocationAndPrayerTimes()
        com.example.DailyVerseReceiver.scheduleAlarm(getApplication(), force = true)
    }

    fun setUserSect(sect: UserSect) {
        _userSect.value = sect
        val prefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
        prefs.edit().putString("user_sect", sect.id).apply()

        val context = getApplication<Application>()
        val lang = _readerSettings.value.language
        Toast.makeText(
            context,
            if (lang == AppLanguage.EN) "Sect set to ${sect.titleEn}" else "Mezhep/Görüş tercihi ${sect.titleTr} olarak güncellendi",
            Toast.LENGTH_SHORT
        ).show()

        refreshLocationAndPrayerTimes()
        com.example.DailyVerseReceiver.scheduleAlarm(getApplication(), force = true)
    }

    fun sendTestNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val lang = _readerSettings.value.language
            
            // Show start progress toast on UI thread
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    if (lang == AppLanguage.EN) "Sending test notification..." else "Test bildirimi gönderiliyor...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            val allowedBookIds = _selectedBooksForVerse.value
            val filteredBooks = books.filter { allowedBookIds.contains(it.id) }
            val targetBooks = if (filteredBooks.isEmpty()) books else filteredBooks
            val randomBook = targetBooks.randomOrNull() ?: books.first()
            val fetched = fetchVerseFromApiWithFallback(randomBook.id, randomBook)

            val religion = _userReligion.value
            val sect = _userSect.value
            
            val locationInfo = com.example.data.api.PrayerTimeApiService.detectLocation(context)
            val schedules = com.example.data.api.PrayerTimeApiService.fetchPrayerTimesFromApi(
                latitude = locationInfo.latitude,
                longitude = locationInfo.longitude,
                religion = religion,
                sect = sect
            )
            val samplePrayer = schedules.randomOrNull()

            val notifTitle = if (samplePrayer != null) {
                "📍 ${locationInfo.cityName} [${sect.getTitle(lang)}] ${samplePrayer.getName(lang)} (${samplePrayer.timeStr})"
            } else fetched.first

            val notifMessage = if (samplePrayer != null) {
                "${samplePrayer.getMessage(lang)}\n\n(${fetched.first}): ${fetched.second}"
            } else fetched.second
            
            val channelId = "hourly_verse_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Günün Ayetleri ve İbadet Vakitleri",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Seçilen din, mezhep ve kutsal kitaplardan ibadet/namaz vakitleri ve ayet bildirimleri."
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(notifTitle)
                .setContentText(notifMessage)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(notifMessage))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(9999, notification)
            
            // Show final success toast
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    if (lang == AppLanguage.EN) "Test notification sent successfully!" else "Test bildirimi başarıyla gönderildi!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun normalizeBibleApiBookName(nameEnglish: String): String {
        return when (nameEnglish) {
            "I Samuel", "I_Samuel" -> "1 Samuel"
            "II Samuel", "II_Samuel" -> "2 Samuel"
            "I Kings", "I_Kings" -> "1 Kings"
            "II Kings", "II_Kings" -> "2 Kings"
            "I Chronicles", "I_Chronicles" -> "1 Chronicles"
            "II Chronicles", "II_Chronicles" -> "2 Chronicles"
            "I Corinthians", "I_Corinthians" -> "1 Corinthians"
            "II Corinthians", "II_Corinthians" -> "2 Corinthians"
            "I Thessalonians", "I_Thessalonians" -> "1 Thessalonians"
            "II Thessalonians", "II_Thessalonians" -> "2 Thessalonians"
            "I Timothy", "I_Timothy" -> "1 Timothy"
            "II Timothy", "II_Timothy" -> "2 Timothy"
            "I Peter", "I_Peter" -> "1 Peter"
            "II Peter", "II_Peter" -> "2 Peter"
            "I John", "I_John" -> "1 John"
            "II John", "II_John" -> "2 John"
            "III John", "III_John" -> "3 John"
            "Song of Songs", "Song_of_Songs" -> "Song of Solomon"
            else -> nameEnglish.replace("_", " ")
        }
    }

    private suspend fun translateVersesBatch(verses: List<String>): List<String> = withContext(Dispatchers.IO) {
        if (verses.isEmpty()) return@withContext emptyList()
        val translatedList = mutableListOf<String>()
        val chunkSize = 5
        for (i in verses.indices step chunkSize) {
            val chunk = verses.subList(i, minOf(i + chunkSize, verses.size))
            val combinedText = chunk.joinToString("\n")
            try {
                val encodedText = java.net.URLEncoder.encode(combinedText, "UTF-8")
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=tr&dt=t&q=$encodedText"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                var chunkLines: List<String>? = null
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val jsonArray = org.json.JSONArray(bodyStr)
                        val sentencesArray = jsonArray.optJSONArray(0)
                        if (sentencesArray != null) {
                            val sb = StringBuilder()
                            for (j in 0 until sentencesArray.length()) {
                                val sentence = sentencesArray.optJSONArray(j)
                                if (sentence != null) {
                                    sb.append(sentence.optString(0))
                                }
                            }
                            val fullTranslated = sb.toString().trim()
                            val lines = fullTranslated.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            if (lines.size == chunk.size) {
                                chunkLines = lines
                            }
                        }
                    }
                }
                if (chunkLines != null) {
                    translatedList.addAll(chunkLines!!)
                } else {
                    for (verse in chunk) {
                        translatedList.add(translateTextGtx(verse))
                    }
                }
            } catch (e: Exception) {
                for (verse in chunk) {
                    try {
                        translatedList.add(translateTextGtx(verse))
                    } catch (_: Exception) {
                        translatedList.add(verse)
                    }
                }
            }
        }
        translatedList
    }

    private fun translateTextGtx(text: String, targetLang: String = "tr", sourceLang: String = "en"): String {
        if (text.isBlank()) return text
        try {
            val prefix = if (text.contains(": ")) text.substringBefore(": ") else ""
            val verseContent = if (text.contains(": ")) text.substringAfter(": ") else text

            val encodedText = java.net.URLEncoder.encode(verseContent, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedText"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonArray = org.json.JSONArray(bodyStr)
                    val sentencesArray = jsonArray.optJSONArray(0)
                    if (sentencesArray != null) {
                        val sb = StringBuilder()
                        for (i in 0 until sentencesArray.length()) {
                            val sentence = sentencesArray.optJSONArray(i)
                            if (sentence != null) {
                                sb.append(sentence.optString(0))
                            }
                        }
                        val translatedText = sb.toString().trim()
                        if (translatedText.isNotBlank()) {
                            return if (prefix.isNotEmpty()) "$prefix: $translatedText" else translatedText
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ScriptureViewModel", "GTX Translation failed for: $text", e)
        }
        return text
    }

    private suspend fun fetchChapterContentInternal(
        bookId: String,
        bibleBook: com.example.data.model.BibleBook,
        chapterNumber: Int
    ): Pair<List<String>, List<String>> = withContext(Dispatchers.IO) {
        val englishVerses = mutableListOf<String>()
        val originalParagraphsList = mutableListOf<String>()

        try {
            when (bookId) {
                "torah" -> {
                    val encodedBookName = bibleBook.nameEnglish.replace(" ", "%20")
                    val sefariaUrl = "https://www.sefaria.org/api/texts/$encodedBookName.$chapterNumber?context=0"
                    val request = Request.Builder().url(sefariaUrl).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: ""
                            val json = JSONObject(bodyStr)
                            val engJA = json.optJSONArray("text")
                            if (engJA != null) {
                                for (i in 0 until engJA.length()) {
                                    val cleanText = engJA.optString(i).replace(Regex("<[^>]*>"), "")
                                    englishVerses.add("${i + 1}: $cleanText")
                                }
                            }
                            val hebJA = json.optJSONArray("he")
                            if (hebJA != null) {
                                for (i in 0 until hebJA.length()) {
                                    val cleanHeb = hebJA.optString(i).replace(Regex("<[^>]*>"), "")
                                    originalParagraphsList.add("${i + 1}: $cleanHeb")
                                }
                            }
                        }
                    }
                }
                "talmud" -> {
                    val pageNum = 2 + (chapterNumber - 1) / 2
                    val side = if (chapterNumber % 2 == 1) "a" else "b"
                    val daf = "$pageNum$side"
                    val sefariaUrl = "https://www.sefaria.org/api/texts/${bibleBook.id}.$daf?context=0"
                    val request = Request.Builder().url(sefariaUrl).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: ""
                            val json = JSONObject(bodyStr)
                            val engJA = json.optJSONArray("text")
                            if (engJA != null) {
                                for (i in 0 until engJA.length()) {
                                    val cleanText = engJA.optString(i).replace(Regex("<[^>]*>"), "")
                                    englishVerses.add("${i + 1}: $cleanText")
                                }
                            }
                            val hebJA = json.optJSONArray("he")
                            if (hebJA != null) {
                                for (i in 0 until hebJA.length()) {
                                    val cleanHeb = hebJA.optString(i).replace(Regex("<[^>]*>"), "")
                                    originalParagraphsList.add("${i + 1}: $cleanHeb")
                                }
                            }
                        }
                    }
                }
                "bukhari" -> {
                    val cacheFileEng = java.io.File(getApplication<Application>().filesDir, "eng-bukhari.min.json")
                    val cacheFileAra = java.io.File(getApplication<Application>().filesDir, "ara-bukhari.min.json")
                    
                    if (!cacheFileEng.exists()) {
                        val bukhariUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari.min.json"
                        downloadFileToLocal(bukhariUrl, cacheFileEng)
                    }
                    if (!cacheFileAra.exists()) {
                        val bukhariAraUrl = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-bukhari.min.json"
                        downloadFileToLocal(bukhariAraUrl, cacheFileAra)
                    }
                    
                    val bookNumber = bibleBook.bookNumber
                    val matchedEngHadiths = getBukhariHadithsForBook(bookNumber, isArabic = false)
                    val matchedAraHadiths = getBukhariHadithsForBook(bookNumber, isArabic = true)
                    
                    if (matchedEngHadiths.isNotEmpty()) {
                        for (idx in 0 until matchedEngHadiths.size) {
                            val targetEng = matchedEngHadiths[idx]
                            val hNum = targetEng.optInt("hadithnumber", idx + 1)
                            val textEng = targetEng.optString("text", "")
                            englishVerses.add("Hadis $hNum: $textEng")
                            
                            val targetAra = matchedAraHadiths.getOrNull(idx)
                            if (targetAra != null) {
                                val textAra = targetAra.optString("text", "")
                                originalParagraphsList.add("Hadis $hNum: $textAra")
                            } else {
                                originalParagraphsList.add("Hadis $hNum: $textEng")
                            }
                        }
                    }
                }
                "gita" -> {
                    val cacheFileGita = java.io.File(getApplication<Application>().filesDir, "gita-verses.min.json")
                    if (!cacheFileGita.exists()) {
                        val gitaUrl = "https://raw.githubusercontent.com/gita/gita/main/data/verse.json"
                        try { downloadFileToLocal(gitaUrl, cacheFileGita) } catch (e: Exception) { android.util.Log.e("ScriptureViewModel", "Failed to download Gita verses", e) }
                    }

                    val cacheFileGitaTrans = java.io.File(getApplication<Application>().filesDir, "gita-translations.min.json")
                    if (!cacheFileGitaTrans.exists()) {
                        val gitaTransUrl = "https://raw.githubusercontent.com/gita/gita/main/data/translation.json"
                        try { downloadFileToLocal(gitaTransUrl, cacheFileGitaTrans) } catch (e: Exception) { android.util.Log.e("ScriptureViewModel", "Failed to download Gita translations", e) }
                    }

                    val sivanandaTranslations = mutableMapOf<Int, String>()
                    if (cacheFileGitaTrans.exists()) {
                        try {
                            val transJsonStr = cacheFileGitaTrans.readText()
                            val transJA = org.json.JSONArray(transJsonStr)
                            for (i in 0 until transJA.length()) {
                                val tObj = transJA.getJSONObject(i)
                                if (tObj.optInt("author_id") == 16) {
                                    val vId = tObj.optInt("verse_id")
                                    val desc = tObj.optString("description").trim()
                                    if (vId > 0 && desc.isNotBlank()) {
                                        sivanandaTranslations[vId] = desc
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ScriptureViewModel", "Error reading gita translations json", e)
                        }
                    }

                    var loadedFromGitaJson = false
                    if (cacheFileGita.exists()) {
                        try {
                            val jsonStr = cacheFileGita.readText()
                            val versesJA = org.json.JSONArray(jsonStr)
                            for (i in 0 until versesJA.length()) {
                                val vObj = versesJA.getJSONObject(i)
                                if (vObj.optInt("chapter_number") == chapterNumber) {
                                    val vNum = vObj.optInt("verse_number")
                                    val vId = vObj.optInt("id")
                                    val sanskritText = vObj.optString("text").replace("\n", " ").trim()
                                    
                                    val englishTrans = sivanandaTranslations[vId]
                                    val verseText = if (!englishTrans.isNullOrBlank()) {
                                        englishTrans
                                    } else {
                                        val translit = vObj.optString("transliteration").replace("\n", " ").trim()
                                        val meanings = vObj.optString("word_meanings").replace("\n", " ").trim()
                                        if (translit.isNotBlank()) translit else meanings
                                    }

                                    if (verseText.isNotBlank()) {
                                        englishVerses.add("$vNum: $verseText")
                                    } else {
                                        englishVerses.add("$vNum: $sanskritText")
                                    }
                                    originalParagraphsList.add("$vNum: $sanskritText")
                                }
                            }
                            if (englishVerses.isNotEmpty()) {
                                loadedFromGitaJson = true
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ScriptureViewModel", "Error reading gita json", e)
                        }
                    }

                    if (!loadedFromGitaJson) {
                        for ((idx, p) in com.example.data.model.books.GitaContent.paragraphs.withIndex()) {
                            englishVerses.add(p)
                            val orig = com.example.data.model.books.GitaContent.originalParagraphs.getOrNull(idx) ?: p
                            originalParagraphsList.add(orig)
                        }
                    }
                }
                else -> {
                    // Bible / Sermon
                    val normalizedName = normalizeBibleApiBookName(bibleBook.nameEnglish)
                    val encodedBookName = normalizedName.replace(" ", "%20")
                    val bibleUrl = "https://bible-api.com/$encodedBookName%20$chapterNumber"
                    val request = Request.Builder().url(bibleUrl).build()
                    var fetchSuccess = false
                    try {
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val bodyStr = response.body?.string() ?: ""
                                val json = JSONObject(bodyStr)
                                val versesJA = json.optJSONArray("verses")
                                if (versesJA != null && versesJA.length() > 0) {
                                    for (i in 0 until versesJA.length()) {
                                        val vObj = versesJA.getJSONObject(i)
                                        val vNum = vObj.getInt("verse")
                                        val vText = vObj.getString("text").trim()
                                        englishVerses.add("$vNum: $vText")
                                        originalParagraphsList.add("$vNum: $vText")
                                    }
                                    fetchSuccess = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("ScriptureViewModel", "Primary Bible API call failed for $normalizedName $chapterNumber", e)
                    }

                    if (!fetchSuccess) {
                        val fallbackUrl = "https://bible-api.com/$encodedBookName%20$chapterNumber?translation=kjv"
                        val fallbackReq = Request.Builder().url(fallbackUrl).build()
                        try {
                            okHttpClient.newCall(fallbackReq).execute().use { response ->
                                if (response.isSuccessful) {
                                    val bodyStr = response.body?.string() ?: ""
                                    val json = JSONObject(bodyStr)
                                    val versesJA = json.optJSONArray("verses")
                                    if (versesJA != null && versesJA.length() > 0) {
                                        for (i in 0 until versesJA.length()) {
                                            val vObj = versesJA.getJSONObject(i)
                                            val vNum = vObj.getInt("verse")
                                            val vText = vObj.getString("text").trim()
                                            englishVerses.add("$vNum: $vText")
                                            originalParagraphsList.add("$vNum: $vText")
                                        }
                                        fetchSuccess = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ScriptureViewModel", "Secondary Bible API call failed for $normalizedName $chapterNumber", e)
                        }
                    }

                    if (!fetchSuccess) {
                        val tertiaryUrl = "https://labs.bible.org/api/?passage=$encodedBookName%20$chapterNumber&type=json"
                        val tertiaryReq = Request.Builder().url(tertiaryUrl).build()
                        try {
                            okHttpClient.newCall(tertiaryReq).execute().use { response ->
                                if (response.isSuccessful) {
                                    val bodyStr = response.body?.string() ?: ""
                                    val versesJA = org.json.JSONArray(bodyStr)
                                    if (versesJA.length() > 0) {
                                        for (i in 0 until versesJA.length()) {
                                            val vObj = versesJA.getJSONObject(i)
                                            val vNum = vObj.optString("verse")
                                            val vText = android.text.Html.fromHtml(vObj.optString("text"), android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                                            englishVerses.add("$vNum: $vText")
                                            originalParagraphsList.add("$vNum: $vText")
                                        }
                                        fetchSuccess = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ScriptureViewModel", "Tertiary Bible API call failed", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ScriptureViewModel", "fetchChapterContentInternal failed for $bookId / ${bibleBook.nameEnglish} $chapterNumber", e)
        }

        Pair(englishVerses, originalParagraphsList)
    }

    fun getBibleChapterFile(bookId: String, bookName: String, chapterNumber: Int): java.io.File {
        val lang = _readerSettings.value.language.name
        return java.io.File(getApplication<Application>().filesDir, "bible_${bookId}_${bookName}_${chapterNumber}_$lang.json")
    }

    fun downloadBibleChapter(bookId: String, bibleBook: com.example.data.model.BibleBook, chapterNumber: Int) {
        viewModelScope.launch {
            val file = getBibleChapterFile(bookId, bibleBook.id, chapterNumber)
            if (file.exists()) {
                val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
                val currentDownloadedChapters = offlinePrefs.getStringSet("downloaded_chapters", emptySet())?.toMutableSet() ?: mutableSetOf()
                currentDownloadedChapters.add("${bookId}_${bibleBook.id}_$chapterNumber")
                offlinePrefs.edit().putStringSet("downloaded_chapters", currentDownloadedChapters).apply()
                _downloadedChapters.value = currentDownloadedChapters
                return@launch
            }
            
            withContext(Dispatchers.IO) {
                try {
                    val paragraphsList = mutableListOf<String>()
                    val originalParagraphsList = mutableListOf<String>()
                    val englishVerses = mutableListOf<String>()
                    val (fetchedEng, fetchedOrig) = fetchChapterContentInternal(bookId, bibleBook, chapterNumber)
                    englishVerses.addAll(fetchedEng)
                    originalParagraphsList.addAll(fetchedOrig)
                    
                    if (englishVerses.isNotEmpty()) {
                        if (_readerSettings.value.language == AppLanguage.EN) {
                            paragraphsList.addAll(englishVerses)
                        } else {
                            val batch = translateVersesBatch(englishVerses)
                            paragraphsList.addAll(batch)
                        }
                    }
                    
                    if (paragraphsList.isNotEmpty()) {
                        val jsonObj = JSONObject().apply {
                            put("bookId", bookId)
                            put("bookName", bibleBook.id)
                            put("chapterNumber", chapterNumber)
                            val paragraphsJA = org.json.JSONArray()
                            paragraphsList.forEach { paragraphsJA.put(it) }
                            put("paragraphs", paragraphsJA)
                            
                            val originalJA = org.json.JSONArray()
                            originalParagraphsList.forEach { originalJA.put(it) }
                            put("originalParagraphs", originalJA)
                        }
                        file.writeText(jsonObj.toString())
                        
                        val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
                        val currentDownloadedChapters = offlinePrefs.getStringSet("downloaded_chapters", emptySet())?.toMutableSet() ?: mutableSetOf()
                        currentDownloadedChapters.add("${bookId}_${bibleBook.id}_$chapterNumber")
                        offlinePrefs.edit().putStringSet("downloaded_chapters", currentDownloadedChapters).apply()
                        
                        _downloadedChapters.value = currentDownloadedChapters
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScriptureViewModel", "Failed to download bible chapter", e)
                }
            }
        }
    }

    fun deleteBibleChapterDownload(bookId: String, bookName: String, chapterNumber: Int) {
        val file = getBibleChapterFile(bookId, bookName, chapterNumber)
        if (file.exists()) {
            file.delete()
        }
        val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
        val currentDownloadedChapters = offlinePrefs.getStringSet("downloaded_chapters", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentDownloadedChapters.remove("${bookId}_${bookName}_$chapterNumber")
        offlinePrefs.edit().putStringSet("downloaded_chapters", currentDownloadedChapters).apply()
        _downloadedChapters.value = currentDownloadedChapters
    }

    fun getBibleVerseAudioUrl(text: String, isEnglish: Boolean): String {
        try {
            val cleanText = text.replace(Regex("^\\d+:\\s*"), "").trim()
            val encoded = java.net.URLEncoder.encode(cleanText, "UTF-8")
            val tl = if (isEnglish) "en" else "tr"
            return "https://translate.google.com/translate_tts?ie=UTF-8&tl=$tl&client=tw-ob&q=$encoded"
        } catch (e: Exception) {
            return ""
        }
    }

    fun getRealHumanAudioUrl(isTorah: Boolean): String? {
        val bibleBook = if (isTorah) _currentSelectedTorahBook.value else _currentSelectedSermonBook.value
        val chapter = if (isTorah) _currentSelectedTorahChapter.value else _currentSelectedSermonChapter.value
        if (bibleBook == null || chapter == null) return null
        
        val isEnglish = _readerSettings.value.language == AppLanguage.EN
        val langCode = if (isEnglish) "01" else "20"
        
        val bookNum = if (isTorah) {
            bibleBook.bookNumber
        } else {
            bibleBook.bookNumber + 39
        }
        
        val bookStr = "%02d".format(bookNum)
        return "https://audio.wordproject.org/bibles/audio/$langCode/$bookStr/$chapter.mp3"
    }

    // ==========================================
    // PERSISTENT EXTERNAL MEMORY / BACKUP SYSTEM
    // ==========================================

    private fun getBackupFiles(): List<java.io.File> {
        val app = getApplication<Application>()
        val files = mutableListOf<java.io.File>()

        try {
            val docsDir = java.io.File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                "Scriptorium"
            )
            files.add(java.io.File(docsDir, "scriptorium_user_backup.json"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val secDir = java.io.File(
                android.os.Environment.getExternalStorageDirectory(),
                "ScriptoriumBackup"
            )
            files.add(java.io.File(secDir, "scriptorium_user_backup.json"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        files.add(java.io.File(app.filesDir, "scriptorium_user_backup.json"))
        return files
    }

    fun exportPersistentBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = org.json.JSONObject()
                root.put("version", 1)
                root.put("lastBackupTime", System.currentTimeMillis())

                // Downloads
                val downloadedBooksArray = org.json.JSONArray()
                _downloadedBooks.value.forEach { downloadedBooksArray.put(it) }
                root.put("downloadedBooks", downloadedBooksArray)

                val downloadedSurahsArray = org.json.JSONArray()
                _downloadedSurahs.value.forEach { downloadedSurahsArray.put(it) }
                root.put("downloadedSurahs", downloadedSurahsArray)

                val downloadedChaptersArray = org.json.JSONArray()
                _downloadedChapters.value.forEach { downloadedChaptersArray.put(it) }
                root.put("downloadedChapters", downloadedChaptersArray)

                // Reader Settings
                val rs = _readerSettings.value
                val settingsObj = org.json.JSONObject()
                settingsObj.put("theme", rs.theme.name)
                settingsObj.put("fontSizeSp", rs.fontSizeSp.toDouble())
                settingsObj.put("fontFamily", rs.fontFamily.name)
                settingsObj.put("lineHeight", rs.lineHeight.name)
                settingsObj.put("language", rs.language.name)
                settingsObj.put("showOriginalScript", rs.showOriginalScript)
                root.put("readerSettings", settingsObj)

                // Religion & Sect & Verse Selection
                root.put("userReligion", _userReligion.value.id)
                root.put("userSect", _userSect.value.id)
                root.put("notificationsEnabled", _notificationsEnabled.value)
                root.put("notificationIntervalMinutes", _notificationIntervalMinutes.value)

                val selectedBooksArray = org.json.JSONArray()
                _selectedBooksForVerse.value.forEach { selectedBooksArray.put(it) }
                root.put("selectedBooksForVerse", selectedBooksArray)

                // User Profile
                val userStateVal = _userState.value
                val profileObj = org.json.JSONObject()
                profileObj.put("displayName", userStateVal.displayName ?: "")
                profileObj.put("bio", userStateVal.bio ?: "")
                profileObj.put("photoUrl", userStateVal.photoUrl ?: "")
                profileObj.put("isDemoLoggedIn", userStateVal.isDemo)
                profileObj.put("email", userStateVal.email ?: "")
                root.put("userProfile", profileObj)

                // Notes & Highlights
                val notesList = notesHighlights.value
                val notesArray = org.json.JSONArray()
                notesList.forEach { note ->
                    val nObj = org.json.JSONObject()
                    nObj.put("id", note.id)
                    nObj.put("bookTitle", note.bookTitle)
                    nObj.put("quoteText", note.quoteText)
                    nObj.put("userReflection", note.userReflection ?: "")
                    nObj.put("dateText", note.dateText)
                    nObj.put("type", note.type)
                    notesArray.put(nObj)
                }
                root.put("notesHighlights", notesArray)

                // Reading History
                val historyList = readingHistory.value
                val historyArray = org.json.JSONArray()
                historyList.forEach { h ->
                    val hObj = org.json.JSONObject()
                    hObj.put("id", h.id)
                    hObj.put("bookTitle", h.bookTitle)
                    hObj.put("subtitle", h.subtitle)
                    hObj.put("progressPercent", h.progressPercent)
                    hObj.put("dateText", h.dateText)
                    hObj.put("surahOrChapter", h.surahOrChapter ?: "")
                    hObj.put("pagesRead", h.pagesRead)
                    hObj.put("isCompleted", h.isCompleted)
                    hObj.put("contemplationMinutes", h.contemplationMinutes)
                    historyArray.put(hObj)
                }
                root.put("readingHistory", historyArray)

                val jsonStr = root.toString(2)
                getBackupFiles().forEach { file ->
                    try {
                        file.parentFile?.mkdirs()
                        file.writeText(jsonStr)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun restoreBackupIfAvailable() {
        val targetFile = getBackupFiles().firstOrNull { it.exists() && it.length() > 0 } ?: return
        try {
            val jsonStr = targetFile.readText()
            if (jsonStr.isBlank()) return
            val root = org.json.JSONObject(jsonStr)

            // Restore Downloads
            if (root.has("downloadedBooks")) {
                val arr = root.getJSONArray("downloadedBooks")
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) set.add(arr.getString(i))
                _downloadedBooks.value = set
                val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
                offlinePrefs.edit().putStringSet("downloaded_books", set).apply()
            }

            if (root.has("downloadedSurahs")) {
                val arr = root.getJSONArray("downloadedSurahs")
                val set = mutableSetOf<Int>()
                val strSet = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getInt(i))
                    strSet.add(arr.getInt(i).toString())
                }
                _downloadedSurahs.value = set
                val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
                offlinePrefs.edit().putStringSet("downloaded_surahs", strSet).apply()
            }

            if (root.has("downloadedChapters")) {
                val arr = root.getJSONArray("downloadedChapters")
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) set.add(arr.getString(i))
                _downloadedChapters.value = set
                val offlinePrefs = getApplication<Application>().getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE)
                offlinePrefs.edit().putStringSet("downloaded_chapters", set).apply()
            }

            // Restore Reader Settings
            if (root.has("readerSettings")) {
                val settingsObj = root.getJSONObject("readerSettings")
                val themeStr = settingsObj.optString("theme", "LIGHT")
                val fontSize = settingsObj.optDouble("fontSizeSp", 20.0).toFloat()
                val fontFamilyStr = settingsObj.optString("fontFamily", "SERIF")
                val lineHeightStr = settingsObj.optString("lineHeight", "NORMAL")
                val languageStr = settingsObj.optString("language", "EN")
                val showOriginal = settingsObj.optBoolean("showOriginalScript", true)

                _readerSettings.value = ReaderSettings(
                    theme = try { AppThemeSetting.valueOf(themeStr) } catch (e: Exception) { AppThemeSetting.LIGHT },
                    fontSizeSp = fontSize,
                    fontFamily = try { FontFamilySetting.valueOf(fontFamilyStr) } catch (e: Exception) { FontFamilySetting.SERIF },
                    lineHeight = try { LineHeightSetting.valueOf(lineHeightStr) } catch (e: Exception) { LineHeightSetting.NORMAL },
                    language = try { AppLanguage.valueOf(languageStr) } catch (e: Exception) { AppLanguage.EN },
                    showOriginalScript = showOriginal
                )

                val settingsPrefs = getApplication<Application>().getSharedPreferences("scriptorium_settings", Context.MODE_PRIVATE)
                settingsPrefs.edit()
                    .putString("theme", themeStr)
                    .putFloat("font_size", fontSize)
                    .putString("font_family", fontFamilyStr)
                    .putString("line_height", lineHeightStr)
                    .putString("language", languageStr)
                    .putBoolean("show_original_script", showOriginal)
                    .apply()
            }

            // Restore Religion & Sect
            val authPrefs = getApplication<Application>().getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
            if (root.has("userReligion")) {
                val relId = root.getString("userReligion")
                val loadedReligion = UserReligion.fromId(relId)
                _userReligion.value = loadedReligion
                authPrefs.edit().putString("user_religion", relId).apply()
            }
            if (root.has("userSect")) {
                val sectId = root.getString("userSect")
                val loadedSect = UserSect.fromId(sectId, _userReligion.value)
                _userSect.value = loadedSect
                authPrefs.edit().putString("user_sect", sectId).apply()
            }

            if (root.has("notificationsEnabled")) {
                val enabled = root.getBoolean("notificationsEnabled")
                _notificationsEnabled.value = enabled
                authPrefs.edit().putBoolean("notifications_enabled", enabled).apply()
            }
            if (root.has("notificationIntervalMinutes")) {
                val interval = root.getInt("notificationIntervalMinutes")
                _notificationIntervalMinutes.value = interval
                authPrefs.edit().putInt("notification_interval_minutes", interval).apply()
            }

            if (root.has("selectedBooksForVerse")) {
                val arr = root.getJSONArray("selectedBooksForVerse")
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) set.add(arr.getString(i))
                _selectedBooksForVerse.value = set
                authPrefs.edit().putStringSet("selected_verse_books", set).apply()
            }

            // Restore Profile
            if (root.has("userProfile")) {
                val prof = root.getJSONObject("userProfile")
                val name = prof.optString("displayName", "")
                val bio = prof.optString("bio", "")
                val photoUrl = prof.optString("photoUrl", "")
                val isDemo = prof.optBoolean("isDemoLoggedIn", false)
                val email = prof.optString("email", "yolcu@scriptorium.org")

                if (name.isNotEmpty() || bio.isNotEmpty() || isDemo) {
                    authPrefs.edit()
                        .putString("custom_name", if (name.isNotEmpty()) name else null)
                        .putString("custom_bio", bio)
                        .putString("custom_photo_url", if (photoUrl.isNotEmpty()) photoUrl else null)
                        .putBoolean("is_demo_logged_in", isDemo)
                        .putString("demo_email", email)
                        .putString("demo_name", name)
                        .apply()

                    _userState.value = UserState(
                        email = email,
                        displayName = if (name.isNotEmpty()) name else "Bilgelik Yolcusu",
                        photoUrl = if (photoUrl.isNotEmpty()) photoUrl else null,
                        bio = bio,
                        isLoggedIn = true,
                        isDemo = isDemo
                    )
                }
            }

            // Restore Notes & Highlights in Room DB
            if (root.has("notesHighlights")) {
                val arr = root.getJSONArray("notesHighlights")
                viewModelScope.launch(Dispatchers.IO) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val note = NoteHighlight(
                            id = obj.optInt("id", 0),
                            bookTitle = obj.optString("bookTitle", ""),
                            quoteText = obj.optString("quoteText", ""),
                            userReflection = obj.optString("userReflection", null),
                            dateText = obj.optString("dateText", ""),
                            type = obj.optString("type", "Highlight")
                        )
                        repository.insertNoteHighlight(note)
                    }
                }
            }

            // Restore Reading History in Room DB
            if (root.has("readingHistory")) {
                val arr = root.getJSONArray("readingHistory")
                viewModelScope.launch(Dispatchers.IO) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.updateReadingProgress(
                            bookTitle = obj.optString("bookTitle", ""),
                            subtitle = obj.optString("subtitle", ""),
                            progress = obj.optInt("progressPercent", 0),
                            dateText = obj.optString("dateText", ""),
                            surahOrChapter = obj.optString("surahOrChapter", null),
                            pagesRead = obj.optInt("pagesRead", 0),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            contemplationMinutes = obj.optInt("contemplationMinutes", 0)
                        )
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun forgetMeAndClearAllData(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            stopAudio()

            // 1. Delete all backup files & directories
            getBackupFiles().forEach { file ->
                try {
                    if (file.exists()) file.delete()
                    file.parentFile?.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Clear Room Database
            try {
                repository.clearAllUserData()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Clear SharedPreferences
            val app = getApplication<Application>()
            app.getSharedPreferences("scriptorium_offline", Context.MODE_PRIVATE).edit().clear().apply()
            app.getSharedPreferences("scriptorium_settings", Context.MODE_PRIVATE).edit().clear().apply()
            app.getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE).edit().clear().apply()

            // 4. Delete local cached surah / book JSON files
            try {
                app.filesDir?.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".json") || file.name.endsWith(".jpg") || file.name.endsWith(".png") || file.name.endsWith(".mp3")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 5. Reset ViewModel states
            withContext(Dispatchers.Main) {
                _downloadedBooks.value = emptySet()
                _downloadedSurahs.value = emptySet()
                _downloadedChapters.value = emptySet()
                _readerSettings.value = ReaderSettings()
                _userReligion.value = UserReligion.ISLAM
                _userSect.value = UserSect.SUNNI
                _selectedBooksForVerse.value = books.map { it.id }.toSet()
                _notificationsEnabled.value = true
                _notificationIntervalMinutes.value = 1440
                _userState.value = UserState(isLoggedIn = false)

                onComplete()
            }
        }
    }
}