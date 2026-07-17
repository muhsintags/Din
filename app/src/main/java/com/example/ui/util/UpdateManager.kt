package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Data class representing the update information fetched from GitHub.
 * You can modify this class or the JSON structure on GitHub as your needs change.
 */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val updateUrl: String,
    val changelogTr: String,
    val changelogEn: String,
    val isForceUpdate: Boolean
)

/**
 * UpdateManager handles updating checking using a public GitHub repository.
 * Since deploying to Google Play Store is not possible, this provides a professional,
 * beautiful, and seamless alternative update mechanism.
 *
 * To customize this for your own repository:
 * Just change [GITHUB_USER] and [GITHUB_REPO] below, or set a direct custom [UPDATE_JSON_URL].
 */
object UpdateManager {
    private const val TAG = "UpdateManager"
    
    // ==========================================
    // CONFIGURATION AREA - CODES TO MODIFY LATER
    // ==========================================
    const val GITHUB_USER = "muhsintags"
    const val GITHUB_REPO = "Scriptorium"
    const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/main/update.json"
    // ==========================================

    private val client = OkHttpClient()

    /**
     * Checks for updates asynchronously by fetching the JSON metadata from GitHub.
     * Returns an [UpdateInfo] object if an update is available, or null otherwise.
     */
    suspend fun checkForUpdate(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(UPDATE_JSON_URL)
                    .header("Cache-Control", "no-cache") // Ensure fresh data from GitHub, avoiding cache
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to fetch update JSON from GitHub (HTTP ${response.code})")
                        return@withContext null
                    }

                    val jsonStr = response.body?.string() ?: return@withContext null
                    val json = JSONObject(jsonStr)
                    
                    val remoteVersionCode = json.getInt("versionCode")
                    val remoteVersionName = json.getString("versionName")
                    val updateUrl = json.getString("updateUrl")
                    val changelogTr = json.optString("changelogTr", "")
                    val changelogEn = json.optString("changelogEn", "")
                    val isForceUpdate = json.optBoolean("isForceUpdate", false)

                    // Read local application version code
                    val currentVersionCode = BuildConfig.VERSION_CODE
                    Log.d(TAG, "Update Check: Current version code = $currentVersionCode, Remote version code = $remoteVersionCode")

                    if (remoteVersionCode > currentVersionCode) {
                        UpdateInfo(
                            versionCode = remoteVersionCode,
                            versionName = remoteVersionName,
                            updateUrl = updateUrl,
                            changelogTr = changelogTr,
                            changelogEn = changelogEn,
                            isForceUpdate = isForceUpdate
                        )
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network or parsing error while checking for updates", e)
                null
            }
        }
    }

    /**
     * Utility function to launch a web browser pointed to the update download link or GitHub release page.
     */
    fun launchUpdate(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch browser with update URL: $url", e)
        }
    }
}
