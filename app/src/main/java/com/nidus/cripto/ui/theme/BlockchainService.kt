package com.nidus.cripto.ui

import com.nidus.cripto.UnspentOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object BlockchainService {

    suspend fun fetchUtxosForAddress(address: String, isMainnet: Boolean = false): List<UnspentOutput> {
        return withContext(Dispatchers.IO) {
            val utxoList = mutableListOf<UnspentOutput>()
            try {
                val baseUrl = if (isMainnet) {
                    "https://blockstream.info/api/address/$address/utxo"
                } else {
                    "https://blockstream.info/testnet/api/address/$address/utxo"
                }

                val url = java.net.URL(baseUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "NidusCripto-AndroidApp")

                if (connection.responseCode == 200) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(responseString)

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val txid = obj.getString("txid")
                        val vout = obj.getInt("vout")
                        val value = obj.getLong("value")

                        utxoList.add(
                            UnspentOutput(
                                txid = txid,
                                vout = vout,
                                valueSats = value,
                                address = address
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            utxoList
        }
    }
}