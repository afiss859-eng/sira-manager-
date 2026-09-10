package com.example.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

/** Minimal ESC/POS Bluetooth printer engine for common 58/80mm thermal printers. */
class SiraBluetoothPrinter(private val context: Context) {
    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PREFS = "sira_printer"
        private const val KEY_MAC = "default_mac"
    }

    fun setDefaultPrinter(macAddress: String) {
        require(BluetoothAdapter.checkBluetoothAddress(macAddress)) { "Adresse Bluetooth invalide" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MAC, macAddress.uppercase()).apply()
    }
    fun getDefaultPrinterMac(): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MAC, null)

    @SuppressLint("MissingPermission")
    fun pairedPrinters(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices.sortedBy { it.name ?: it.address }
    }

    @SuppressLint("MissingPermission")
    fun printSale(sale: Sale, items: List<SaleItem>): Result<Unit> {
        return withPrinter { output -> output.write(buildReceipt(sale, items).toByteArray(Charset.forName("CP437"))) }
    }

    /** Print a compact shelf/product label containing readable name, price and a real QR payload. */
    @SuppressLint("MissingPermission")
    fun printProductQrLabel(productName: String, price: Double, payload: String, currency: String = "FCFA"): Result<Unit> {
        val safeName = productName.trim().take(28)
        val safePayload = payload.trim()
        if (safeName.isBlank() || safePayload.isBlank() || price <= 0) return Result.failure(IllegalArgumentException("Nom, prix ou QR invalide"))
        return withPrinter { output ->
            output.write("\u001B\u0040".toByteArray())
            output.write("\u001B\u0061\u0001".toByteArray())
            output.write("$safeName\n".toByteArray(Charset.forName("CP437")))
            output.write("${money(price)} $currency\n".toByteArray(Charset.forName("CP437")))
            output.write(buildQrCommand(safePayload))
            output.write("\n\n\u001D\u0056\u0000".toByteArray())
            output.flush()
        }
    }

    @SuppressLint("MissingPermission")
    private fun withPrinter(block: (java.io.OutputStream) -> Unit): Result<Unit> {
        if (!hasBluetoothPermission()) return Result.failure(IllegalStateException("Autorisation Bluetooth requise"))
        val mac = getDefaultPrinterMac() ?: return Result.failure(IllegalStateException("Aucune imprimante par défaut configurée"))
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return Result.failure(IllegalStateException("Bluetooth indisponible sur cet appareil"))
        if (!adapter.isEnabled) return Result.failure(IllegalStateException("Bluetooth désactivé"))
        val device = try { adapter.getRemoteDevice(mac) } catch (_: IllegalArgumentException) { return Result.failure(IllegalStateException("Imprimante Bluetooth introuvable")) }
        return try {
            adapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.use { it.connect(); block(it.outputStream) }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("Impossible d'imprimer : ${e.message ?: "connexion refusée"}", e))
        }
    }

    private fun hasBluetoothPermission(): Boolean = if (android.os.Build.VERSION.SDK_INT >= 31) ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED else true

    /** ESC/POS GS ( k QR: model 2, size 6, error correction M, then store+print. */
    private fun buildQrCommand(data: String): ByteArray {
        val bytes = data.toByteArray(Charsets.UTF_8)
        val pL = ((bytes.size + 3) and 0xFF).toByte()
        val pH = (((bytes.size + 3) shr 8) and 0xFF).toByte()
        val out = java.io.ByteArrayOutputStream()
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06))
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31))
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        out.write(bytes)
        out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
        return out.toByteArray()
    }

    private fun buildReceipt(sale: Sale, items: List<SaleItem>): String {
        val sb = StringBuilder().apply {
            append("\u001B\u0040\u001B\u0061\u0001SIRA\nRECU DE VENTE\n\u001B\u0061\u0000")
            append("--------------------------------\nRef: ${sale.reference}\nClient: ${sale.customerName}\nPaiement: ${sale.paymentMethod.label}\n--------------------------------\n")
        }
        items.forEach { item -> sb.append("${item.productName.take(24)}\n  ${item.quantity} x ${money(item.unitPrice)} = ${money(item.subtotal)} F\n") }
        sb.append("--------------------------------\nTOTAL: ${money(sale.totalAmount)} FCFA\n--------------------------------\nMerci pour votre achat !\n\n\n\u001D\u0056\u0000")
        return sb.toString()
    }
    private fun money(value: Double): String = "%,.0f".format(java.util.Locale.US, value).replace(',', ' ')
}
