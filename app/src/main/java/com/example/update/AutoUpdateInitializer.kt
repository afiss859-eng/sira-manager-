package com.example.update

import android.app.Application
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.content.Intent
import androidx.startup.Initializer
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class AutoUpdateInitializer : Initializer<Unit> {
    override fun create(context: android.content.Context) {
        val app = context.applicationContext as? Application ?: return
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            var shown = false
            override fun onActivityResumed(activity: Activity) {
                if (shown || activity is UpdateActivity) return
                shown = true
                Executors.newSingleThreadExecutor().execute {
                    val version = runCatching { activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "0.0.0" }.getOrDefault("0.0.0")
                    val url = "https://sira-manager-admin.vercel.app/api/updates/latest?app=business&version=$version"
                    val json = runCatching {
                        val c = URL(url).openConnection() as HttpURLConnection
                        try { c.connectTimeout = 4000; c.readTimeout = 6000; if (c.responseCode !in 200..299) return@runCatching null; JSONObject(c.inputStream.bufferedReader().use { it.readText() }) } finally { c.disconnect() }
                    }.getOrNull() ?: return@execute
                    if (!json.optBoolean("available", false)) return@execute
                    Handler(Looper.getMainLooper()).post {
                        val info = listOf(json.optString("latestVersion"), if (json.optBoolean("mandatory")) "1" else "0", json.optString("title"), json.optString("notes")).joinToString("\n")
                        activity.startActivity(Intent(activity, UpdateActivity::class.java).putExtra("update_info", info).putExtra("apk_url", json.optString("apkUrl")))
                    }
                }
            }
            override fun onActivityCreated(a: Activity, s: android.os.Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, s: android.os.Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
