package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AppAutoUpdate(private val context: Context) {
    data class UpdateInfo(val available: Boolean, val latestVersion: String, val mandatory: Boolean, val title: String, val notes: String, val apkUrl: String)

    fun check(): UpdateInfo? {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        val conn = (URL("https://sira-manager-admin.vercel.app/api/updates/latest?app=business&version=$version").openConnection() as HttpURLConnection)
        return try {
            conn.connectTimeout = 5000; conn.readTimeout = 7000
            val code = conn.responseCode
            if (code !in 200..299) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            if (!json.optBoolean("available", false)) return null
            UpdateInfo(true, json.optString("latestVersion"), json.optBoolean("mandatory"), json.optString("title"), json.optString("notes"), json.optString("apkUrl"))
        } finally { conn.disconnect() }
    }

    fun downloadAndInstall(info: UpdateInfo): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(dir, "sira-business-${info.latestVersion}.apk")
        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection)
        try {
            conn.connectTimeout = 8000; conn.readTimeout = 30000
            if (conn.responseCode !in 200..299) error("Téléchargement impossible")
            conn.inputStream.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
        } finally { conn.disconnect() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { context.startActivity(intent) }
        catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settings)
            } else throw e
        }
    }
}
