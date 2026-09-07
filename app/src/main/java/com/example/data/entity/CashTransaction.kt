package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CashOpType(val label: String, val isCredit: Boolean) {
    DEPOT("Dépôt Caisse", true),
    RETRAIT("Retrait Caisse", false),
    TRANSFERT_ENTRANT("Transfert Entrant", true),
    TRANSFERT_SORTANT("Transfert Sortant", false),
    DEPENSE("Dépense d'exploitation", false),
    VENTE_ENCAISSEMENT("Encaissement Vente", true),
    ACHAT_DECAISSEMENT("Décaissement Achat", false)
}

enum class CashChannel(val label: String) {
    ESPECES("Caisse Espèces (Tiroir)"),
    ORANGE_MONEY("Orange Money BF"),
    MOOV_MONEY("Moov Money BF"),
    CORIS_MONEY("Coris Money / Banque"),
    AUTRE("Autre")
}

@Entity(tableName = "cash_transactions")
data class CashTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: CashOpType,
    val channel: CashChannel,
    val amount: Double,
    val fee: Double = 0.0,
    val reference: String, // ex: TX-2026-001
    val beneficiaryOrPayer: String? = null,
    val phone: String? = null,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
