package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_qr_codes")
data class ProductQrCode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val price: Double,
    val currency: String = "XOF",
    val payload: String,
    val variant: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
