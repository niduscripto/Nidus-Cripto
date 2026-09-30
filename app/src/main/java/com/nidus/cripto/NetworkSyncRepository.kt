package com.nidus.cripto

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object NetworkSyncRepository {
    private fun getStringRes(lang: String, key: String): String {
        return when (lang) {
            "en" -> when (key) {
                "sent" -> "Sent"
                "received" -> "Received"
                "network_error" -> "Network error"
                "rejected" -> "Rejected"
                else -> key
            }
            else -> when (key) {
                "sent" -> "Enviado"
                "received" -> "Recibido"
                "network_error" -> "Error de red"
                "rejected" -> "Rechazada"
                else -> key
            }
        }
    }

    private fun getBaseUrl(isMainnet: Boolean): String {
        return if (isMainnet) "https://blockstream.info/api/" else "https://blockstream.info/testnet/api/"
    }

    suspend fun syncBalance(context: Context, address: String, isMainnet: Boolean): Long = withContext(Dispatchers.IO) {
        if (address.isBlank()) return@withContext 0L
        val baseUrl = getBaseUrl(isMainnet)
        try {
            val url = URL("${baseUrl}address/$address")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "NidusCripto-AndroidApp")

            when (conn.responseCode) {
                200 -> {
                    val jsonStr = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(jsonStr)

                    val chainStats = json.getJSONObject("chain_stats")
                    val mempoolStats = json.getJSONObject("mempool_stats")

                    val chainBalance = chainStats.getLong("funded_txo_sum") - chainStats.getLong("spent_txo_sum")
                    val mempoolBalance = mempoolStats.getLong("funded_txo_sum") - mempoolStats.getLong("spent_txo_sum")
                    val totalBalance = chainBalance + mempoolBalance

                    saveCachedBalanceForAddress(context, address, totalBalance)
                    return@withContext totalBalance
                }
                else -> {
                    return@withContext getCachedBalanceForAddress(context, address)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext getCachedBalanceForAddress(context, address)
        }
    }

    private fun saveCachedBalanceForAddress(context: Context, address: String, balance: Long) {
        val prefs = context.getSharedPreferences("nidus_balance_cache", Context.MODE_PRIVATE)
        prefs.edit().putLong(address, balance).apply()
    }

    private fun getCachedBalanceForAddress(context: Context, address: String): Long {
        val prefs = context.getSharedPreferences("nidus_balance_cache", Context.MODE_PRIVATE)
        return prefs.getLong(address, 0L)
    }

    suspend fun fetchAndSyncWalletTransactions(
        context: Context,
        addresses: List<String>,
        isMainnet: Boolean,
        globalBtcPriceUsd: Double
    ) {
        withContext(Dispatchers.IO) {
            val currentLanguage = StorageUtils.getLanguage(context)
            val baseUrl = getBaseUrl(isMainnet)
            val existingTxs = StorageUtils.getTransactions(context).toMutableList()

            val existingNotesMap = existingTxs.associate { it.txid to it.note }
            val existingPriceMap = existingTxs.associate { it.txid to it.btcPriceUsd }
            val existingFeeMap = existingTxs.associate { it.txid to it.feeSats }
            val existingTxids = existingTxs.map { it.txid }.toSet()

            var dataChanged = false
            val fetchedMap = mutableMapOf<String, WalletTransaction>()

            for (tx in existingTxs) {
                fetchedMap[tx.txid] = tx
            }

            for (addr in addresses) {
                try {
                    val url = URL("${baseUrl}address/$addr/txs")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.setRequestProperty("User-Agent", "NidusCripto-AndroidApp")

                    if (conn.responseCode == 200) {
                        val jsonStr = conn.inputStream.bufferedReader().readText()
                        val jsonArray = JSONArray(jsonStr)

                        for (i in 0 until jsonArray.length()) {
                            val txObj = jsonArray.getJSONObject(i)
                            val txid = txObj.getString("txid")

                            val vinArray = txObj.getJSONArray("vin")
                            val voutArray = txObj.getJSONArray("vout")

                            var sentSats = 0L
                            var receivedSats = 0L
                            val networkFee = txObj.optLong("fee", 0L)
                            val feeSats = if (networkFee > 0L) networkFee else (existingFeeMap[txid] ?: 0L)

                            var isOutgoing = false
                            for (v in 0 until vinArray.length()) {
                                val vin = vinArray.getJSONObject(v)
                                if (vin.has("prevout")) {
                                    val prevout = vin.getJSONObject("prevout")
                                    val prevAddr = prevout.optString("scriptpubkey_address", "")
                                    if (addresses.contains(prevAddr)) {
                                        isOutgoing = true
                                        sentSats += prevout.optLong("value", 0L)
                                    }
                                }
                            }

                            for (v in 0 until voutArray.length()) {
                                val vout = voutArray.getJSONObject(v)
                                val outAddr = vout.optString("scriptpubkey_address", "")
                                if (addresses.contains(outAddr)) {
                                    receivedSats += vout.optLong("value", 0L)
                                }
                            }

                            val statusObj = txObj.optJSONObject("status")
                            val blockTime = statusObj?.optLong("block_time", System.currentTimeMillis() / 1000) ?: (System.currentTimeMillis() / 1000)
                            val timestamp = blockTime * 1000L

                            val txType: String
                            val finalAmountSats: Long

                            if (isOutgoing) {
                                txType = getStringRes(currentLanguage, "sent")
                                finalAmountSats = if (sentSats > receivedSats) sentSats - receivedSats else receivedSats
                            } else {
                                txType = getStringRes(currentLanguage, "received")
                                finalAmountSats = receivedSats
                            }

                            val preservedNote = existingNotesMap[txid] ?: ""

                            val assignedPrice = existingPriceMap[txid] ?: if (globalBtcPriceUsd > 0.0) globalBtcPriceUsd else 0.0

                            val newTx = WalletTransaction(
                                txid = txid,
                                timestamp = timestamp,
                                amountSats = finalAmountSats,
                                type = txType,
                                address = addr,
                                feeSats = feeSats,
                                note = preservedNote,
                                btcPriceUsd = assignedPrice
                            )

                            if (!existingTxids.contains(txid) || !fetchedMap.containsKey(txid)) {
                                dataChanged = true

                                if (!isOutgoing) {
                                    val btcAmount = finalAmountSats / 100_000_000.0
                                    val amountBtcStr = String.format(Locale.US, "%.8f", btcAmount)
                                    val usdValue = btcAmount * globalBtcPriceUsd
                                    val usdValueStr = String.format(Locale.US, "%.2f", usdValue)

                                    NotificationHelper.showReceivedTransactionNotification(
                                        context = context,
                                        txid = txid,
                                        amountBtc = amountBtcStr,
                                        usdValue = usdValueStr
                                    )
                                }
                            }
                            fetchedMap[txid] = newTx
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (dataChanged || fetchedMap.size != existingTxs.size) {
                val finalSortedList = fetchedMap.values.sortedByDescending { it.timestamp }
                StorageUtils.saveTransactions(context, finalSortedList)
            }
        }
    }

    suspend fun broadcastTransaction(context: Context, txHex: String, isMainnet: Boolean): Result<String> = withContext(Dispatchers.IO) {
        val currentLanguage = StorageUtils.getLanguage(context)
        val baseUrl = getBaseUrl(isMainnet)
        try {
            val url = URL("${baseUrl}tx")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "text/plain")
            conn.setRequestProperty("User-Agent", "NidusCripto-AndroidApp")

            conn.outputStream.use { os ->
                os.write(txHex.toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode == 200) {
                val txid = conn.inputStream.bufferedReader().readText().trim()
                return@withContext Result.success(txid)
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.readText() ?: getStringRes(currentLanguage, "network_error")
                return@withContext Result.failure(Exception("${getStringRes(currentLanguage, "rejected")}: $errorMsg"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun getRecommendedFees(isMainnet: Boolean): Int = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl(isMainnet)
        try {
            val url = URL("${baseUrl}fee-estimates")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.setRequestProperty("User-Agent", "NidusCripto-AndroidApp")

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(jsonStr)
                return@withContext json.optDouble("2", 10.0).toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext 10
    }

    suspend fun getBalance(isMainnet: Boolean, address: String): Long = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl(isMainnet)
        try {
            val url = URL("${baseUrl}address/$address")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "NidusCripto-AndroidApp")

            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val chainStats = json.getJSONObject("chain_stats")
                val mempoolStats = json.getJSONObject("mempool_stats")
                val chain = chainStats.getLong("funded_txo_sum") - chainStats.getLong("spent_txo_sum")
                val mempool = mempoolStats.getLong("funded_txo_sum") - mempoolStats.getLong("spent_txo_sum")
                return@withContext chain + mempool
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext 0L
    }

    private fun saveCachedBalance(context: Context, balance: Long, isMainnet: Boolean) {
        val prefs = context.getSharedPreferences("nidus_cache", Context.MODE_PRIVATE)
        val key = if (isMainnet) "balance_mainnet" else "balance_testnet"
        prefs.edit().putLong(key, balance).apply()
    }

    fun getCachedBalance(context: Context, isMainnet: Boolean): Long {
        val prefs = context.getSharedPreferences("nidus_cache", Context.MODE_PRIVATE)
        val key = if (isMainnet) "balance_mainnet" else "balance_testnet"
        return prefs.getLong(key, 0L)
    }
}