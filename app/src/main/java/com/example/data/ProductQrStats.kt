package com.example.data

import android.content.Context
import org.json.JSONObject

object ProductQrStats {
    private const val PREFS = "sira_qr_stats"
    private const val KEY_COUNTS = "scan_counts"

    @Synchronized
    fun recordScan(context: Context, payload: String) {
        val key = payload.trim(); if (key.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = runCatching { JSONObject(prefs.getString(KEY_COUNTS, "{}") ?: "{}") }.getOrDefault(JSONObject())
        json.put(key, json.optInt(key, 0) + 1)
        prefs.edit().putString(KEY_COUNTS, json.toString()).apply()
    }

    fun scanCount(context: Context, payload: String): Int = runCatching {
        val json = JSONObject(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_COUNTS, "{}") ?: "{}")
        json.optInt(payload.trim(), 0)
    }.getOrDefault(0)

    fun top(context: Context, payloads: List<String>, limit: Int = 5): List<Pair<String, Int>> =
        payloads.map { it to scanCount(context, it) }.filter { it.second > 0 }.sortedByDescending { it.second }.take(limit)
}
