package com.example.printer

import android.content.Context
import com.example.data.entity.PaymentMethod
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small durable local queue for receipts that could not be printed.
 * The queue is deliberately independent from the sales database so printing failures
 * never alter the financial record.
 */
class SiraPrintQueue(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun enqueue(sale: Sale, items: List<SaleItem>) {
        val queue = readQueue()
        queue.put(encode(sale, items))
        prefs.edit().putString(KEY_QUEUE, queue.toString()).apply()
    }

    @Synchronized
    fun pendingCount(): Int = readQueue().length()

    @Synchronized
    fun takeAll(): List<Pair<Sale, List<SaleItem>>> {
        val source = readQueue()
        val result = mutableListOf<Pair<Sale, List<SaleItem>>>()
        for (i in 0 until source.length()) {
            decode(source.optJSONObject(i))?.let(result::add)
        }
        return result
    }

    @Synchronized
    fun replace(items: List<Pair<Sale, List<SaleItem>>>) {
        val queue = JSONArray()
        items.forEach { (sale, saleItems) -> queue.put(encode(sale, saleItems)) }
        prefs.edit().putString(KEY_QUEUE, queue.toString()).apply()
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_QUEUE).apply()
    }

    private fun readQueue(): JSONArray {
        val raw = prefs.getString(KEY_QUEUE, null) ?: return JSONArray()
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }

    private fun encode(sale: Sale, items: List<SaleItem>): JSONObject = JSONObject().apply {
        put("sale", JSONObject().apply {
            put("id", sale.id)
            put("reference", sale.reference)
            put("customerId", sale.customerId ?: JSONObject.NULL)
            put("customerName", sale.customerName)
            put("totalAmount", sale.totalAmount)
            put("profitAmount", sale.profitAmount)
            put("paymentMethod", sale.paymentMethod.name)
            put("status", sale.status.name)
            put("timestamp", sale.timestamp)
            put("notes", sale.notes ?: JSONObject.NULL)
        })
        put("items", JSONArray().apply {
            items.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("saleId", item.saleId)
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("unitCost", item.unitCost)
                    put("subtotal", item.subtotal)
                })
            }
        })
    }

    private fun decode(root: JSONObject?): Pair<Sale, List<SaleItem>>? {
        if (root == null) return null
        return try {
            val saleJson = root.getJSONObject("sale")
            val sale = Sale(
                id = saleJson.optLong("id"),
                reference = saleJson.getString("reference"),
                customerId = if (saleJson.isNull("customerId")) null else saleJson.optLong("customerId"),
                customerName = saleJson.optString("customerName"),
                totalAmount = saleJson.optDouble("totalAmount"),
                profitAmount = saleJson.optDouble("profitAmount"),
                paymentMethod = runCatching { PaymentMethod.valueOf(saleJson.optString("paymentMethod")) }.getOrDefault(PaymentMethod.ESPECES),
                status = runCatching { SaleStatus.valueOf(saleJson.optString("status")) }.getOrDefault(SaleStatus.PAYE),
                timestamp = saleJson.optLong("timestamp"),
                notes = if (saleJson.isNull("notes")) null else saleJson.optString("notes")
            )
            val array = root.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    add(SaleItem(
                        id = item.optLong("id"),
                        saleId = item.optLong("saleId"),
                        productId = item.optLong("productId"),
                        productName = item.optString("productName"),
                        quantity = item.optInt("quantity"),
                        unitPrice = item.optDouble("unitPrice"),
                        unitCost = item.optDouble("unitCost"),
                        subtotal = item.optDouble("subtotal")
                    ))
                }
            }
            sale to items
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS = "sira_print_queue"
        private const val KEY_QUEUE = "pending_receipts"
    }
}
