package com.nidus.cripto

data class UnspentOutput(
    val txid: String,
    val vout: Int,
    val valueSats: Long,
    val address: String
)