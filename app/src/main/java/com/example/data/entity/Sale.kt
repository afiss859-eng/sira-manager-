package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentMethod(val label: String) {
    ESPECES("Espèces"),
    ORANGE_MONEY("Orange Money BF"),
    MOOV_MONEY("Moov Money BF"),
    CORIS_MONEY("Coris Money / Banque"),
    A_CREDIT("À crédit")
}

enum class SaleStatus(val label: String) {
    PAYE("Payé"),
    EN_ATTENTE("En attente"),
    ANNULE("Annulé")
}

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String, // ex: VTE-2026-001
    val customerId: Long? = null,
    val customerName: String,
    val totalAmount: Double, // FCFA
    val profitAmount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.ESPECES,
    val status: SaleStatus = SaleStatus.PAYE,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double = 0.0,
    val subtotal: Double = quantity * unitPrice
)
