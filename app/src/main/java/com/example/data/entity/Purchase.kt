package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String, // ex: ACH-2026-001
    val supplierId: Long? = null,
    val supplierName: String,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.ESPECES,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

@Entity(tableName = "purchase_items")
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val subtotal: Double = quantity * unitCost
)
