package com.sira.orangemoney

data class OrangeTransaction(
    val reference: String,
    val amount: Long,
    val fee: Long,
    val timestamp: Long,
    val type: String,
    val counterparty: String,
    val customerPhone: String = "",
    val recipientPhone: String = "",
    val idDocument: String = "",
    val balanceBefore: Long = 0,
    val balanceAfter: Long = 0,
    val status: String = "VALIDE",
    val note: String = ""
)
