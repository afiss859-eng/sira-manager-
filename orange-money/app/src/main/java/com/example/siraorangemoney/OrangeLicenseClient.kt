package com.example.siraorangemoney

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class OrangeLicenseClient(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sira_orange_license", Context.MODE_PRIVATE)
    val deviceId: String = prefs.getString("device_id", null) ?: ("OM-DEV-" + UUID.randomUUID().toString().take(12).uppercase()).also { prefs.edit().putString("device_id", it).apply() }
    private val base = "https://sira.dev"

    suspend fun activate(key: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("key", key.trim()); put("deviceId", deviceId) }
            val conn = URL("$base/api/manager/orange-money?action=validate").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"; conn.connectTimeout = 5000; conn.readTimeout = 8000; conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
            if (code !in 200..299) return@withContext Result.failure(Exception(JSONObject(text).optString("error", "Licence invalide.")))
            val json = JSONObject(text)
            if (!json.optBoolean("valid")) return@withContext Result.failure(Exception(json.optString("error", "Licence invalide.")))
            prefs.edit().putString("license_key", key.trim()).apply()
            Result.success(json.getJSONObject("license").optString("appName", "SIRA Orange Money"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
