package com.nidus.cripto

import org.bitcoinj.core.*
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.ScriptBuilder

data class Recipient(
    val address: String,
    val amountSats: Long
)

data class TransactionEstimate(
    val totalAmountSats: Long,
    val networkFeeSats: Long,
    val appFeeSats: Long,
    val totalRequiredSats: Long,
    val vBytes: Int,
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class WalletTransaction(
    val txid: String,
    val timestamp: Long,
    val amountSats: Long,
    val type: String,
    val address: String,
    val feeSats: Long,
    val note: String,
    val btcPriceUsd: Double = 0.0
)

object TransactionManager {

    const val DUST_LIMIT_SATS = 546L

    const val APP_FEE_ADDRESS_MAINNET = BuildConfig.APP_FEE_ADDRESS_MAINNET
    const val APP_FEE_ADDRESS_TESTNET = BuildConfig.APP_FEE_ADDRESS_TESTNET

    const val APP_FEE_USD = 0.25

    private fun getStringRes(lang: String, key: String, vararg args: Any): String {
        return when (lang) {
            "en" -> when (key) {
                "err_no_recipient" -> "You must add at least one recipient."
                "err_address_empty" -> "Address #${args.firstOrNull() ?: 0} is empty."
                "err_invalid_network" -> "Address #${args.firstOrNull() ?: 0} does not correspond to the ${args.getOrNull(1) ?: ""} network."
                "err_dust_limit" -> "Amount #${args.firstOrNull() ?: 0} is below the dust limit (${args.getOrNull(1) ?: 0} Sats)."
                "err_config_fee" -> "Configuration error: App Fee address not configured for the network."
                "err_insufficient_balance" -> "Insufficient balance. You are missing ${args.firstOrNull() ?: 0} Satoshis to cover amounts + fees."
                "err_private_key_mismatch" -> "Internal error: The number of private keys does not match the UTXOs."
                "err_insufficient_fees" -> "Insufficient funds to cover fees."
                else -> key
            }
            else -> when (key) {
                "err_no_recipient" -> "Debe agregar al menos un destinatario."
                "err_address_empty" -> "La dirección #${args.firstOrNull() ?: 0} está vacía."
                "err_invalid_network" -> "La dirección #${args.firstOrNull() ?: 0} no corresponde a la red ${args.getOrNull(1) ?: ""}."
                "err_dust_limit" -> "El monto #${args.firstOrNull() ?: 0} es inferior al límite de polvo (${args.getOrNull(1) ?: 0} Sats)."
                "err_config_fee" -> "Error de configuración: Dirección de App Fee no configurada para la red."
                "err_insufficient_balance" -> "Saldo insuficiente. Le faltan ${args.firstOrNull() ?: 0} Satoshis para cubrir montos + comisiones."
                "err_private_key_mismatch" -> "Error interno: La cantidad de claves privadas no coincide con los UTXOs."
                "err_insufficient_fees" -> "Fondos insuficientes para cubrir comisiones."
                else -> key
            }
        }
    }

    fun estimateTransaction(
        currentLanguage: String,
        recipients: List<Recipient>,
        feeRateSatPerVb: Int,
        availableBalanceSats: Long,
        isMainnet: Boolean,
        btcPriceUsd: Double,
        estimatedInputsCount: Int = 1
    ): TransactionEstimate {

        if (recipients.isEmpty()) {
            return buildErrorEstimate(getStringRes(currentLanguage, "err_no_recipient"))
        }

        var totalAmountSats = 0L

        for ((index, recipient) in recipients.withIndex()) {
            val address = recipient.address.trim()
            val amount = recipient.amountSats

            if (address.isEmpty()) {
                return buildErrorEstimate(getStringRes(currentLanguage, "err_address_empty", index + 1))
            }

            if (!isValidAddressForNetwork(address, isMainnet)) {
                val expectedNet = if (isMainnet) "Mainnet (bc1q...)" else "Testnet (tb1q...)"
                return buildErrorEstimate(getStringRes(currentLanguage, "err_invalid_network", index + 1, expectedNet))
            }

            if (amount < DUST_LIMIT_SATS) {
                return buildErrorEstimate(getStringRes(currentLanguage, "err_dust_limit", index + 1, DUST_LIMIT_SATS))
            }

            totalAmountSats += amount
        }

        val appFeeSats = if (btcPriceUsd > 0.0) {
            val btcAmountForFee = APP_FEE_USD / btcPriceUsd
            (btcAmountForFee * 100_000_000).toLong()
        } else {
            1000L
        }

        val finalAppFeeSats = if (appFeeSats < DUST_LIMIT_SATS) DUST_LIMIT_SATS else appFeeSats
        val targetAppFeeAddress = if (isMainnet) APP_FEE_ADDRESS_MAINNET else APP_FEE_ADDRESS_TESTNET

        if (targetAppFeeAddress.isBlank() || targetAppFeeAddress.contains("xxxxxx")) {
            return buildErrorEstimate(getStringRes(currentLanguage, "err_config_fee"))
        }

        val hasAppFeeOutput = finalAppFeeSats >= DUST_LIMIT_SATS
        val outputsCount = recipients.size + (if (hasAppFeeOutput) 1 else 0) + 1
        val vBytes = 11 + (estimatedInputsCount * 68) + (outputsCount * 31)

        val networkFeeSats = vBytes * feeRateSatPerVb.toLong()
        val totalRequiredSats = totalAmountSats + networkFeeSats + finalAppFeeSats

        if (availableBalanceSats < totalRequiredSats) {
            val deficit = totalRequiredSats - availableBalanceSats
            return TransactionEstimate(
                totalAmountSats = totalAmountSats,
                networkFeeSats = networkFeeSats,
                appFeeSats = finalAppFeeSats,
                totalRequiredSats = totalRequiredSats,
                vBytes = vBytes,
                isValid = false,
                errorMessage = getStringRes(currentLanguage, "err_insufficient_balance", deficit)
            )
        }

        return TransactionEstimate(
            totalAmountSats = totalAmountSats,
            networkFeeSats = networkFeeSats,
            appFeeSats = finalAppFeeSats,
            totalRequiredSats = totalRequiredSats,
            vBytes = vBytes,
            isValid = true
        )
    }

    fun createAndSignTransaction(
        currentLanguage: String,
        recipients: List<Recipient>,
        utxos: List<UnspentOutput>,
        privateKeyWifs: List<String>,
        isMainnet: Boolean,
        btcPriceUsd: Double,
        changeAddress: String,
        feeRateSatPerVb: Int
    ): Result<String> {
        return try {
            val params = if (isMainnet) MainNetParams.get() else TestNet3Params.get()

            if (utxos.size != privateKeyWifs.size) {
                return Result.failure(Exception(getStringRes(currentLanguage, "err_private_key_mismatch")))
            }

            val tx = Transaction(params)

            var totalOutputsSats = 0L
            for ((index, recipient) in recipients.withIndex()) {
                val address = Address.fromString(params, recipient.address)
                val value = Coin.valueOf(recipient.amountSats)
                tx.addOutput(value, address)
                totalOutputsSats += recipient.amountSats
            }

            val appFeeSats = if (btcPriceUsd > 0.0) {
                val btcAmountForFee = APP_FEE_USD / btcPriceUsd
                (btcAmountForFee * 100_000_000).toLong()
            } else {
                1000L
            }
            val finalAppFeeSats = if (appFeeSats < DUST_LIMIT_SATS) DUST_LIMIT_SATS else appFeeSats
            val targetAppFeeAddress = if (isMainnet) APP_FEE_ADDRESS_MAINNET else APP_FEE_ADDRESS_TESTNET

            tx.addOutput(Coin.valueOf(finalAppFeeSats), Address.fromString(params, targetAppFeeAddress))
            totalOutputsSats += finalAppFeeSats

            var totalInputSats = 0L
            for (utxo in utxos) {
                val txHash = Sha256Hash.wrap(utxo.txid)
                val addressObj = Address.fromString(params, utxo.address)
                val scriptPubKey = ScriptBuilder.createOutputScript(addressObj)

                tx.addInput(txHash, utxo.vout.toLong(), scriptPubKey)
                totalInputSats += utxo.valueSats
            }

            val estimatedVBytes = 11 + (utxos.size * 68) + (tx.outputs.size * 31)
            val networkFeeSats = estimatedVBytes * feeRateSatPerVb.toLong()

            val changeSats = totalInputSats - (totalOutputsSats + networkFeeSats)
            if (changeSats > DUST_LIMIT_SATS) {
                val changeOutAddress = Address.fromString(params, changeAddress)
                tx.addOutput(Coin.valueOf(changeSats), changeOutAddress)
            } else if (changeSats < 0) {
                return Result.failure(Exception(getStringRes(currentLanguage, "err_insufficient_fees")))
            }

            for (i in 0 until tx.inputs.size) {
                val input = tx.getInput(i.toLong())
                val correspondingUtxo = utxos[i]
                val currentWif = privateKeyWifs[i].trim()

                val ecKey = if (currentWif.length == 64 && currentWif.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                    val privateKeyBytes = org.bitcoinj.core.Utils.HEX.decode(currentWif)
                    ECKey.fromPrivate(privateKeyBytes)
                } else {
                    val dumpedPrivateKey = DumpedPrivateKey.fromBase58(params, currentWif)
                    dumpedPrivateKey.key
                }

                val p2pkhAddress = Address.fromKey(params, ecKey, org.bitcoinj.script.Script.ScriptType.P2PKH)
                val scriptCode = ScriptBuilder.createOutputScript(p2pkhAddress)
                val utxoValue = Coin.valueOf(correspondingUtxo.valueSats)

                val signature = tx.calculateWitnessSignature(
                    i,
                    ecKey,
                    scriptCode,
                    utxoValue,
                    Transaction.SigHash.ALL,
                    false
                )

                val witness = TransactionWitness(2)
                witness.setPush(0, signature.encodeToBitcoin())
                witness.setPush(1, ecKey.pubKey)

                input.witness = witness
                input.scriptSig = ScriptBuilder().build()
            }

            val txHex = Utils.HEX.encode(tx.bitcoinSerialize())
            Result.success(txHex)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isValidAddressForNetwork(address: String, isMainnet: Boolean): Boolean {
        val cleanAddr = address.trim().lowercase()
        return if (isMainnet) {
            cleanAddr.startsWith("bc1q") || cleanAddr.startsWith("bc1p") || cleanAddr.startsWith("1") || cleanAddr.startsWith("3")
        } else {
            cleanAddr.startsWith("tb1q") || cleanAddr.startsWith("tb1p") || cleanAddr.startsWith("m") || cleanAddr.startsWith("n") || cleanAddr.startsWith("2")
        }
    }

    private fun buildErrorEstimate(message: String): TransactionEstimate {
        return TransactionEstimate(
            totalAmountSats = 0L,
            networkFeeSats = 0L,
            appFeeSats = 0L,
            totalRequiredSats = 0L,
            vBytes = 0,
            isValid = false,
            errorMessage = message
        )
    }
}