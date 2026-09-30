package com.nidus.cripto

import android.content.Context
import android.content.SharedPreferences

object StorageUtils {
    private const val PREF_NAME = "nidus_wallet_prefs"

    private const val KEY_ENCRYPTED_SEED = "key_encrypted_seed"
    private const val KEY_ADDRESSES = "key_addresses"
    private const val KEY_PIN = "key_user_pin"
    private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    private const val KEY_IS_WALLET_CREATED = "key_is_wallet_created"
    private const val KEY_TEMP_MNEMONIC = "temp_mnemonic"
    private const val KEY_USER_MNEMONIC = "user_mnemonic"
    private const val KEY_IS_MAINNET = "is_mainnet"
    private const val KEY_LAST_BTC_PRICE = "last_btc_price"
    private const val KEY_TRANSACTIONS = "key_wallet_transactions"
    private const val KEY_TRANSACTIONS_TESTNET = "key_wallet_transactions_testnet"
    private const val KEY_LANGUAGE = "key_app_language"

    fun setLanguage(context: Context, languageCode: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, "es") ?: "es"
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun clearAllData(context: Context) {
        try {
            getPrefs(context).edit().clear().commit()
            val generalPrefs = context.getSharedPreferences("nidus_prefs", Context.MODE_PRIVATE)
            generalPrefs.edit().clear().commit()
            context.filesDir.deleteRecursively()
            context.cacheDir.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importWallet(context: Context, mnemonic: String, newPin: String, isMainnet: Boolean) {
        try {
            val prefs = getPrefs(context)

            prefs.edit()
                .remove(KEY_USER_MNEMONIC)
                .remove(KEY_ENCRYPTED_SEED)
                .remove(KEY_TEMP_MNEMONIC)
                .remove(KEY_PIN)
                .remove(KEY_ADDRESSES)
                .remove(KEY_TRANSACTIONS)
                .remove(KEY_TRANSACTIONS_TESTNET)
                .apply()

            saveMnemonic(context, mnemonic)
            savePin(context, newPin)
            setWalletCreated(context, true)
            setMainnet(context, isMainnet)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveTransactions(context: Context, transactions: List<WalletTransaction>) {
        try {
            val sortedAndUnique = transactions
                .sortedByDescending { it.timestamp }
                .distinctBy { it.txid }

            val jsonArray = org.json.JSONArray()
            for (tx in sortedAndUnique) {
                val obj = org.json.JSONObject().apply {
                    put("txid", tx.txid)
                    put("timestamp", tx.timestamp)
                    put("amountSats", tx.amountSats)
                    put("type", tx.type)
                    put("address", tx.address)
                    put("feeSats", tx.feeSats)
                    put("note", tx.note ?: "")
                    put("btcPriceUsd", tx.btcPriceUsd)
                }
                jsonArray.put(obj)
            }

            val key = if (isMainnet(context)) KEY_TRANSACTIONS else KEY_TRANSACTIONS_TESTNET
            getPrefs(context).edit().putString(key, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTransactions(context: Context): List<WalletTransaction> {
        val key = if (isMainnet(context)) KEY_TRANSACTIONS else KEY_TRANSACTIONS_TESTNET
        val raw = getPrefs(context).getString(key, null) ?: return emptyList()

        return try {
            val list = mutableListOf<WalletTransaction>()
            val jsonArray = org.json.JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WalletTransaction(
                        txid = obj.getString("txid"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        amountSats = obj.getLong("amountSats"),
                        type = obj.getString("type"),
                        address = obj.getString("address"),
                        feeSats = obj.optLong("feeSats", 0L),
                        note = obj.optString("note", ""),
                        btcPriceUsd = obj.optDouble("btcPriceUsd", 0.0)
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateTransactionNote(context: Context, txid: String, newNote: String): Boolean {
        try {
            val transactions = getTransactions(context).toMutableList()
            val index = transactions.indexOfFirst { it.txid == txid }

            if (index != -1) {
                val tx = transactions[index]
                transactions[index] = tx.copy(note = newNote)

                saveTransactions(context, transactions)
                return true
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun setWalletCreated(context: Context, created: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_WALLET_CREATED, created).apply()
    }

    fun isWalletCreated(context: Context): Boolean {
        val prefs = getPrefs(context)
        val isExplicitlyCreated = prefs.getBoolean(KEY_IS_WALLET_CREATED, false)
        val hasMnemonic = !getMnemonic(context).isNullOrEmpty()
        return isExplicitlyCreated || hasMnemonic
    }

    fun saveMnemonic(context: Context, mnemonic: String) {
        try {
            val encryptedMnemonic = SecurityUtils.encrypt(mnemonic.trim(), "NidusSeedSaltKey")
            getPrefs(context).edit().putString(KEY_USER_MNEMONIC, encryptedMnemonic).apply()
        } catch (e: Exception) {
            getPrefs(context).edit().putString(KEY_USER_MNEMONIC, mnemonic.trim()).apply()
        }
    }

    fun getMnemonic(context: Context): String? {
        val rawEncrypted = getPrefs(context).getString(KEY_USER_MNEMONIC, null)
            ?: getPrefs(context).getString(KEY_ENCRYPTED_SEED, null)
            ?: getPrefs(context).getString(KEY_TEMP_MNEMONIC, null)
            ?: return null

        val userPin = getSavedPin(context)

        if (!userPin.isNullOrEmpty()) {
            try {
                val decryptedWithPin = SecurityUtils.decrypt(rawEncrypted, userPin)
                if (decryptedWithPin.isNotBlank()) return decryptedWithPin
            } catch (_: Exception) {}
        }

        return try {
            SecurityUtils.decrypt(rawEncrypted, "NidusSeedSaltKey")
        } catch (e: Exception) {
            rawEncrypted
        }
    }

    fun savePin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_PIN, SecurityUtils.encrypt(pin, "NidusPinSaltKey")).apply()
    }

    fun getSavedPin(context: Context): String? {
        val encryptedPin = getPrefs(context).getString(KEY_PIN, null) ?: return null
        return try {
            SecurityUtils.decrypt(encryptedPin, "NidusPinSaltKey")
        } catch (e: Exception) {
            null
        }
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun saveEncryptedSeed(context: Context, encryptedSeed: String) {
        getPrefs(context).edit().putString(KEY_ENCRYPTED_SEED, encryptedSeed).apply()
    }

    fun getEncryptedSeed(context: Context): String? {
        return getPrefs(context).getString(KEY_ENCRYPTED_SEED, null)
    }

    fun saveAddresses(context: Context, addresses: List<String>) {
        val joinedStr = addresses.joinToString(",")
        getPrefs(context).edit().putString(KEY_ADDRESSES, joinedStr).apply()
    }

    fun getAddresses(context: Context): List<String> {
        val rawStr = getPrefs(context).getString(KEY_ADDRESSES, "") ?: ""
        return if (rawStr.isEmpty()) emptyList() else rawStr.split(",")
    }

    fun saveTempMnemonic(context: Context, mnemonic: String) {
        getPrefs(context).edit().putString(KEY_TEMP_MNEMONIC, mnemonic).apply()
    }

    fun getTempMnemonic(context: Context): String? {
        return getPrefs(context).getString(KEY_TEMP_MNEMONIC, null)
    }

    fun clearTempMnemonic(context: Context) {
        getPrefs(context).edit().remove(KEY_TEMP_MNEMONIC).apply()
    }

    fun isMainnet(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_MAINNET, true)
    }

    fun setMainnet(context: Context, isMainnet: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_MAINNET, isMainnet).apply()
    }

    fun saveLastBtcPrice(context: Context, price: String) {
        getPrefs(context).edit().putString(KEY_LAST_BTC_PRICE, price).apply()
    }

    fun getLastBtcPrice(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_BTC_PRICE, "0.00") ?: "0.00"
    }

    fun updatePinAndReencryptSeed(context: Context, newPin: String): Boolean {
        try {
            val currentMnemonic = getMnemonic(context)

            if (currentMnemonic.isNullOrBlank()) {
                return false
            }

            savePin(context, newPin)

            val encryptedMnemonic = SecurityUtils.encrypt(currentMnemonic.trim(), newPin)
            getPrefs(context).edit().putString(KEY_USER_MNEMONIC, encryptedMnemonic).apply()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}