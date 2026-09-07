package com.example.admin

import android.content.Context
import android.content.SharedPreferences
import com.example.ai.GeminiClient
import com.example.auth.SiraRole
import com.example.data.database.SiraDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class MerchantAccountInfo(
    val id: String,
    val email: String,
    val displayName: String,
    val shopName: String,
    val city: String,
    val phone: String,
    val role: SiraRole,
    val dbFileName: String,
    val dbSizeBytes: Long,
    val productCount: Int,
    val saleCount: Int,
    val totalRevenueFcfa: Long,
    val isActive: Boolean,
    val isApproved: Boolean,
    val ifuNumber: String,
    val lastActiveTimestamp: Long
)

data class AdminDirective(
    val id: String = UUID.randomUUID().toString().take(8),
    val title: String,
    val message: String,
    val author: String,
    val priority: String = "INFO", // "URGENT", "INFO", "REGLEMENTAIRE"
    val timestamp: Long = System.currentTimeMillis()
)

data class AdminLicenseKey(
    val key: String,
    val merchantName: String,
    val shopName: String,
    val maxUsers: Int = 1,
    val usedCount: Int = 0,
    val devices: List<String> = emptyList(),
    val themeColor: String = "#005AC1",
    val appName: String = "SIRA Business",
    val logoUrl: String = "",
    val bgUrl: String = "",
    val cguText: String = "Licence officielle commerciale concédée par l'Organisation SIRA.",
    val privacyText: String = "Données marchandes isolées et sécurisées. Souveraineté totale.",
    val keypadLayout: String = "GRID_4",
    val status: String = "ACTIVE", // ACTIVE or REVOKED
    val createdAt: Long = System.currentTimeMillis()
)

data class AdminApiKeys(
    val geminiApiKey: String = "",
    val cinetPayApiKey: String = "",
    val cinetPaySiteId: String = "",
    val payDunyaMasterKey: String = "",
    val payDunyaToken: String = "",
    val orangeMoneyMerchantCode: String = "",
    val orangeMoneyApiKey: String = ""
)

data class SystemServerMetrics(
    val serverStatus: String,
    val serverUrl: String,
    val cloudPortalUrl: String,
    val port: Int,
    val jvmFreeMemoryMb: Long,
    val jvmTotalMemoryMb: Long,
    val activeDatabasesCount: Int,
    val totalMerchantsCount: Int,
    val uptimeSeconds: Long
)

data class AdminAuditEntry(
    val id: String = UUID.randomUUID().toString().take(8),
    val action: String,
    val performedBy: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AdminManager private constructor(private val context: Context) {

    val cloudDevUrl = "https://ais-pre-vnyffmslextjnn7vtodb2q-848224578156.europe-west2.run.app"
    val cloudAdminUrl = "https://ais-pre-vnyffmslextjnn7vtodb2q-848224578156.europe-west2.run.app"
    val cloudSharedUrl = "https://ais-pre-vnyffmslextjnn7vtodb2q-848224578156.europe-west2.run.app"
    val localAssetUrl = "file:///android_asset/admin_panel/index.html"

    private val prefs: SharedPreferences = context.getSharedPreferences("sira_admin_vault", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val startTime = System.currentTimeMillis()

    private val _apiKeys = MutableStateFlow(loadApiKeys())
    val apiKeys: StateFlow<AdminApiKeys> = _apiKeys.asStateFlow()

    private val _serverMetrics = MutableStateFlow(calculateMetrics(8080, false))
    val serverMetrics: StateFlow<SystemServerMetrics> = _serverMetrics.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AdminAuditEntry>>(emptyList())
    val auditLogs: StateFlow<List<AdminAuditEntry>> = _auditLogs.asStateFlow()

    private val _directives = MutableStateFlow<List<AdminDirective>>(loadDirectives())
    val directives: StateFlow<List<AdminDirective>> = _directives.asStateFlow()

    private val _licenseKeys = MutableStateFlow<List<AdminLicenseKey>>(loadLicenseKeys())
    val licenseKeys: StateFlow<List<AdminLicenseKey>> = _licenseKeys.asStateFlow()

    private val suspendedUsers = ConcurrentHashMap.newKeySet<String>()
    private val approvedUsers = ConcurrentHashMap.newKeySet<String>()
    private val userRoles = ConcurrentHashMap<String, SiraRole>()

    init {
        // Clean slate reset for zero simulation policy
        if (!prefs.getBoolean("admin_clean_slate_v3", false)) {
            prefs.edit()
                .remove("admin_directives")
                .putStringSet("approved_users", setOf("sawadogoafis125_gmail_com"))
                .putStringSet("suspended_users", emptySet())
                .putBoolean("admin_clean_slate_v3", true)
                .apply()
        }

        // Load initially suspended and approved users
        val suspendedSet = prefs.getStringSet("suspended_users", emptySet()) ?: emptySet()
        suspendedUsers.addAll(suspendedSet)

        val approvedSet = prefs.getStringSet("approved_users", setOf("sawadogoafis125_gmail_com")) ?: emptySet()
        approvedUsers.addAll(approvedSet)

        recordAudit("SYSTEM_BOOT", "admin@sira.bf", "Direction Centrale SIRA initialisée (Environnement de production réel, zéro simulation)")
    }

    private fun loadApiKeys(): AdminApiKeys {
        return AdminApiKeys(
            geminiApiKey = prefs.getString("gemini_key", GeminiClient.getApiKey()) ?: "",
            cinetPayApiKey = prefs.getString("cinetpay_key", "") ?: "",
            cinetPaySiteId = prefs.getString("cinetpay_site", "") ?: "",
            payDunyaMasterKey = prefs.getString("paydunya_key", "") ?: "",
            payDunyaToken = prefs.getString("paydunya_token", "") ?: "",
            orangeMoneyMerchantCode = prefs.getString("orange_code", "") ?: "",
            orangeMoneyApiKey = prefs.getString("orange_key", "") ?: ""
        )
    }

    private fun loadDirectives(): List<AdminDirective> {
        val raw = prefs.getString("admin_directives", null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<AdminDirective>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AdminDirective(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            message = obj.optString("message"),
                            author = obj.optString("author"),
                            priority = obj.optString("priority", "INFO"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                return list
            } catch (ignored: Exception) {}
        }
        return emptyList()
    }

    private fun loadLicenseKeys(): List<AdminLicenseKey> {
        val raw = prefs.getString("sira_license_keys_list", null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<AdminLicenseKey>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val devArray = obj.optJSONArray("devices") ?: JSONArray()
                    val devList = mutableListOf<String>()
                    for (d in 0 until devArray.length()) devList.add(devArray.getString(d))

                    list.add(
                        AdminLicenseKey(
                            key = obj.optString("key"),
                            merchantName = obj.optString("merchantName"),
                            shopName = obj.optString("shopName"),
                            maxUsers = obj.optInt("maxUsers", 1),
                            usedCount = obj.optInt("usedCount", devList.size),
                            devices = devList,
                            themeColor = obj.optString("themeColor", "#005AC1"),
                            appName = obj.optString("appName", "SIRA Business"),
                            logoUrl = obj.optString("logoUrl", ""),
                            bgUrl = obj.optString("bgUrl", ""),
                            cguText = obj.optString("cguText", "Licence officielle commerciale concédée par l'Organisation SIRA."),
                            privacyText = obj.optString("privacyText", "Données marchandes isolées et sécurisées. Souveraineté totale."),
                            keypadLayout = obj.optString("keypadLayout", "GRID_4"),
                            status = obj.optString("status", "ACTIVE"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                return list
            } catch (ignored: Exception) {}
        }

        // Default initial VIP key
        val defaultVip = AdminLicenseKey(
            key = "SIRA-VIP-BURKINA-2026",
            merchantName = "Direction Générale & Pilote",
            shopName = "SIRA Commercial VIP",
            maxUsers = 50,
            usedCount = 1,
            devices = listOf("sawadogoafis125_gmail_com"),
            themeColor = "#005AC1",
            appName = "SIRA Business VIP",
            logoUrl = "",
            bgUrl = "",
            cguText = "Licence officielle commerciale concédée par l'Organisation SIRA.",
            privacyText = "Données marchandes isolées et sécurisées. Souveraineté totale.",
            keypadLayout = "GRID_4",
            status = "ACTIVE"
        )
        return listOf(defaultVip)
    }

    private fun persistLicenseKeys(keys: List<AdminLicenseKey>) {
        val array = JSONArray()
        keys.forEach { k ->
            val devArr = JSONArray()
            k.devices.forEach { devArr.put(it) }
            array.put(JSONObject().apply {
                put("key", k.key)
                put("merchantName", k.merchantName)
                put("shopName", k.shopName)
                put("maxUsers", k.maxUsers)
                put("usedCount", k.usedCount)
                put("devices", devArr)
                put("themeColor", k.themeColor)
                put("appName", k.appName)
                put("logoUrl", k.logoUrl)
                put("bgUrl", k.bgUrl)
                put("cguText", k.cguText)
                put("privacyText", k.privacyText)
                put("keypadLayout", k.keypadLayout)
                put("status", k.status)
                put("createdAt", k.createdAt)
            })
        }
        prefs.edit().putString("sira_license_keys_list", array.toString()).apply()
    }

    fun createLicenseKey(
        merchantName: String,
        shopName: String,
        maxUsers: Int,
        themeColor: String = "#005AC1",
        appName: String = "SIRA Business",
        keypadLayout: String = "GRID_4",
        adminEmail: String
    ): AdminLicenseKey {
        val rand = UUID.randomUUID().toString().take(6).uppercase()
        val key = "SIRA-$rand-2026"
        val newLic = AdminLicenseKey(
            key = key,
            merchantName = merchantName.trim(),
            shopName = shopName.trim(),
            maxUsers = maxUsers.coerceAtLeast(1),
            usedCount = 0,
            devices = emptyList(),
            themeColor = themeColor,
            appName = appName.trim().ifBlank { "SIRA Business" },
            keypadLayout = keypadLayout,
            status = "ACTIVE"
        )
        val updated = listOf(newLic) + _licenseKeys.value
        _licenseKeys.value = updated
        persistLicenseKeys(updated)
        recordAudit("LICENSE_CREATE", adminEmail, "Création clé API: $key (Quota: $maxUsers appareils) pour $merchantName")
        return newLic
    }

    fun toggleLicenseRevocation(key: String, adminEmail: String): String {
        val updated = _licenseKeys.value.map { lic ->
            if (lic.key == key) {
                val newStatus = if (lic.status == "ACTIVE") "REVOKED" else "ACTIVE"
                recordAudit(
                    if (newStatus == "REVOKED") "LICENSE_REVOKE" else "LICENSE_ACTIVATE",
                    adminEmail,
                    "Clé API $key passée à l'état $newStatus (Arrêt immédiat de l'APK client si révoqué)"
                )
                lic.copy(status = newStatus)
            } else lic
        }
        _licenseKeys.value = updated
        persistLicenseKeys(updated)
        return updated.find { it.key == key }?.status ?: "REVOKED"
    }

    fun updateLicenseCustomization(
        key: String,
        themeColor: String?,
        appName: String?,
        shopName: String?,
        logoUrl: String?,
        bgUrl: String?,
        cguText: String?,
        privacyText: String?,
        keypadLayout: String?,
        caller: String
    ): AdminLicenseKey? {
        val current = _licenseKeys.value.find { it.key == key } ?: return null
        val updatedLic = current.copy(
            themeColor = themeColor ?: current.themeColor,
            appName = appName ?: current.appName,
            shopName = shopName ?: current.shopName,
            logoUrl = logoUrl ?: current.logoUrl,
            bgUrl = bgUrl ?: current.bgUrl,
            cguText = cguText ?: current.cguText,
            privacyText = privacyText ?: current.privacyText,
            keypadLayout = keypadLayout ?: current.keypadLayout
        )
        val updatedList = _licenseKeys.value.map { if (it.key == key) updatedLic else it }
        _licenseKeys.value = updatedList
        persistLicenseKeys(updatedList)
        recordAudit("LICENSE_CUSTOMIZE", caller, "Personnalisation mise à jour pour clé: $key")
        return updatedLic
    }

    fun validateLicense(key: String, deviceId: String): Pair<Boolean, String?> {
        val lic = _licenseKeys.value.find { it.key == key.trim() }
            ?: return Pair(false, "Clé de licence inexistante. Contactez l'administrateur.")

        if (lic.status == "REVOKED") {
            return Pair(false, "Cette clé API de licence a été révoquée par l'administrateur SIRA. L'application est immédiatement bloquée.")
        }

        val alreadyRegistered = lic.devices.contains(deviceId)
        if (!alreadyRegistered) {
            if (lic.devices.size >= lic.maxUsers) {
                return Pair(false, "Quota maximal d'utilisateurs atteint (${lic.maxUsers}/${lic.maxUsers} appareils autorisés). Contactez la Direction pour augmenter la capacité.")
            }
            val newDevs = lic.devices + deviceId
            val updatedLic = lic.copy(devices = newDevs, usedCount = newDevs.size)
            val updatedList = _licenseKeys.value.map { if (it.key == key) updatedLic else it }
            _licenseKeys.value = updatedList
            persistLicenseKeys(updatedList)
        }

        return Pair(true, null)
    }

    fun isUserSuspended(userId: String): Boolean {
        return suspendedUsers.contains(userId)
    }

    fun isUserApproved(userId: String): Boolean {
        return approvedUsers.contains(userId)
    }

    fun getUserRole(userId: String): SiraRole {
        if (userId.contains("sawadogoafis") || userId.contains("admin")) return SiraRole.SUPER_ADMIN
        return userRoles[userId] ?: SiraRole.MERCHANT
    }

    fun toggleUserSuspension(userId: String, adminEmail: String): Boolean {
        val currentlySuspended = suspendedUsers.contains(userId)
        if (currentlySuspended) {
            suspendedUsers.remove(userId)
            recordAudit("USER_ACTIVATE", adminEmail, "Réactivation administrative du compte: $userId")
        } else {
            suspendedUsers.add(userId)
            recordAudit("USER_SUSPEND", adminEmail, "Suspension administrative du compte: $userId")
        }
        prefs.edit().putStringSet("suspended_users", suspendedUsers.toSet()).apply()
        return !currentlySuspended
    }

    fun toggleMerchantApproval(userId: String, adminEmail: String): Boolean {
        val currentlyApproved = approvedUsers.contains(userId)
        if (currentlyApproved) {
            approvedUsers.remove(userId)
            recordAudit("APPROVAL_REVOKE", adminEmail, "Révocation de l'agrément officiel pour: $userId")
        } else {
            approvedUsers.add(userId)
            recordAudit("APPROVAL_GRANT", adminEmail, "Validation et octroi de l'agrément officiel SIRA pour: $userId")
        }
        prefs.edit().putStringSet("approved_users", approvedUsers.toSet()).apply()
        return !currentlyApproved
    }

    fun setUserRole(userId: String, role: SiraRole, adminEmail: String) {
        userRoles[userId] = role
        recordAudit("ROLE_CHANGE", adminEmail, "Modification du rôle de $userId -> ${role.label}")
    }

    fun broadcastDirective(title: String, message: String, adminEmail: String, priority: String = "INFO") {
        val newDirective = AdminDirective(
            title = title.trim(),
            message = message.trim(),
            author = adminEmail,
            priority = priority
        )
        val updated = listOf(newDirective) + _directives.value.take(9)
        _directives.value = updated

        // Persist
        val array = JSONArray()
        updated.forEach { d ->
            array.put(JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("message", d.message)
                put("author", d.author)
                put("priority", d.priority)
                put("timestamp", d.timestamp)
            })
        }
        prefs.edit().putString("admin_directives", array.toString()).apply()
        recordAudit("DIRECTIVE_BROADCAST", adminEmail, "Publication de la directive officielle : $title")
    }

    fun registerNewMerchant(
        email: String,
        displayName: String,
        shopName: String,
        city: String,
        phone: String,
        role: SiraRole,
        adminEmail: String
    ): MerchantAccountInfo {
        val cleanEmail = email.trim().lowercase()
        val userId = cleanEmail.replace("@", "_").replace(".", "_")
        userRoles[userId] = role
        approvedUsers.add(userId)
        prefs.edit().putStringSet("approved_users", approvedUsers.toSet()).apply()

        val ifuNumber = "BF-IFU-${(10000000..99999999).random()}"
        saveMerchantProfile(userId, cleanEmail, displayName, shopName, city, phone, ifuNumber)

        // Create initial isolated local database for this user (starts completely blank)
        val userDb = SiraDatabase.getDatabaseForUser(context, userId, scope)

        recordAudit("USER_REGISTER", adminEmail, "Création et intégration du nouveau commerçant réel: $displayName ($shopName, $city)")

        return MerchantAccountInfo(
            id = userId,
            email = cleanEmail,
            displayName = displayName,
            shopName = shopName,
            city = city,
            phone = phone,
            role = role,
            dbFileName = "sira_user_$userId.db",
            dbSizeBytes = 40960L,
            productCount = 0,
            saleCount = 0,
            totalRevenueFcfa = 0L,
            isActive = true,
            isApproved = true,
            ifuNumber = ifuNumber,
            lastActiveTimestamp = System.currentTimeMillis()
        )
    }

    fun saveMerchantProfile(id: String, email: String, name: String, shop: String, city: String, phone: String, ifu: String) {
        val json = JSONObject().apply {
            put("id", id)
            put("email", email)
            put("name", name)
            put("shop", shop)
            put("city", city)
            put("phone", phone)
            put("ifu", ifu)
        }
        prefs.edit().putString("merchant_meta_$id", json.toString()).apply()
    }

    private fun getMerchantProfile(id: String): Tuple5? {
        val raw = prefs.getString("merchant_meta_$id", null) ?: return null
        return try {
            val obj = JSONObject(raw)
            Tuple5(
                obj.optString("shop"),
                obj.optString("name"),
                obj.optString("city"),
                obj.optString("phone"),
                obj.optString("ifu")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveApiKeys(keys: AdminApiKeys, adminEmail: String) {
        prefs.edit()
            .putString("gemini_key", keys.geminiApiKey)
            .putString("cinetpay_key", keys.cinetPayApiKey)
            .putString("cinetpay_site", keys.cinetPaySiteId)
            .putString("paydunya_key", keys.payDunyaMasterKey)
            .putString("paydunya_token", keys.payDunyaToken)
            .putString("orange_code", keys.orangeMoneyMerchantCode)
            .putString("orange_key", keys.orangeMoneyApiKey)
            .apply()

        _apiKeys.value = keys
        if (keys.geminiApiKey.isNotBlank()) {
            GeminiClient.setCustomApiKey(keys.geminiApiKey)
        }
        recordAudit("API_KEY_UPDATE", adminEmail, "Mise à jour sécurisée du trousseau de clés API de production")
    }

    fun recordAudit(action: String, performedBy: String, details: String) {
        val entry = AdminAuditEntry(action = action, performedBy = performedBy, details = details)
        _auditLogs.value = listOf(entry) + _auditLogs.value.take(49)
    }

    suspend fun getLocalMerchantAccounts(): List<MerchantAccountInfo> = withContext(Dispatchers.IO) {
        val dbDir = context.getDatabasePath("dummy").parentFile ?: return@withContext emptyList()
        val dbFiles = dbDir.listFiles { _, name -> name.startsWith("sira_user_") && name.endsWith(".db") } ?: emptyArray()

        val accounts = mutableListOf<MerchantAccountInfo>()
        for (dbFile in dbFiles) {
            val rawId = dbFile.name.removePrefix("sira_user_").removeSuffix(".db")
            val isSuspended = suspendedUsers.contains(rawId)
            val isApproved = approvedUsers.contains(rawId)
            val role = getUserRole(rawId)

            val db = SiraDatabase.getDatabaseForUser(context, rawId, scope)
            val products = try { db.productDao().getAllProductsSync() } catch (e: Exception) { emptyList() }
            val sales = try { db.saleDao().getAllSalesSync() } catch (e: Exception) { emptyList() }
            val totalRev = sales.sumOf { it.totalAmount.toLong() }

            val meta = getMerchantProfile(rawId)
            val (shopName, merchantName, city, phone, ifu) = when {
                rawId == "sawadogoafis125_gmail_com" -> Tuple5("SIRA Direction Centrale", "Afis Sawadogo", "Ouagadougou", "+226 70 00 00 00", "BF-ADMIN-001")
                meta != null -> meta
                else -> Tuple5("Commerce local", rawId.replace("_", " "), "Burkina Faso", "", "Non renseigné")
            }

            accounts.add(
                MerchantAccountInfo(
                    id = rawId,
                    email = if (rawId.contains("@")) rawId else "$rawId@sira.bf",
                    displayName = merchantName,
                    shopName = shopName,
                    city = city,
                    phone = phone,
                    role = role,
                    dbFileName = dbFile.name,
                    dbSizeBytes = dbFile.length(),
                    productCount = products.size,
                    saleCount = sales.size,
                    totalRevenueFcfa = totalRev,
                    isActive = !isSuspended,
                    isApproved = isApproved,
                    ifuNumber = ifu,
                    lastActiveTimestamp = dbFile.lastModified()
                )
            )
        }

        if (accounts.isEmpty()) {
            val adminDb = SiraDatabase.getDatabaseForUser(context, "sawadogoafis125_gmail_com", scope)
            val file = context.getDatabasePath("sira_user_sawadogoafis125_gmail_com.db")
            val pCount = try { adminDb.productDao().getAllProductsSync().size } catch (e: Exception) { 0 }
            val sCount = try { adminDb.saleDao().getAllSalesSync().size } catch (e: Exception) { 0 }
            val rev = try { adminDb.saleDao().getAllSalesSync().sumOf { it.totalAmount.toLong() } } catch (e: Exception) { 0L }
            accounts.add(
                MerchantAccountInfo(
                    id = "sawadogoafis125_gmail_com",
                    email = "sawadogoafis125@gmail.com",
                    displayName = "Afis Sawadogo",
                    shopName = "SIRA Direction Centrale",
                    city = "Ouagadougou",
                    phone = "+226 70 00 00 00",
                    role = SiraRole.SUPER_ADMIN,
                    dbFileName = "sira_user_sawadogoafis125_gmail_com.db",
                    dbSizeBytes = if (file.exists()) file.length() else 40960L,
                    productCount = pCount,
                    saleCount = sCount,
                    totalRevenueFcfa = rev,
                    isActive = true,
                    isApproved = true,
                    ifuNumber = "BF-ADMIN-001",
                    lastActiveTimestamp = System.currentTimeMillis()
                )
            )
        }

        accounts.sortedByDescending { it.lastActiveTimestamp }
    }

    fun updateServerStatus(port: Int, isRunning: Boolean) {
        _serverMetrics.value = calculateMetrics(port, isRunning)
    }

    private fun calculateMetrics(port: Int, isRunning: Boolean): SystemServerMetrics {
        val runtime = Runtime.getRuntime()
        val freeMb = runtime.freeMemory() / (1024 * 1024)
        val totalMb = runtime.totalMemory() / (1024 * 1024)
        val uptime = (System.currentTimeMillis() - startTime) / 1000

        val dbDir = context.getDatabasePath("dummy").parentFile
        val activeCount = dbDir?.listFiles { _, name -> name.startsWith("sira_user_") && name.endsWith(".db") }?.size ?: 1

        return SystemServerMetrics(
            serverStatus = "PORTAIL CLOUD SÉCURISÉ (HTTPS)",
            serverUrl = cloudAdminUrl,
            cloudPortalUrl = cloudAdminUrl,
            port = port,
            jvmFreeMemoryMb = freeMb,
            jvmTotalMemoryMb = totalMb,
            activeDatabasesCount = activeCount,
            totalMerchantsCount = activeCount,
            uptimeSeconds = uptime
        )
    }

    private data class Tuple5(val a: String, val b: String, val c: String, val d: String, val e: String)

    companion object {
        @Volatile
        private var INSTANCE: AdminManager? = null

        fun getInstance(context: Context): AdminManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdminManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
