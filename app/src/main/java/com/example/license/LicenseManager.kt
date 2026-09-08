package com.example.license

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class KeypadLayoutType(val label: String) {
    GRID_4("Grille 4 Touches (Standard)"), GRID_2("Grille 2 Grandes Touches (Express)"), LIST_COMPACT("Liste Verticale Compacte")
}

data class AppLicenseConfig(
    val key: String = "",
    val merchantName: String = "",
    val shopName: String = "SIRA Business",
    val maxUsers: Int = 1,
    val usedCount: Int = 0,
    val stockModel: String = "BOUTIQUE",
    val themeColorHex: String = "#005AC1",
    val appName: String = "SIRA Business",
    val profilePhotoUrl: String = "",
    val logoUrl: String = "",
    val bgUrl: String = "",
    val cguText: String = "Licence officielle commerciale concédée par l'Organisation SIRA. Souveraineté totale des données.",
    val privacyText: String = "Données marchandes hautement protégées.",
    val keypadLayout: KeypadLayoutType = KeypadLayoutType.GRID_4,
    val isActivated: Boolean = false,
    val isRevoked: Boolean = false,
    val statusMessage: String = ""
)

class LicenseManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sira_license_vault", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val deviceId: String
    private val _licenseConfig = MutableStateFlow(loadSavedLicense())
    val licenseConfig: StateFlow<AppLicenseConfig> = _licenseConfig.asStateFlow()

    // SIRA Manager central license plane.
    val cloudBaseUrl = "https://sira-manager-admin.vercel.app"
    val localBaseUrl = "http://127.0.0.1:8080"

    init {
        var did = prefs.getString("sira_device_id", null)
        if (did == null) {
            did = "DEV-" + UUID.randomUUID().toString().take(12).uppercase()
            prefs.edit().putString("sira_device_id", did).apply()
        }
        deviceId = did

        if (_licenseConfig.value.isActivated && _licenseConfig.value.key.isNotBlank()) {
            scope.launch {
                validateKeyOnline(_licenseConfig.value.key)
                while (isActive) {
                    delay(60_000)
                    if (_licenseConfig.value.key.isNotBlank()) validateKeyOnline(_licenseConfig.value.key)
                }
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
        val usedCount = prefs.getInt("used_count", 0)
        val stockModel = prefs.getString("stock_model", "BOUTIQUE") ?: "BOUTIQUE"
        val color = prefs.getString("theme_color", "#005AC1") ?: "#005AC1"
        val appName = prefs.getString("app_name", "SIRA Business") ?: "SIRA Business"
        val profilePhotoUrl = prefs.getString("profile_photo_url", "") ?: ""
        val logoUrl = prefs.getString("logo_url", "") ?: ""
        val bgUrl = prefs.getString("bg_url", "") ?: ""
        val cgu = prefs.getString("cgu_text", "Licence officielle SIRA.") ?: ""
        val privacy = prefs.getString("privacy_text", "Données protégées et isolées.") ?: ""
        val keypadStr = prefs.getString("keypad_layout", "GRID_4") ?: "GRID_4"
        val keypad = try { KeypadLayoutType.valueOf(keypadStr) } catch (_: Exception) { KeypadLayoutType.GRID_4 }
        return AppLicenseConfig(key, merchant, shop, maxUsers, usedCount, stockModel, color, appName, profilePhotoUrl, logoUrl, bgUrl, cgu, privacy, keypad, isAct, isRev,
            if (isRev) "LICENCE RÉVOQUÉE PAR L'ADMINISTRATEUR" else if (isAct) "Licence active" else "Non activée")
    }

    suspend fun activateWithKey(inputKey: String): Result<AppLicenseConfig> = withContext(Dispatchers.IO) {
        val trimmed = inputKey.trim()
        if (trimmed.isBlank()) return@withContext Result.failure(Exception("Veuillez saisir une clé de licence valide."))
        val validation = callValidateEndpoint(trimmed)
        if (validation.optBoolean("valid", false)) {
            val licObj = validation.optJSONObject("license") ?: JSONObject()
            val keypad = try { KeypadLayoutType.valueOf(licObj.optString("keypadLayout", "GRID_4")) } catch (_: Exception) { KeypadLayoutType.GRID_4 }
            saveToPrefs(
                trimmed, true, false,
                licObj.optString("merchantName", "Commerçant"),
                licObj.optString("shopName", "Commerce SIRA"),
                licObj.optInt("maxUsers", 1),
                licObj.optInt("usedCount", 0),
                licObj.optString("stockModel", "BOUTIQUE"),
                licObj.optString("themeColor", "#005AC1"),
                licObj.optString("appName", "SIRA Business"),
                licObj.optString("profilePhotoUrl", ""),
                licObj.optString("logoUrl", ""),
                licObj.optString("bgUrl", ""),
                licObj.optString("cguText", "Licence officielle SIRA."),
                licObj.optString("privacyText", "Données protégées et isolées."), keypad
            )
            Result.success(_licenseConfig.value)
        } else {
            val error = validation.optString("error", "Clé de licence invalide ou quota dépassé.")
            if (validation.optBoolean("revoked", false) || validation.optBoolean("expired", false)) revokeLocally(error)
            Result.failure(Exception(error))
        }
    }

    suspend fun validateKeyOnline(keyToTest: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val validation = callValidateEndpoint(keyToTest)
            if (!validation.optBoolean("valid", false)) {
                if (validation.optBoolean("revoked", false) || validation.optBoolean("expired", false)) {
                    revokeLocally(validation.optString("error", "Licence désactivée à distance."))
                }
                return@withContext false
            }

            val licObj = validation.optJSONObject("license") ?: JSONObject()
            val keypad = try { KeypadLayoutType.valueOf(licObj.optString("keypadLayout", _licenseConfig.value.keypadLayout.name)) } catch (_: Exception) { _licenseConfig.value.keypadLayout }
            saveToPrefs(
                keyToTest, true, false,
                licObj.optString("merchantName", _licenseConfig.value.merchantName),
                licObj.optString("shopName", _licenseConfig.value.shopName),
                licObj.optInt("maxUsers", _licenseConfig.value.maxUsers),
                licObj.optInt("usedCount", _licenseConfig.value.usedCount),
                licObj.optString("stockModel", _licenseConfig.value.stockModel),
                licObj.optString("themeColor", _licenseConfig.value.themeColorHex),
                licObj.optString("appName", _licenseConfig.value.appName),
                licObj.optString("profilePhotoUrl", _licenseConfig.value.profilePhotoUrl),
                licObj.optString("logoUrl", _licenseConfig.value.logoUrl),
                licObj.optString("bgUrl", _licenseConfig.value.bgUrl),
                licObj.optString("cguText", _licenseConfig.value.cguText),
                licObj.optString("privacyText", _licenseConfig.value.privacyText), keypad
            )

            val commands = validation.optJSONArray("commands")
            if (commands != null) {
                for (i in 0 until commands.length()) {
                    val command = commands.optJSONObject(i)?.optString("command") ?: continue
                    if (command == "LOCK_APP") {
                        revokeLocally("Application verrouillée à distance par l'administrateur.")
                        return@withContext false
                    }
                }
            }
            true
        } catch (_: Exception) {
            _licenseConfig.value.isActivated && !_licenseConfig.value.isRevoked
        }
    }

    suspend fun saveCustomization(appName: String, shopName: String, themeColorHex: String, logoUrl: String, bgUrl: String, cguText: String, privacyText: String, keypadLayout: KeypadLayoutType): Result<Unit> = withContext(Dispatchers.IO) {
        val currentKey = _licenseConfig.value.key
        if (currentKey.isBlank()) return@withContext Result.failure(Exception("Aucune licence active."))
        saveToPrefs(currentKey, true, false, _licenseConfig.value.merchantName, shopName, _licenseConfig.value.maxUsers, _licenseConfig.value.usedCount, _licenseConfig.value.stockModel, themeColorHex, appName, _licenseConfig.value.profilePhotoUrl, logoUrl, bgUrl, cguText, privacyText, keypadLayout)
        Result.success(Unit)
    }

    fun revokeLocally(reason: String = "Licence révoquée.") {
        prefs.edit().putBoolean("is_revoked", true).putBoolean("is_activated", false).apply()
        _licenseConfig.value = _licenseConfig.value.copy(isActivated = false, isRevoked = true, statusMessage = reason)
    }

    fun resetLicense() {
        prefs.edit().clear().apply()
        _licenseConfig.value = AppLicenseConfig(isActivated = false, isRevoked = false, statusMessage = "Application en attente d'activation par clé de licence")
    }

    private fun saveToPrefs(
        key: String, isActivated: Boolean, isRevoked: Boolean, merchant: String, shop: String, maxUsers: Int, usedCount: Int,
        stockModel: String, color: String, appName: String, profilePhotoUrl: String, logoUrl: String, bgUrl: String, cgu: String, privacy: String, keypad: KeypadLayoutType
    ) {
        prefs.edit()
            .putString("license_key", key)
            .putBoolean("is_activated", isActivated)
            .putBoolean("is_revoked", isRevoked)
            .putString("merchant_name", merchant)
            .putString("shop_name", shop)
            .putInt("max_users", maxUsers)
            .putInt("used_count", usedCount)
            .putString("stock_model", stockModel)
            .putString("theme_color", color)
            .putString("app_name", appName)
            .putString("profile_photo_url", profilePhotoUrl)
            .putString("logo_url", logoUrl)
            .putString("bg_url", bgUrl)
            .putString("cgu_text", cgu)
            .putString("privacy_text", privacy)
            .putString("keypad_layout", keypad.name)
            .apply()
        _licenseConfig.value = AppLicenseConfig(key, merchant, shop, maxUsers, usedCount, stockModel, color, appName, profilePhotoUrl, logoUrl, bgUrl, cgu, privacy, keypad, isActivated, isRevoked,
            if (isRevoked) "RÉVOQUÉE" else "Activée avec succès")
    }

    private fun callValidateEndpoint(key: String): JSONObject {
        val payload = JSONObject().apply {
            put("key", key)
            put("deviceId", deviceId)
            put("appVersion", "1.0")
        }
        return try {
            JSONObject(sendPostRequest("$cloudBaseUrl/api/licenses/validate", payload))
        } catch (_: Exception) {
            try { JSONObject(sendPostRequest("$localBaseUrl/api/licenses/validate", payload)) }
            catch (_: Exception) { JSONObject().apply { put("valid", false); put("error", "Connexion au serveur de licence impossible. Vérifiez votre réseau.") } }
        }
    }

    private fun sendPostRequest(targetUrl: String, body: JSONObject): String {
        val conn = (URL(targetUrl).openConnection() as HttpURLConnection)
        conn.requestMethod = "POST"; conn.connectTimeout = 7000; conn.readTimeout = 7000; conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8"); conn.setRequestProperty("Accept", "application/json")
        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body.toString()); it.flush() }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        conn.disconnect(); return response
    }

    companion object {
        @Volatile private var INSTANCE: LicenseManager? = null
        fun getInstance(context: Context): LicenseManager = INSTANCE ?: synchronized(this) { INSTANCE ?: LicenseManager(context.applicationContext).also { INSTANCE = it } }
    }
}
