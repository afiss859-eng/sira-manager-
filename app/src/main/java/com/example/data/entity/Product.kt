package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val quantity: Int,
    val purchasePrice: Double, // Prix d'achat en FCFA
    val salePrice: Double,     // Prix de vente en FCFA
    val photoUri: String? = null,
    val minAlertStock: Int = 5,
    val barcode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = quantity <= minAlertStock

    val potentialProfit: Double
        get() = (salePrice - purchasePrice) * quantity
}
