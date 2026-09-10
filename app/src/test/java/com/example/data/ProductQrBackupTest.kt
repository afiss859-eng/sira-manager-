package com.example.data

import com.example.data.entity.ProductQrCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductQrBackupTest {
    @Test fun csvRoundTripPreservesSpecialCharacters() {
        val source = listOf(ProductQrCode(productId = 7, productName = "Jus, mangue", price = 1250.0, currency = "XOF", payload = "615\"ABC", variant = "1 L, Rouge", createdAt = 123L))
        val parsed = ProductQrBackup.parseCsv(ProductQrBackup.toCsv(source))
        assertEquals(1, parsed.size)
        assertEquals("Jus, mangue", parsed[0].name)
        assertEquals(1250.0, parsed[0].price, 0.0)
        assertEquals("615\"ABC", parsed[0].payload)
        assertEquals("1 L, Rouge", parsed[0].variant)
    }

    @Test fun invalidRowsAreIgnored() {
        val csv = ProductQrBackup.HEADER + "\n\"\",\"\",\"0\",\"XOF\",\"\",\"\",\"0\"\n"
        assertTrue(ProductQrBackup.parseCsv(csv).isEmpty())
    }
}
