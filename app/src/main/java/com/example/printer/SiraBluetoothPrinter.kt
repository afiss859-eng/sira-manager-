package com.example.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

/**
 * Minimal ESC/POS Bluetooth printer engine for common 58/80mm thermal printers.
 * It never invents a printer: callers must select a paired printer MAC address.
 */
class SiraBluetoothPrinter(private val context: Context) {
    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PREFS = "sira_printer"
        private const val KEY_MAC = "default_mac"
    }

    fun setDefaultPrinter(macAddress: String) {
        require(BluetoothDevice.checkBluetoothAddress(macAddress)) { "Adresse Bluetooth invalide" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_MAC, macAddress.uppercase())
            .apply()
    }

    fun getDefaultPrinterMac(): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MAC, null)

    @SuppressLint("MissingPermission")
    fun pairedPrinters(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices.sortedBy { it.name ?: it.address }
    }

    /** Print only after the sale is committed locally. Returns a user-safe error message on failure. */
    @SuppressLint("MissingPermission")
    fun printSale(sale: Sale, items: List<SaleItem>): Result<Unit> {
        if (!hasBluetoothPermission()) return Result.failure(IllegalStateException("Autorisation Bluetooth requise"))
        val mac = getDefaultPrinterMac()
            ?: return Result.failure(IllegalStateException("Aucune imprimante par défaut configurée"))
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return Result.failure(IllegalStateException("Bluetooth indisponible sur cet appareil"))
        if (!adapter.isEnabled) return Result.failure(IllegalStateException("Bluetooth désactivé"))

        val device = try { adapter.getRemoteDevice(mac) } catch (_: IllegalArgumentException) {
            return Result.failure(IllegalStateException("Imprimante Bluetooth introuvable"))
        }

        return try {
            adapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.use {
                it.connect()
                it.outputStream.write(buildReceipt(sale, items).toByteArray(Charset.forName("CP437")))
                it.outputStream.flush()
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("Impossible d'imprimer : ${e.message ?: "connexion refusée"}", e))
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun buildReceipt(sale: Sale, items: List<SaleItem>): String {
        val sb = StringBuilder()
        sb.append("\u001B\u0040") // initialize
        sb.append("\u001B\u0061\u0001") // center
        sb.append("SIRA\n")
        sb.append("RECU DE VENTE\n")
        sb.append("\u001B\u0061\u0000") // left
        sb.append("--------------------------------\n")
        sb.append("Ref: ${sale.reference}\n")
        sb.append("Client: ${sale.customerName}\n")
        sb.append("Paiement: ${sale.paymentMethod.label}\n")
        sb.append("--------------------------------\n")
        items.forEach { item ->
            sb.append("${item.productName.take(24)}\n")
            sb.append("  ${item.quantity} x ${money(item.unitPrice)} = ${money(item.subtotal)} F\n")
        }
        sb.append("--------------------------------\n")
        sb.append("TOTAL: ${money(sale.totalAmount)} FCFA\n")
        sb.append("--------------------------------\n")
        sb.append("Merci pour votre achat !\n\n\n")
        sb.append("\u001D\u0056\u0000") // full cut where supported
        return sb.toString()
    }

    private fun money(value: Double): String = "%,.0f".format(java.util.Locale.US, value).replace(',', ' ')
}
