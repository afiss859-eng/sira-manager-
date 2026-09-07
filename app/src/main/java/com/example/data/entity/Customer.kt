package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class IdDocumentType(val label: String) {
    CNIB("CNIB (Burkina Faso)"),
    PASSEPORT("Passeport"),
    PERMIS("Permis de conduire"),
    CARTE_CONSULAIRE("Carte Consulaire"),
    AUTRE("Autre document officiel")
}

enum class KycStatus(val label: String) {
    VERIFIE("Vérifié (Conforme KYC)"),
    EN_ATTENTE("En attente de vérification"),
    NON_FOURNI("Non renseigné")
}

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val idDocumentType: IdDocumentType = IdDocumentType.CNIB,
    val idDocumentNumber: String? = null,
    val idDocumentPhotoUri: String? = null,
    val kycStatus: KycStatus = KycStatus.NON_FOURNI,
    val kycNotes: String? = null,
    val creditBalance: Double = 0.0, // Montant dû au commerçant
    val createdAt: Long = System.currentTimeMillis()
)
