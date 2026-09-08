package com.sira.orangemoney

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class OrangeLicenseClient(context: Context) {
    companion object {
        private const val PREFS = "sira_orange_license"
        private const val KEY = "license_key"
        private const val DEVICE_ID = "device_id"
        private const val APP_NAME = "app_name"
        private const val LAST_VALIDATED_AT = "last_validated_at"
        private const val VALIDATION_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
        private val LICENSE_PATTERN = Regex("SIRA-OM-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}")
        private const val BASE_URL = "https://sira-website-doma1.vercel.app"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val deviceId: String = prefs.getString(DEVICE_ID, null)
        ?: ("OM-DEV-" + UUID.randomUUID().toString().replace("-", "").take(12).uppercase())
            .also { prefs.edit().putString(DEVICE_ID, it).apply() }

    val cachedLicenseKey: String?
        get() = prefs.getString(KEY, null)

    val cachedAppName: String
        get() = prefs.getString(APP_NAME, "SIRA Orange Money") ?: "SIRA Orange Money"

    val isOfflineCacheUsable: Boolean
        get() {
            val key = cachedLicenseKey ?: return false
            val last = prefs.getLong(LAST_VALIDATED_AT, 0L)
            return LICENSE_PATTERN.matches(key) && last > 0L && System.currentTimeMillis() - last <= VALIDATION_WINDOW_MS
        }

    suspend fun activate(key: String): Result<String> = withContext(Dispatchers.IO) {
        val normalized = key.trim().uppercase()
        if (!LICENSE_PATTERN.matches(normalized)) {
            return@withContext Result.failure(Exception("Format de licence invalide. Attendu : SIRA-OM-XXXX-XXXX-XXXX"))
        }

        var conn: HttpURLConnection? = null
        try {
            val body = JSONObject().apply {
                put("key", normalized)
                put("deviceId", deviceId)
                put("applicationId", "com.sira.orangemoney")
            }

            conn = (URL("$BASE_URL/api/manager/orange-money?action=validate").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()

            if (code !in 200..299) {
                return@withContext Result.failure(Exception(json?.optString("error") ?: "Licence invalide."))
            }

            if (json?.optBoolean("valid") != true) {
                return@withContext Result.failure(Exception(json?.optString("error") ?: "Licence invalide."))
            }

            val appName = json.optJSONObject("license")?.optString("appName", "SIRA Orange Money")
                ?: "SIRA Orange Money"

            prefs.edit()
                .putString(KEY, normalized)
                .putString(APP_NAME, appName)
                .putLong(LAST_VALIDATED_AT, System.currentTimeMillis())
                .apply()

            Result.success(appName)
        } catch (e: Exception) {
            if (isOfflineCacheUsable && cachedLicenseKey == normalized) {
                Result.success(cachedAppName)
            } else {
                Result.failure(e)
            }
        } finally {
            conn?.disconnect()
        }
    }

    fun clearLocalLicense() {
        prefs.edit()
            .remove(KEY)
            .remove(APP_NAME)
            .remove(LAST_VALIDATED_AT)
            .apply()
    }
}
