package com.example.license

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class KeypadLayoutType(val label: String) {
    GRID_4("Grille 4 Touches (Standard)"),
    GRID_2("Grille 2 Grandes Touches (Express)"),
    LIST_COMPACT("Liste Verticale Compacte")
}

data class AppLicenseConfig(
    val key: String = "",
    val merchantName: String = "",
    val shopName: String = "SIRA Business",
    val maxUsers: Int = 1,
    val usedCount: Int = 0,
    val themeColorHex: String = "#005AC1",
    val appName: String = "SIRA Business",
    val logoUrl: String = "",
    val bgUrl: String = "",
    val cguText: String = "Licence officielle commerciale concédée par l'Organisation SIRA. Souveraineté totale des données.",
    val privacyText: String = "Données marchandes isolées et hautement protégées. Aucune fuite cloud non autorisée.",
    val keypadLayout: KeypadLayoutType = KeypadLayoutType.GRID_4,
    val isActivated: Boolean = false,
    val isRevoked: Boolean = false,
    val statusMessage: String = ""
)

class LicenseManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sira_license_vault", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Unique anonymous device fingerprint
    val deviceId: String

    private val _licenseConfig = MutableStateFlow(loadSavedLicense())
    val licenseConfig: StateFlow<AppLicenseConfig> = _licenseConfig.asStateFlow()

    // Central cloud endpoints for verification
    val cloudBaseUrl = "https://ais-dev-vnyffmslextjnn7vtodb2q-848224578156.europe-west2.run.app"
    val localBaseUrl = "http://127.0.0.1:8080"

    init {
        var did = prefs.getString("sira_device_id", null)
        if (did == null) {
            did = "DEV-" + UUID.randomUUID().toString().take(12).uppercase()
            prefs.edit().putString("sira_device_id", did).apply()
        }
        deviceId = did

        // Recheck license validity on startup in background if activated
        if (_licenseConfig.value.isActivated && _licenseConfig.value.key.isNotBlank()) {
            scope.launch {
                validateKeyOnline(_licenseConfig.value.key)
            }
        }
    }

    private fun loadSavedLicense(): AppLicenseConfig {
        val key = prefs.getString("license_key", "") ?: ""
        val isAct = prefs.getBoolean("is_activated", false)
        val isRev = prefs.getBoolean("is_revoked", false)
        val merchant = prefs.getString("merchant_name", "Commerçant SIRA") ?: "Commerçant SIRA"
        val shop = prefs.getString("shop_name", "SIRA Business") ?: "SIRA Business"
        val maxUsers = prefs.getInt("max_users", 1)
        val usedCount = prefs.getInt("used_count", 1)
        val color = prefs.getString("theme_color", "#005AC1") ?: "#005AC1"
        val appName = prefs.getString("app_name", "SIRA Business") ?: "SIRA Business"
        val logoUrl = prefs.getString("logo_url", "") ?: ""
        val bgUrl = prefs.getString("bg_url", "") ?: ""
        val cgu = prefs.getString("cgu_text", "Licence officielle commerciale concédée par l'Organisation SIRA.") ?: ""
        val privacy = prefs.getString("privacy_text", "Données marchandes isolées et hautement protégées.") ?: ""
        val keypadStr = prefs.getString("keypad_layout", "GRID_4") ?: "GRID_4"
        val keypad = try { KeypadLayoutType.valueOf(keypadStr) } catch (e: Exception) { KeypadLayoutType.GRID_4 }

        return AppLicenseConfig(
            key = key,
            merchantName = merchant,
            shopName = shop,
            maxUsers = maxUsers,
            usedCount = usedCount,
            themeColorHex = color,
            appName = appName,
            logoUrl = logoUrl,
            bgUrl = bgUrl,
            cguText = cgu,
            privacyText = privacy,
            keypadLayout = keypad,
            isActivated = isAct,
            isRevoked = isRev,
            statusMessage = if (isRev) "LICENCE RÉVOQUÉE PAR L'ADMINISTRATEUR" else if (isAct) "Licence active" else "Non activée"
        )
    }

    suspend fun activateWithKey(inputKey: String): Result<AppLicenseConfig> = withContext(Dispatchers.IO) {
        val trimmed = inputKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(Exception("Veuillez saisir une clé API valide."))
        }

        val validation = callValidateEndpoint(trimmed)
        if (validation.optBoolean("valid", false)) {
            val licObj = validation.optJSONObject("license") ?: JSONObject()
            val themeColor = licObj.optString("themeColor", "#005AC1")
            val appName = licObj.optString("appName", "SIRA Business")
            val shopName = licObj.optString("shopName", "Commerce SIRA")
            val merchantName = licObj.optString("merchantName", "Commerçant")
            val maxUsers = licObj.optInt("maxUsers", 1)
            val usedCount = licObj.optInt("usedCount", 1)
            val logoUrl = licObj.optString("logoUrl", "")
            val bgUrl = licObj.optString("bgUrl", "")
            val cgu = licObj.optString("cguText", "Licence officielle commerciale concédée par l'Organisation SIRA.")
            val privacy = licObj.optString("privacyText", "Données marchandes isolées et hautement protégées.")
            val keypadStr = licObj.optString("keypadLayout", "GRID_4")
            val keypad = try { KeypadLayoutType.valueOf(keypadStr) } catch (e: Exception) { KeypadLayoutType.GRID_4 }

            saveToPrefs(
                key = trimmed,
                isActivated = true,
                isRevoked = false,
                merchant = merchantName,
                shop = shopName,
                maxUsers = maxUsers,
                usedCount = usedCount,
                color = themeColor,
                appName = appName,
                logoUrl = logoUrl,
                bgUrl = bgUrl,
                cgu = cgu,
                privacy = privacy,
                keypad = keypad
            )

            val updated = _licenseConfig.value
            Result.success(updated)
        } else {
            val error = validation.optString("error", "Clé de licence invalide ou quota dépassé.")
            if (validation.optBoolean("revoked", false)) {
                revokeLocally("Cette clé API a été révoquée.")
            }
            Result.failure(Exception(error))
        }
    }

    suspend fun validateKeyOnline(keyToTest: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val validation = callValidateEndpoint(keyToTest)
            if (!validation.optBoolean("valid", false)) {
                val isRevoked = validation.optBoolean("revoked", false)
                if (isRevoked) {
                    revokeLocally("Clé révoquée par l'administrateur.")
                }
                return@withContext false
            } else {
                // Update customizations in case admin changed them
                val licObj = validation.optJSONObject("license") ?: JSONObject()
                val themeColor = licObj.optString("themeColor", _licenseConfig.value.themeColorHex)
                val appName = licObj.optString("appName", _licenseConfig.value.appName)
                val shopName = licObj.optString("shopName", _licenseConfig.value.shopName)
                val logoUrl = licObj.optString("logoUrl", _licenseConfig.value.logoUrl)
                val bgUrl = licObj.optString("bgUrl", _licenseConfig.value.bgUrl)
                val cgu = licObj.optString("cguText", _licenseConfig.value.cguText)
                val privacy = licObj.optString("privacyText", _licenseConfig.value.privacyText)
                val keypadStr = licObj.optString("keypadLayout", _licenseConfig.value.keypadLayout.name)
                val keypad = try { KeypadLayoutType.valueOf(keypadStr) } catch (e: Exception) { _licenseConfig.value.keypadLayout }

                saveToPrefs(
                    key = keyToTest,
                    isActivated = true,
                    isRevoked = false,
                    merchant = licObj.optString("merchantName", _licenseConfig.value.merchantName),
                    shop = shopName,
                    maxUsers = licObj.optInt("maxUsers", _licenseConfig.value.maxUsers),
                    usedCount = licObj.optInt("usedCount", _licenseConfig.value.usedCount),
                    color = themeColor,
                    appName = appName,
                    logoUrl = logoUrl,
                    bgUrl = bgUrl,
                    cgu = cgu,
                    privacy = privacy,
                    keypad = keypad
                )
                return@withContext true
            }
        } catch (e: Exception) {
            // Keep current local state on network error
            return@withContext _licenseConfig.value.isActivated && !_licenseConfig.value.isRevoked
        }
    }

    suspend fun saveCustomization(
        appName: String,
        shopName: String,
        themeColorHex: String,
        logoUrl: String,
        bgUrl: String,
        cguText: String,
        privacyText: String,
        keypadLayout: KeypadLayoutType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val currentKey = _licenseConfig.value.key
        if (currentKey.isBlank()) {
            return@withContext Result.failure(Exception("Aucune licence active."))
        }

        saveToPrefs(
            key = currentKey,
            isActivated = true,
            isRevoked = false,
            merchant = _licenseConfig.value.merchantName,
            shop = shopName,
            maxUsers = _licenseConfig.value.maxUsers,
            usedCount = _licenseConfig.value.usedCount,
            color = themeColorHex,
            appName = appName,
            logoUrl = logoUrl,
            bgUrl = bgUrl,
            cgu = cguText,
            privacy = privacyText,
            keypad = keypadLayout
        )

        // Try syncing customization to server
        try {
            val payload = JSONObject().apply {
                put("key", currentKey)
                put("appName", appName)
                put("shopName", shopName)
                put("themeColor", themeColorHex)
                put("logoUrl", logoUrl)
                put("bgUrl", bgUrl)
                put("cguText", cguText)
                put("privacyText", privacyText)
                put("keypadLayout", keypadLayout.name)
                put("deviceId", deviceId)
            }
            sendPostRequest("$localBaseUrl/api/licenses/customize", payload)
        } catch (e: Exception) {
            try {
                val payload = JSONObject().apply {
                    put("key", currentKey)
                    put("appName", appName)
                    put("shopName", shopName)
                    put("themeColor", themeColorHex)
                    put("logoUrl", logoUrl)
                    put("bgUrl", bgUrl)
                    put("cguText", cguText)
                    put("privacyText", privacyText)
                    put("keypadLayout", keypadLayout.name)
                    put("deviceId", deviceId)
                }
                sendPostRequest("$cloudBaseUrl/api/licenses/customize", payload)
            } catch (ignored: Exception) {}
        }

        Result.success(Unit)
    }

    fun revokeLocally(reason: String = "Licence révoquée.") {
        prefs.edit()
            .putBoolean("is_revoked", true)
            .putBoolean("is_activated", false)
            .apply()

        _licenseConfig.value = _licenseConfig.value.copy(
            isActivated = false,
            isRevoked = true,
            statusMessage = reason
        )
    }

    fun resetLicense() {
        prefs.edit().clear().apply()
        _licenseConfig.value = AppLicenseConfig(
            isActivated = false,
            isRevoked = false,
            statusMessage = "Application en attente d'activation par clé API"
        )
    }

    private fun saveToPrefs(
        key: String,
        isActivated: Boolean,
        isRevoked: Boolean,
        merchant: String,
        shop: String,
        maxUsers: Int,
        usedCount: Int,
        color: String,
        appName: String,
        logoUrl: String,
        bgUrl: String,
        cgu: String,
        privacy: String,
        keypad: KeypadLayoutType
    ) {
        prefs.edit()
            .putString("license_key", key)
            .putBoolean("is_activated", isActivated)
            .putBoolean("is_revoked", isRevoked)
            .putString("merchant_name", merchant)
            .putString("shop_name", shop)
            .putInt("max_users", maxUsers)
            .putInt("used_count", usedCount)
            .putString("theme_color", color)
            .putString("app_name", appName)
            .putString("logo_url", logoUrl)
            .putString("bg_url", bgUrl)
            .putString("cgu_text", cgu)
            .putString("privacy_text", privacy)
            .putString("keypad_layout", keypad.name)
            .apply()

        _licenseConfig.value = AppLicenseConfig(
            key = key,
            merchantName = merchant,
            shopName = shop,
            maxUsers = maxUsers,
            usedCount = usedCount,
            themeColorHex = color,
            appName = appName,
            logoUrl = logoUrl,
            bgUrl = bgUrl,
            cguText = cgu,
            privacyText = privacy,
            keypadLayout = keypad,
            isActivated = isActivated,
            isRevoked = isRevoked,
            statusMessage = if (isRevoked) "RÉVOQUÉE" else "Activée avec succès"
        )
    }

    private fun callValidateEndpoint(key: String): JSONObject {
        val payload = JSONObject().apply {
            put("key", key)
            put("deviceId", deviceId)
        }

        // Try local webserver first (port 8080/8081)
        try {
            val res = sendPostRequest("$localBaseUrl/api/licenses/validate", payload)
            return JSONObject(res)
        } catch (e1: Exception) {
            // Fallback to cloud admin portal
            try {
                val res = sendPostRequest("$cloudBaseUrl/api/licenses/validate", payload)
                return JSONObject(res)
            } catch (e2: Exception) {
                // If network unavailable and user inputs the master VIP key, allow offline fallback
                if (key.equals("SIRA-VIP-BURKINA-2026", ignoreCase = true)) {
                    return JSONObject().apply {
                        put("valid", true)
                        put("license", JSONObject().apply {
                            put("merchantName", "Direction Générale & Pilote")
                            put("shopName", "SIRA Commercial VIP")
                            put("maxUsers", 50)
                            put("usedCount", 1)
                            put("themeColor", "#005AC1")
                            put("appName", "SIRA Business VIP")
                            put("logoUrl", "")
                            put("bgUrl", "")
                            put("cguText", "Licence officielle commerciale concédée par l'Organisation SIRA.")
                            put("privacyText", "Données marchandes isolées et sécurisées. Souveraineté totale.")
                            put("keypadLayout", "GRID_4")
                        })
                    }
                }
                return JSONObject().apply {
                    put("valid", false)
                    put("error", "Connexion au serveur de licence impossible. Vérifiez votre réseau.")
                }
            }
        }
    }

    private fun sendPostRequest(targetUrl: String, body: JSONObject): String {
        val url = URL(targetUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")

        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(body.toString())
            writer.flush()
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        conn.disconnect()
        return response
    }

    companion object {
        @Volatile
        private var INSTANCE: LicenseManager? = null

        fun getInstance(context: Context): LicenseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LicenseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
