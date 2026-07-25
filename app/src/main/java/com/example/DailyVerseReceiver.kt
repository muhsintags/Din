package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.model.BookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class DailyVerseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            scheduleAlarm(context)
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
                val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
                if (!notificationsEnabled) {
                    pendingResult.finish()
                    return@launch
                }

                // Get allowed books
                val savedBooks = prefs.getStringSet("selected_verse_books", null)
                val books = BookRepository.books
                val filteredBooks = if (savedBooks != null) {
                    books.filter { savedBooks.contains(it.id) }
                } else {
                    books
                }
                val targetBooks = if (filteredBooks.isEmpty()) books else filteredBooks

                // Choose a random book from the selected ones
                val randomBook = targetBooks.randomOrNull() ?: books.first()

                val settingsPrefs = context.getSharedPreferences("scriptorium_settings", Context.MODE_PRIVATE)
                val isEn = settingsPrefs.getString("language", "TR") == "EN"

                // Fetch random verse from live API, fallback to offline if error
                val (ref, text) = fetchVerseFromApiWithFallback(randomBook.id, randomBook, isEn)

                val savedReligionStr = prefs.getString("user_religion", "islam")
                val userReligion = com.example.ui.util.UserReligion.fromId(savedReligionStr)
                val savedSectStr = prefs.getString("user_sect", "sunni")
                val userSect = com.example.ui.util.UserSect.fromId(savedSectStr, userReligion)

                // Detect location and fetch live API prayer times
                val locationInfo = com.example.data.api.PrayerTimeApiService.detectLocation(context)
                val livePrayerSchedules = com.example.data.api.PrayerTimeApiService.fetchPrayerTimesFromApi(
                    latitude = locationInfo.latitude,
                    longitude = locationInfo.longitude,
                    religion = userReligion,
                    sect = userSect
                )

                val randomPrayer = livePrayerSchedules.randomOrNull()
                val lang = if (isEn) com.example.ui.util.AppLanguage.EN else com.example.ui.util.AppLanguage.TR

                val finalTitle = if (randomPrayer != null) {
                    "📍 ${locationInfo.cityName} [${userSect.getTitle(lang)}] ${randomPrayer.getName(lang)} (${randomPrayer.timeStr})"
                } else ref

                val finalText = if (randomPrayer != null && ref != null && text != null) {
                    "${randomPrayer.getMessage(lang)}\n\n($ref): $text"
                } else text ?: ""

                if (finalTitle != null && finalText.isNotEmpty()) {
                    showNotification(context, finalTitle, finalText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                scheduleAlarm(context)
                pendingResult.finish()
            }
        }
    }

    private suspend fun fetchVerseFromApiWithFallback(bookId: String, fallbackBook: com.example.data.model.Book, isEn: Boolean): Pair<String, String> {
        val client = OkHttpClient()
        return try {
            when (bookId) {
                "quran" -> {
                    val randomAyah = (1..6236).random()
                    val quranEdition = if (isEn) "en.yusufali" else "tr.diyanet"
                    val url = "https://api.alquran.cloud/v1/ayah/$randomAyah/$quranEdition"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Quran API failure")
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val dataObj = json.getJSONObject("data")
                        val text = dataObj.getString("text").trim()
                        val surahObj = dataObj.getJSONObject("surah")
                        val surahNum = surahObj.getInt("number")
                        val surahName = surahObj.getString("englishName")
                        val numberInSurah = dataObj.getInt("numberInSurah")
                        if (isEn) {
                            Pair("HOLY QURAN (Surah $surahName, Verse $numberInSurah)", text)
                        } else {
                            val surahTurkishName = when (surahNum) {
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
                                else -> surahName
                            }
                            Pair("KUR'AN-I KERİM ($surahTurkishName Suresi, Ayet $numberInSurah)", text)
                        }
                    }
                }
                "torah" -> {
                    // Random Torah book and chapter details
                    val torahBooks = if (isEn) {
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
                    val url = "https://bible-api.com/${selectedTorah.first}+$chapter"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Torah Bible API failure")
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val versesJA = json.getJSONArray("verses")
                        if (versesJA.length() == 0) throw Exception("No verses returned")
                        val randomIdx = (0 until versesJA.length()).random()
                        val verseObj = versesJA.getJSONObject(randomIdx)
                        val englishText = verseObj.getString("text").trim()
                        val verseNum = verseObj.getInt("verse")
                        val verseText = if (isEn) englishText else translateTextGtx(englishText)
                        val refHeader = if (isEn) "TORAH (${selectedTorah.second}, Chapter $chapter:$verseNum)" else "TEVRAT (${selectedTorah.second}, Bölüm $chapter:$verseNum)"
                        Pair(refHeader, verseText)
                    }
                }
                "sermon" -> {
                    // Random sermon chapter from Matthew (5, 6, 7)
                    val chapter = (5..7).random()
                    val url = "https://bible-api.com/matthew+$chapter"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Sermon Bible API failure")
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val versesJA = json.getJSONArray("verses")
                        if (versesJA.length() == 0) throw Exception("No verses returned")
                        val randomIdx = (0 until versesJA.length()).random()
                        val verseObj = versesJA.getJSONObject(randomIdx)
                        val englishText = verseObj.getString("text").trim()
                        val verseNum = verseObj.getInt("verse")
                        val verseText = if (isEn) englishText else translateTextGtx(englishText)
                        val refHeader = if (isEn) "GOSPEL (Matthew $chapter:$verseNum)" else "İNCİL (Matta $chapter:$verseNum)"
                        Pair(refHeader, verseText)
                    }
                }
                else -> fetchOfflineVerse(fallbackBook, isEn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fetchOfflineVerse(fallbackBook, isEn)
        }
    }

    private fun fetchOfflineVerse(book: com.example.data.model.Book, isEn: Boolean): Pair<String, String> {
        val paragraphs = book.paragraphs
        if (paragraphs.isEmpty()) {
            return Pair(
                if (isEn) "HOLY SCRIPTURES" else "KUTSAL KİTAP",
                if (isEn) "Verse content not found." else "Ayet içeriği bulunamadı."
            )
        }
        val randomIndex = (paragraphs.indices).random()
        val text = paragraphs[randomIndex]
        val ref = if (isEn) {
            when (book.id) {
                "quran" -> "HOLY QURAN (Surah Al-Fath, Verse ${randomIndex + 1})"
                "torah" -> "TORAH (Genesis, Chapter 1:${randomIndex + 1})"
                "sermon" -> "GOSPEL (Matthew 5:${randomIndex + 1})"
                else -> "${book.title.uppercase()} (${randomIndex + 1})"
            }
        } else {
            when (book.id) {
                "quran" -> "KUR'AN-I KERİM (Fetih Suresi, Ayet ${randomIndex + 1})"
                "torah" -> "TEVRAT (Yaratılış, Bölüm 1:${randomIndex + 1})"
                "sermon" -> "İNCİL (Matta 5:${randomIndex + 1})"
                else -> "${book.title.uppercase()} (${randomIndex + 1})"
            }
        }
        return Pair(ref, text)
    }

    private fun translateTextGtx(text: String): String {
        val okHttpClient = OkHttpClient()
        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=tr&dt=t&q=$encodedText"
            val request = Request.Builder().url(url).build()
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
                        return sb.toString().trim()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return text
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "hourly_verse_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Günün Ayetleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Seçilen kutsal kitaplardan saatlik ayet bildirimleri."
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4242, notification)
    }

    companion object {
        fun scheduleAlarm(context: Context, force: Boolean = false) {
            val prefs = context.getSharedPreferences("scriptorium_auth", Context.MODE_PRIVATE)
            val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyVerseReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!notificationsEnabled) {
                alarmManager.cancel(pendingIntent)
                return
            }

            // Get interval in minutes, default is 24 hours (1440 minutes)
            val intervalMinutes = prefs.getInt("notification_interval_minutes", 1440)
            val intervalMillis = intervalMinutes * 60 * 1000L

            val lastTriggerTime = prefs.getLong("last_trigger_time", 0L)
            val currentTime = System.currentTimeMillis()

            // Calculate when the next alarm should trigger
            var triggerAtMillis = lastTriggerTime + intervalMillis
            if (force) {
                triggerAtMillis = currentTime + 3000L // Trigger in 3 seconds to show immediate confirmation
                prefs.edit().putLong("last_trigger_time", currentTime).apply()
            } else if (triggerAtMillis <= currentTime) {
                triggerAtMillis = currentTime + intervalMillis
                prefs.edit().putLong("last_trigger_time", currentTime).apply()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }
}
