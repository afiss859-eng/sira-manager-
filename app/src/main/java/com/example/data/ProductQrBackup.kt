package com.example.data

import com.example.data.entity.ProductQrCode
import com.example.viewmodel.SiraViewModel

object ProductQrBackup {
    const val HEADER = "product_id,product_name,price,currency,payload,variant,created_at"

    fun toCsv(codes: List<ProductQrCode>): String = buildString {
        append(HEADER).append('\n')
        codes.sortedBy { it.productName.lowercase() }.forEach { code ->
            append(row(code.productId.toString(), code.productName, code.price.toString(), code.currency, code.payload, code.variant, code.createdAt.toString())).append('\n')
        }
    }

    fun parseCsv(csv: String): List<BackupRow> {
        val lines = csv.lineSequence().map { it.trimEnd('\r') }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty() || lines.first().trim().lowercase() != HEADER) return emptyList()
        return lines.drop(1).mapNotNull { parseRow(it) }
    }

    private fun row(vararg values: String): String = values.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }

    private fun parseRow(line: String): BackupRow? {
        val fields = mutableListOf<String>(); val current = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (quoted && i + 1 < line.length && line[i + 1] == '"') { current.append('"'); i++ }
                else quoted = !quoted
            } else if (ch == ',' && !quoted) { fields += current.toString(); current.setLength(0) } else current.append(ch)
            i++
        }
        fields += current.toString()
        if (fields.size < 7) return null
        val payload = fields[4].trim(); val name = fields[1].trim(); val price = fields[2].toDoubleOrNull() ?: return null
        if (payload.isBlank() || name.isBlank() || price <= 0) return null
        return BackupRow(name, price, fields[3].ifBlank { "XOF" }, payload, fields[5], fields[6].toLongOrNull() ?: 0L)
    }

    data class BackupRow(val name: String, val price: Double, val currency: String, val payload: String, val variant: String, val createdAt: Long)
}

/** Restore a CSV backup without duplicating existing QR references. */
suspend fun SiraViewModel.importProductQrCsv(csv: String): Int {
    val rows = ProductQrBackup.parseCsv(csv)
    if (rows.isEmpty()) return 0
    var imported = 0
    for (row in rows) {
        val finalName = if (row.variant.isBlank()) row.name else "${row.name} — ${row.variant}"
        saveScannedProductQr(row.payload, finalName, row.price, "Import QR", 0)
        imported++
    }
    return imported
}
