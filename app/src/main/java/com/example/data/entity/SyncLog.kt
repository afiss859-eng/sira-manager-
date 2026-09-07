package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncType: String,
    val status: String, // SUCCES, ERREUR, HORS_LIGNE
    val itemsCount: Int,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
