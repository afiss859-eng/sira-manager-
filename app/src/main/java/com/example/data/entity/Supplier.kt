package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val companyName: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val balanceOwed: Double = 0.0, // Montant dû au fournisseur
    val createdAt: Long = System.currentTimeMillis()
)
