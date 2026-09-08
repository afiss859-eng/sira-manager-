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
        val version = p[0]; val mandatory = p[1] == "1"; val title = p[2]; val notes = p[3]
        AlertDialog.Builder(this)
            .setTitle(title.ifBlank { "Mise à jour SIRA Orange Money disponible" })
            .setMessage("Nouvelle version : $version\n\n${notes.take(1400)}")
            .setPositiveButton(if (mandatory) "Mettre à jour" else "Télécharger") { _, _ ->
                Toast.makeText(this, "Téléchargement de la mise à jour…", Toast.LENGTH_SHORT).show()
                Thread { AppAutoUpdate(this).downloadAndInstall(AppAutoUpdate.UpdateInfo(true, version, mandatory, title, notes, apkUrl)) }.start()
            }
            .apply { if (!mandatory) setNegativeButton("Plus tard") { _, _ -> finish() } }
            .setOnDismissListener { finish() }
            .show()
    }
}
