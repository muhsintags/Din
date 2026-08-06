package com.example.data.api

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.example.ui.util.AppLanguage
import com.example.ui.util.PrayerTimeInfo
import com.example.ui.util.UserReligion
import com.example.ui.util.UserSect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class UserLocationInfo(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val source: String // "GPS" or "IP API"
)

object PrayerTimeApiService {

    suspend fun detectLocation(context: Context): UserLocationInfo = withContext(Dispatchers.IO) {
        // 1. Try Android LocationManager if permission granted
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            var bestLocation: Location? = null
            if (isNetworkEnabled) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (bestLocation == null && isGpsEnabled) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }

            if (bestLocation != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "Bilinmeyen Şehir"
                    val country = addresses[0].countryName ?: "Türkiye"
                    return@withContext UserLocationInfo(
                        cityName = city,
                        countryName = country,
                        latitude = bestLocation.latitude,
                        longitude = bestLocation.longitude,
                        source = "GPS / Ağ"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback to free IP-based Geolocation API (ip-api.com)
        try {
            val url = URL("http://ip-api.com/json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            if (json.optString("status") == "success") {
                val city = json.optString("city", "İstanbul")
                val country = json.optString("country", "Türkiye")
                val lat = json.optDouble("lat", 41.0082)
                val lon = json.optDouble("lon", 28.9784)
                return@withContext UserLocationInfo(
                    cityName = city,
                    countryName = country,
                    latitude = lat,
                    longitude = lon,
                    source = "IP-Konum"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Default Fallback: Istanbul, Turkey
        return@withContext UserLocationInfo(
            cityName = "İstanbul",
            countryName = "Türkiye",
            latitude = 41.0082,
            longitude = 28.9784,
            source = "Varsayılan"
        )
    }

    suspend fun fetchPrayerTimesFromApi(
        latitude: Double,
        longitude: Double,
        religion: UserReligion,
        sect: UserSect
    ): List<PrayerTimeInfo> = withContext(Dispatchers.IO) {
        if (religion == UserReligion.ISLAM) {
            try {
                // Method 0 = Shia Ithna-Ashari / Jafari (Caferilik)
                // Method 13 = Diyanet İşleri Başkanlığı, Turkey
                val methodId = if (sect == UserSect.SHIA) 0 else 13
                val timestamp = System.currentTimeMillis() / 1000
                val apiUrl = "https://api.aladhan.com/v1/timings/$timestamp?latitude=$latitude&longitude=$longitude&method=$methodId"

                val url = URL(apiUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)

                if (json.optInt("code") == 200) {
                    val timings = json.getJSONObject("data").getJSONObject("timings")
                    val fajr = timings.optString("Fajr", "05:15")
                    val dhuhr = timings.optString("Dhuhr", "12:45")
                    val asr = timings.optString("Asr", "16:30")
                    val maghrib = timings.optString("Maghrib", "19:25")
                    val isha = timings.optString("Isha", "20:55")

                    if (sect == UserSect.SHIA) {
                        return@withContext listOf(
                            PrayerTimeInfo("Sabah (Fecr) Namazı", "Fajr Prayer", fajr, "Sabah Kur'an okuyuşu şahitlidir. (İsrâ 78)", "Indeed, the recitation of dawn is ever witnessed. (17:78)"),
                            PrayerTimeInfo("Öğle (Zuhr) Namazı", "Dhuhr Prayer", dhuhr, "Kıl namazı güneşin batıya kaymasından gecenin kararmasına kadar. (İsrâ 78)", "Perform prayer from the decline of the sun until the darkness of the night. (17:78)"),
                            PrayerTimeInfo("İkindi (Asr) Namazı", "Asr Prayer", "$asr (Ort. Öğle ile birleştirilebilir)", "Namazı dosdoğru kılın, zekâtı verin. (Bakara 43)", "Establish prayer and give zakah. (2:43)"),
                            PrayerTimeInfo("Akşam (Maghrib) Namazı", "Maghrib Prayer", maghrib, "Gündüzün iki tarafında ve gecenin gündüze yakın saatlerinde namaz kıl. (Hûd 114)", "Establish prayer at the two ends of the day. (11:114)"),
                            PrayerTimeInfo("Yatsı (Isha) Namazı", "Isha Prayer", "$isha (Ort. Akşam ile birleştirilebilir)", "Gecenin bir kısmında secde et ve O'nu tesbih et. (İnsân 26)", "And during the night prostrate to Him. (76:26)")
                        )
                    } else {
                        return@withContext listOf(
                            PrayerTimeInfo("Sabah (Fecr) Namazı", "Fajr Prayer", fajr, "Sabah Kur'an okuyuşu şahitlidir. (İsrâ 78)", "Indeed, the recitation of dawn is ever witnessed. (17:78)"),
                            PrayerTimeInfo("Öğle (Zuhr) Namazı", "Dhuhr Prayer", dhuhr, "Şüphesiz namaz, müminler üzerine vakitleri belirlenmiş bir farzdır. (Nisâ 103)", "Indeed, prayer has been decreed upon the believers. (4:103)"),
                            PrayerTimeInfo("İkindi (Asr) Namazı", "Asr Prayer", asr, "Namazlara ve orta namaza devam edin. (Bakara 238)", "Maintain with care the obligatory prayers and the middle prayer. (2:238)"),
                            PrayerTimeInfo("Akşam (Maghrib) Namazı", "Maghrib Prayer", maghrib, "Rabbini hamd ile tesbih et; güneşin doğuşundan ve batışından önce. (Tâhâ 130)", "Exalt with praise of your Lord before rising and setting. (20:130)"),
                            PrayerTimeInfo("Yatsı (Isha) Namazı", "Isha Prayer", isha, "Gecenin saatlerinde ve gündüzün uçlarında tesbih et. (Tâhâ 130)", "Exalt Him in hours of the night. (20:130)")
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback or non-Islamic religions: return local calculated times
        return@withContext com.example.ui.util.FaithPrayerSchedule.getPrayerSchedules(religion, sect)
    }
}
