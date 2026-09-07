package com.example.printer

import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.PaymentMethod
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SiraPrintQueueTest {
    @Test
    fun enqueueAndRestoreReceiptKeepsSaleAndItems() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("sira_print_queue", android.content.Context.MODE_PRIVATE).edit().clear().commit()

        val queue = SiraPrintQueue(context)
        val sale = Sale(
            id = 42L,
            reference = "VTE-TEST-042",
            customerId = 7L,
            customerName = "Client test",
            totalAmount = 2500.0,
            profitAmount = 500.0,
            paymentMethod = PaymentMethod.ESPECES,
            status = SaleStatus.PAYE,
            timestamp = 123456789L,
            notes = "Ticket test"
        )
        val item = SaleItem(
            id = 8L,
            saleId = 42L,
            productId = 3L,
            productName = "Riz 5kg",
            quantity = 2,
            unitPrice = 1250.0,
            unitCost = 900.0,
            subtotal = 2500.0
        )

        queue.enqueue(sale, listOf(item))
        assertEquals(1, queue.pendingCount())

        val restored = queue.takeAll().single()
        assertEquals(sale.reference, restored.first.reference)
        assertEquals(sale.totalAmount, restored.first.totalAmount, 0.0)
        assertEquals(sale.paymentMethod, restored.first.paymentMethod)
        assertEquals(1, restored.second.size)
        assertEquals("Riz 5kg", restored.second.first().productName)
        assertEquals(2, restored.second.first().quantity)
        assertEquals(2500.0, restored.second.first().subtotal, 0.0)
        assertNotNull(restored.second.first())

        queue.replace(emptyList())
        assertEquals(0, queue.pendingCount())
    }
}
