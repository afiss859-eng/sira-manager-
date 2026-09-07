package com.example.admin

import android.content.Context
import com.example.auth.SiraRole
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

class AdminWebServer(private val context: Context) {

    private val adminManager = AdminManager.getInstance(context)
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var currentPort = 8080
        private set

    fun start(preferredPort: Int = 8080) {
        if (isRunning) return
        scope.launch {
            try {
                currentPort = preferredPort
                serverSocket = try {
                    ServerSocket(preferredPort)
                } catch (e: Exception) {
                    ServerSocket(8081).also { currentPort = 8081 }
                }
                isRunning = true
                adminManager.updateServerStatus(currentPort, true)
                adminManager.recordAudit("SERVER_START", "admin@sira.bf", "Portail d'Administration SIRA en ligne (Synchronisation Cloud)")

                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch { handleClient(clientSocket) }
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            } catch (e: Exception) {
                adminManager.updateServerStatus(currentPort, false)
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        adminManager.updateServerStatus(currentPort, false)
        adminManager.recordAudit("SERVER_STOP", "admin@sira.bf", "Arrêt du service web d'administration")
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            val output = BufferedOutputStream(socket.getOutputStream())

            val requestLine = input.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val path = parts[1].split("?")[0]

            // Read headers to extract Content-Length
            var contentLength = 0
            var line: String?
            while (input.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Read Body if POST
            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = input.read(chars, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(chars, 0, totalRead)
            } else ""

            when {
                // Static Root & Web UI
                method == "GET" && (path == "/" || path == "/index.html" || path == "/admin") -> {
                    serveAsset(output, "admin_panel/index.html", "text/html; charset=UTF-8")
                }
                method == "GET" && (path == "/sira_logo.jpg" || path == "/logo.jpg" || path == "/assets/sira_logo.jpg") -> {
                    serveAsset(output, "admin_panel/sira_logo.jpg", "image/jpeg")
                }

                // API: System Metrics & Cloud Info
                method == "GET" && path == "/api/system" -> {
                    val metrics = adminManager.serverMetrics.value
                    val json = JSONObject().apply {
                        put("serverStatus", metrics.serverStatus)
                        put("serverUrl", metrics.serverUrl)
                        put("cloudPortalUrl", metrics.cloudPortalUrl)
                        put("port", metrics.port)
                        put("jvmFreeMemoryMb", metrics.jvmFreeMemoryMb)
                        put("jvmTotalMemoryMb", metrics.jvmTotalMemoryMb)
                        put("activeDatabasesCount", metrics.activeDatabasesCount)
                        put("totalMerchantsCount", metrics.totalMerchantsCount)
                        put("uptimeSeconds", metrics.uptimeSeconds)
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                // API: Get All Users / Merchants
                method == "GET" && path == "/api/users" -> {
                    val users = adminManager.getLocalMerchantAccounts()
                    val array = JSONArray()
                    users.forEach { u ->
                        array.put(JSONObject().apply {
                            put("id", u.id)
                            put("email", u.email)
                            put("displayName", u.displayName)
                            put("shopName", u.shopName)
                            put("city", u.city)
                            put("phone", u.phone)
                            put("role", u.role.name)
                            put("roleLabel", u.role.label)
                            put("dbFileName", u.dbFileName)
                            put("dbSizeBytes", u.dbSizeBytes)
                            put("productCount", u.productCount)
                            put("saleCount", u.saleCount)
                            put("totalRevenueFcfa", u.totalRevenueFcfa)
                            put("isActive", u.isActive)
                            put("isApproved", u.isApproved)
                            put("ifuNumber", u.ifuNumber)
                            put("lastActiveTimestamp", u.lastActiveTimestamp)
                        })
                    }
                    sendJsonResponse(output, 200, array.toString())
                }

                // API: Toggle User Active (Suspend / Activate)
                method == "POST" && path == "/api/users/toggle" -> {
                    val req = JSONObject(body)
                    val userId = req.optString("userId")
                    val newActive = adminManager.toggleUserSuspension(userId, "admin_cloud")
                    sendJsonResponse(output, 200, JSONObject().put("success", true).put("isActive", newActive).toString())
                }

                // API: Toggle Merchant Approval (Agrément SIRA)
                method == "POST" && path == "/api/users/approve" -> {
                    val req = JSONObject(body)
                    val userId = req.optString("userId")
                    val newApproved = adminManager.toggleMerchantApproval(userId, "admin_cloud")
                    sendJsonResponse(output, 200, JSONObject().put("success", true).put("isApproved", newApproved).toString())
                }

                // API: Change User Role
                method == "POST" && path == "/api/users/role" -> {
                    val req = JSONObject(body)
                    val userId = req.optString("userId")
                    val roleStr = req.optString("role")
                    val role = try { SiraRole.valueOf(roleStr) } catch (e: Exception) { SiraRole.MERCHANT }
                    adminManager.setUserRole(userId, role, "admin_cloud")
                    sendJsonResponse(output, 200, JSONObject().put("success", true).put("role", role.name).toString())
                }

                // API: Register / Invite New Merchant
                method == "POST" && path == "/api/users/create" -> {
                    val req = JSONObject(body)
                    val email = req.optString("email")
                    val name = req.optString("displayName")
                    val shop = req.optString("shopName")
                    val city = req.optString("city")
                    val phone = req.optString("phone")
                    val roleStr = req.optString("role", "MERCHANT")
                    val role = try { SiraRole.valueOf(roleStr) } catch (e: Exception) { SiraRole.MERCHANT }

                    val created = adminManager.registerNewMerchant(
                        email = email,
                        displayName = name,
                        shopName = shop,
                        city = city,
                        phone = phone,
                        role = role,
                        adminEmail = "admin_cloud"
                    )
                    sendJsonResponse(output, 200, JSONObject().put("success", true).put("userId", created.id).toString())
                }

                // API: Directives (Get & Broadcast)
                method == "GET" && path == "/api/directives" -> {
                    val directives = adminManager.directives.value
                    val array = JSONArray()
                    directives.forEach { d ->
                        array.put(JSONObject().apply {
                            put("id", d.id)
                            put("title", d.title)
                            put("message", d.message)
                            put("author", d.author)
                            put("priority", d.priority)
                            put("timestamp", d.timestamp)
                        })
                    }
                    sendJsonResponse(output, 200, array.toString())
                }

                method == "POST" && path == "/api/directives" -> {
                    val req = JSONObject(body)
                    val title = req.optString("title")
                    val message = req.optString("message")
                    val priority = req.optString("priority", "INFO")
                    adminManager.broadcastDirective(title, message, "admin_cloud", priority)
                    sendJsonResponse(output, 200, JSONObject().put("success", true).toString())
                }

                // API: Get Keys
                method == "GET" && path == "/api/keys" -> {
                    val keys = adminManager.apiKeys.value
                    val json = JSONObject().apply {
                        put("geminiApiKey", keys.geminiApiKey)
                        put("cinetPayApiKey", keys.cinetPayApiKey)
                        put("cinetPaySiteId", keys.cinetPaySiteId)
                        put("payDunyaMasterKey", keys.payDunyaMasterKey)
                        put("payDunyaToken", keys.payDunyaToken)
                        put("orangeMoneyMerchantCode", keys.orangeMoneyMerchantCode)
                        put("orangeMoneyApiKey", keys.orangeMoneyApiKey)
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                // API: Save Keys
                method == "POST" && path == "/api/keys" -> {
                    val req = JSONObject(body)
                    val keys = AdminApiKeys(
                        geminiApiKey = req.optString("geminiApiKey"),
                        cinetPayApiKey = req.optString("cinetPayApiKey"),
                        cinetPaySiteId = req.optString("cinetPaySiteId"),
                        payDunyaMasterKey = req.optString("payDunyaMasterKey"),
                        payDunyaToken = req.optString("payDunyaToken"),
                        orangeMoneyMerchantCode = req.optString("orangeMoneyMerchantCode"),
                        orangeMoneyApiKey = req.optString("orangeMoneyApiKey")
                    )
                    adminManager.saveApiKeys(keys, "admin_cloud")
                    sendJsonResponse(output, 200, JSONObject().put("success", true).toString())
                }

                // API: Monetization & Client License Keys
                method == "GET" && path == "/api/licenses" -> {
                    val licenses = adminManager.licenseKeys.value
                    val array = JSONArray()
                    licenses.forEach { l ->
                        val devArr = JSONArray()
                        l.devices.forEach { devArr.put(it) }
                        array.put(JSONObject().apply {
                            put("key", l.key)
                            put("merchantName", l.merchantName)
                            put("shopName", l.shopName)
                            put("maxUsers", l.maxUsers)
                            put("usedCount", l.usedCount)
                            put("devices", devArr)
                            put("themeColor", l.themeColor)
                            put("appName", l.appName)
                            put("logoUrl", l.logoUrl)
                            put("bgUrl", l.bgUrl)
                            put("cguText", l.cguText)
                            put("privacyText", l.privacyText)
                            put("keypadLayout", l.keypadLayout)
                            put("status", l.status)
                            put("createdAt", l.createdAt)
                        })
                    }
                    sendJsonResponse(output, 200, array.toString())
                }

                method == "POST" && path == "/api/licenses/create" -> {
                    val req = JSONObject(body)
                    val mName = req.optString("merchantName", "Commerçant")
                    val sName = req.optString("shopName", mName)
                    val maxU = req.optInt("maxUsers", 1)
                    val color = req.optString("themeColor", "#005AC1")
                    val appN = req.optString("appName", "SIRA Business")
                    val kLayout = req.optString("keypadLayout", "GRID_4")

                    val created = adminManager.createLicenseKey(
                        merchantName = mName,
                        shopName = sName,
                        maxUsers = maxU,
                        themeColor = color,
                        appName = appN,
                        keypadLayout = kLayout,
                        adminEmail = "admin_cloud"
                    )
                    sendJsonResponse(output, 200, JSONObject().put("success", true).put("key", created.key).toString())
                }

                method == "POST" && path == "/api/licenses/revoke" -> {
                    val req = JSONObject(body)
                    val key = req.optString("key")
                    val newStatus = adminManager.toggleLicenseRevocation(key, "admin_cloud")
                    sendJsonResponse(output, 200, JSONObject().put("success", true).put("status", newStatus).toString())
                }

                method == "POST" && path == "/api/licenses/customize" -> {
                    val req = JSONObject(body)
                    val key = req.optString("key")
                    val color = if (req.has("themeColor")) req.optString("themeColor") else null
                    val appN = if (req.has("appName")) req.optString("appName") else null
                    val shopN = if (req.has("shopName")) req.optString("shopName") else null
                    val logo = if (req.has("logoUrl")) req.optString("logoUrl") else null
                    val bg = if (req.has("bgUrl")) req.optString("bgUrl") else null
                    val cgu = if (req.has("cguText")) req.optString("cguText") else null
                    val priv = if (req.has("privacyText")) req.optString("privacyText") else null
                    val layout = if (req.has("keypadLayout")) req.optString("keypadLayout") else null

                    val updated = adminManager.updateLicenseCustomization(
                        key = key,
                        themeColor = color,
                        appName = appN,
                        shopName = shopN,
                        logoUrl = logo,
                        bgUrl = bg,
                        cguText = cgu,
                        privacyText = priv,
                        keypadLayout = layout,
                        caller = "client_or_admin"
                    )
                    sendJsonResponse(output, 200, JSONObject().put("success", updated != null).toString())
                }

                method == "POST" && path == "/api/licenses/validate" -> {
                    val req = JSONObject(body)
                    val key = req.optString("key")
                    val deviceId = req.optString("deviceId", "unknown_device")

                    val (valid, errorMsg) = adminManager.validateLicense(key, deviceId)
                    val lic = adminManager.licenseKeys.value.find { it.key == key.trim() }
                    val json = JSONObject().apply {
                        put("valid", valid)
                        if (!valid) {
                            put("error", errorMsg ?: "Clé invalide.")
                            put("revoked", lic?.status == "REVOKED")
                        } else if (lic != null) {
                            put("license", JSONObject().apply {
                                put("key", lic.key)
                                put("merchantName", lic.merchantName)
                                put("shopName", lic.shopName)
                                put("maxUsers", lic.maxUsers)
                                put("usedCount", lic.usedCount)
                                put("themeColor", lic.themeColor)
                                put("appName", lic.appName)
                                put("logoUrl", lic.logoUrl)
                                put("bgUrl", lic.bgUrl)
                                put("cguText", lic.cguText)
                                put("privacyText", lic.privacyText)
                                put("keypadLayout", lic.keypadLayout)
                                put("status", lic.status)
                            })
                        }
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                // API: Audit Logs
                method == "GET" && path == "/api/logs" -> {
                    val logs = adminManager.auditLogs.value
                    val array = JSONArray()
                    logs.forEach { l ->
                        array.put(JSONObject().apply {
                            put("id", l.id)
                            put("action", l.action)
                            put("performedBy", l.performedBy)
                            put("details", l.details)
                            put("timestamp", l.timestamp)
                        })
                    }
                    sendJsonResponse(output, 200, array.toString())
                }

                else -> {
                    val notFound = "404 Not Found"
                    sendRawResponse(output, 404, "text/plain", notFound.toByteArray())
                }
            }
        } catch (ignored: Exception) {
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    private fun serveAsset(output: OutputStream, assetPath: String, contentType: String) {
        try {
            val bytes = context.assets.open(assetPath).use { it.readBytes() }
            sendRawResponse(output, 200, contentType, bytes)
        } catch (e: Exception) {
            val err = "Fichier introuvable: $assetPath".toByteArray()
            sendRawResponse(output, 404, "text/plain; charset=UTF-8", err)
        }
    }

    private fun sendJsonResponse(output: OutputStream, code: Int, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        sendRawResponse(output, code, "application/json; charset=UTF-8", bytes)
    }

    private fun sendRawResponse(output: OutputStream, code: Int, contentType: String, data: ByteArray) {
        val statusText = if (code == 200) "OK" else if (code == 404) "Not Found" else "Internal Error"
        val header = "HTTP/1.1 $code $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${data.size}\r\n" +
                "Connection: close\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(data)
        output.flush()
    }
}
