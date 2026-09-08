package com.sira.orangemoney

data class OrangeTransaction(
    val reference: String,
    val amount: Long,
    val fee: Long,
    val timestamp: Long,
    val type: String = "ENCAISSEMENT",
    val counterparty: String = ""
)
