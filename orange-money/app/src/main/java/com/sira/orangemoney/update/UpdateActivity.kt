package com.sira.orangemoney.update

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast

class UpdateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p = intent.getStringExtra("update_info")?.split("\n", limit = 4)
        val apkUrl = intent.getStringExtra("apk_url").orEmpty()
        if (p == null || p.size < 4 || apkUrl.isBlank()) { finish(); return }
        val version = p[0]; val title = p[2]; val notes = p[3]
        val message = buildString {
            append("Une mise à jour obligatoire est disponible : $version\n\n")
            if (notes.isNotBlank()) append(notes.take(1400))
        }
        AlertDialog.Builder(this)
            .setTitle(title.ifBlank { "Mise à jour obligatoire SIRA Orange Money" })
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Mettre à jour maintenant") { _, _ ->
                Toast.makeText(this, "Téléchargement de la mise à jour…", Toast.LENGTH_SHORT).show()
                val info = AppAutoUpdate.UpdateInfo(true, version, true, title, notes, apkUrl)
                Thread { AppAutoUpdate(this).downloadAndInstall(info) }.start()
            }
            .setOnDismissListener { if (!isFinishing) finishAffinity() }
            .show()
    }
}
