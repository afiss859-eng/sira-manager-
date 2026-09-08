package com.example.update

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class UpdateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val info = intent.getStringExtra("update_info")?.let { decode(it) }
        if (info == null) { finish(); return }
        val message = buildString {
            append("Nouvelle version : ${info.latestVersion}\n\n")
            if (info.notes.isNotBlank()) append(info.notes.take(1400))
        }
        AlertDialog.Builder(this)
            .setTitle(info.title.ifBlank { "Mise à jour SIRA disponible" })
            .setMessage(message)
            .setPositiveButton(if (info.mandatory) "Mettre à jour" else "Télécharger") { _, _ ->
                Toast.makeText(this, "Téléchargement de la mise à jour…", Toast.LENGTH_SHORT).show()
                Thread { AppAutoUpdate(this).downloadAndInstall(info) }.start()
            }
            .apply { if (!info.mandatory) setNegativeButton("Plus tard") { _, _ -> finish() } }
            .setOnDismissListener { if (isFinishing.not()) finish() }
            .show()
    }

    private fun decode(raw: String): AppAutoUpdate.UpdateInfo? {
        val p = raw.split("\n", limit = 4)
        if (p.size < 4) return null
        return AppAutoUpdate.UpdateInfo(true, p[0], p[1] == "1", p[2], p[3], intent.getStringExtra("apk_url") ?: "")
    }
}
