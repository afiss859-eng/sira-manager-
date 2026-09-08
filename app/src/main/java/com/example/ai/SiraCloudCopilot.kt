package com.example.ai

import com.example.license.LicenseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** Cloud SIRA Copilot. The provider secret is never stored in the APK. */
class SiraCloudCopilot(private val licenseManager: LicenseManager) {
    suspend fun ask(query: String, context: Map<String, Any?> = emptyMap()): Result<String> = withContext(Dispatchers.IO) {
        val license = licenseManager.licenseConfig.value
        if (!license.isActivated || license.isRevoked || license.key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Licence SIRA inactive."))
        }

        val payload = JSONObject().apply {
            put("key", license.key)
            put("deviceId", licenseManager.deviceId)
            put("query", query.trim())
            put("context", JSONObject(context))
        }

        try {
            val response = post("${licenseManager.cloudBaseUrl}/api/manager/copilot", payload)
            val json = JSONObject(response)
            if (!json.optBoolean("ok", false)) {
                return@withContext Result.failure(Exception(json.optString("error", "Copilote indisponible.")))
            }
            Result.success(json.optString("answer", "Le copilote n’a pas retourné de réponse."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun post(targetUrl: String, body: JSONObject): String {
        val connection = URL(targetUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 8000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(body.toString()) }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
            if (status !in 200..299) throw Exception("Copilote HTTP $status")
            return response
        } finally {
            connection.disconnect()
        }
    }
}
