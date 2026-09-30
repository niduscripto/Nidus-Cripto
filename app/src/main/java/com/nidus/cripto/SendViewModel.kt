package com.nidus.cripto

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nidus.cripto.ui.BlockchainService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bitcoinj.core.Coin
import org.bitcoinj.core.DumpedPrivateKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionWitness
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.ScriptBuilder

sealed class SendUiState {
    object Idle : SendUiState()
    data class Loading(val message: String) : SendUiState()
    data class Success(val txid: String, val txHex: String) : SendUiState()
    data class Error(val message: String, val txHex: String? = null) : SendUiState()
}

class SendViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<SendUiState>(SendUiState.Idle)
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    private val _userUtxos = MutableStateFlow<List<UnspentOutput>>(emptyList())
    val userUtxos: StateFlow<List<UnspentOutput>> = _userUtxos.asStateFlow()

    private val _isFetchingUtxos = MutableStateFlow(false)
    val isFetchingUtxos: StateFlow<Boolean> = _isFetchingUtxos.asStateFlow()

    private fun getStringRes(lang: String, key: String, vararg args: Any): String {
        return when (lang) {
            "en" -> when (key) {
                "processing" -> "Processing transaction..."
                "signing_error" -> "Error signing transaction."
                "broadcasting" -> "Broadcasting transaction to the network..."
                "multiple_addresses" -> "Multiple Addresses (${args.firstOrNull() ?: 0})"
                "sent" -> "Sent"
                "network_rejected" -> "The network rejected the transaction."
                "unexpected_error" -> "Unexpected error"
                else -> key
            }
            else -> when (key) {
                "processing" -> "Procesando la transacción...."
                "signing_error" -> "Error al firmar la transacción."
                "broadcasting" -> "Difundiendo transacción a la red..."
                "multiple_addresses" -> "Múltiples Direcciones (${args.firstOrNull() ?: 0})"
                "sent" -> "Enviado"
                "network_rejected" -> "La red rechazó la transacción."
                "unexpected_error" -> "Error inesperado"
                else -> key
            }
        }
    }

    fun fetchUtxosForAddress(address: String, isMainnet: Boolean = false) {
        if (address.isBlank()) return

        viewModelScope.launch {
            _isFetchingUtxos.value = true
            try {
                val currentUtxos = _userUtxos.value.filter { it.address != address }
                val newUtxos = BlockchainService.fetchUtxosForAddress(address, isMainnet)

                val updatedUtxos = currentUtxos + newUtxos

                val oldTotalSats = _userUtxos.value.sumOf { it.valueSats }
                val newTotalSats = updatedUtxos.sumOf { it.valueSats }

                if (oldTotalSats > 0 && newTotalSats > oldTotalSats) {
                    SoundEffectsHelper.playReceiveTone(getApplication())
                }

                _userUtxos.value = updatedUtxos
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isFetchingUtxos.value = false
            }
        }
    }

    fun processAndSendTransaction(
        recipients: List<Recipient>,
        notes: List<String>,
        utxos: List<UnspentOutput>,
        seedMnemonic: String,
        fallbackWif: String,
        isMainnet: Boolean,
        btcPriceUsd: Double,
        changeAddress: String,
        feeRateSatPerVb: Int
    ) {
        viewModelScope.launch {
            val cleanRecipients = recipients.map {
                it.copy(address = it.address.trim())
            }
            val cleanChangeAddress = changeAddress.trim()
            val context = getApplication<Application>()
            val currentLanguage = StorageUtils.getLanguage(context)

            _uiState.value = SendUiState.Loading(getStringRes(currentLanguage, "processing"))

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val resolvedPrivateKeyWifs = mutableListOf<String>()

                    if (seedMnemonic.isNotBlank()) {
                        val savedAddresses = StorageUtils.getAddresses(context)
                        val addressToIndexMap = mutableMapOf<String, Int>()
                        savedAddresses.forEachIndexed { index, addr ->
                            addressToIndexMap[addr.trim().lowercase()] = index
                        }

                        for (utxo in utxos) {
                            val utxoAddrLower = utxo.address.trim().lowercase()
                            val matchedIndex = addressToIndexMap[utxoAddrLower]

                            val matchedWif = if (matchedIndex != null) {
                                RealWalletManager.derivePrivateKeyFromMnemonic(
                                    seedMnemonic,
                                    addressIndex = matchedIndex,
                                    isMainnet = isMainnet
                                )
                            } else {
                                fallbackWif
                            }

                            resolvedPrivateKeyWifs.add(matchedWif.ifBlank { fallbackWif })
                        }
                    } else {
                        for (ignored in utxos) {
                            resolvedPrivateKeyWifs.add(fallbackWif)
                        }
                    }

                    val params = if (isMainnet) MainNetParams.get() else TestNet3Params.get()

                    if (utxos.size != resolvedPrivateKeyWifs.size) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _uiState.value = SendUiState.Error(getStringRes(currentLanguage, "signing_error"), null)
                        }
                        return@withContext
                    }

                    val tx = Transaction(params)
                    var totalOutputsSats = 0L
                    for (recipient in cleanRecipients) {
                        val address = org.bitcoinj.core.Address.fromString(params, recipient.address)
                        val value = Coin.valueOf(recipient.amountSats)
                        tx.addOutput(value, address)
                        totalOutputsSats += recipient.amountSats
                    }

                    val appFeeSats = if (btcPriceUsd > 0.0) {
                        val btcAmountForFee = TransactionManager.APP_FEE_USD / btcPriceUsd
                        (btcAmountForFee * 100_000_000).toLong()
                    } else {
                        1000L
                    }
                    val finalAppFeeSats = if (appFeeSats < TransactionManager.DUST_LIMIT_SATS) TransactionManager.DUST_LIMIT_SATS else appFeeSats
                    val targetAppFeeAddress = if (isMainnet) TransactionManager.APP_FEE_ADDRESS_MAINNET else TransactionManager.APP_FEE_ADDRESS_TESTNET

                    tx.addOutput(Coin.valueOf(finalAppFeeSats), org.bitcoinj.core.Address.fromString(params, targetAppFeeAddress))
                    totalOutputsSats += finalAppFeeSats

                    var totalInputSats = 0L
                    for (utxo in utxos) {
                        val txHash = Sha256Hash.wrap(utxo.txid)
                        val addressObj = org.bitcoinj.core.Address.fromString(params, utxo.address)
                        val scriptPubKey = ScriptBuilder.createOutputScript(addressObj)

                        tx.addInput(txHash, utxo.vout.toLong(), scriptPubKey)
                        totalInputSats += utxo.valueSats
                    }

                    val estimatedVBytes = 11 + (utxos.size * 68) + (tx.outputs.size * 31)
                    val realMiningFeeSats = estimatedVBytes * feeRateSatPerVb.toLong()

                    val changeSats = totalInputSats - (totalOutputsSats + realMiningFeeSats)
                    if (changeSats > TransactionManager.DUST_LIMIT_SATS) {
                        val changeOutAddress = org.bitcoinj.core.Address.fromString(params, cleanChangeAddress)
                        tx.addOutput(Coin.valueOf(changeSats), changeOutAddress)
                    }

                    for (i in 0 until tx.inputs.size) {
                        val input = tx.getInput(i.toLong())
                        val correspondingUtxo = utxos[i]
                        val currentWif = resolvedPrivateKeyWifs[i].trim()

                        val ecKey = if (currentWif.length == 64 && currentWif.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                            val privateKeyBytes = org.bitcoinj.core.Utils.HEX.decode(currentWif)
                            ECKey.fromPrivate(privateKeyBytes)
                        } else {
                            val dumpedPrivateKey = DumpedPrivateKey.fromBase58(params, currentWif)
                            dumpedPrivateKey.key
                        }

                        val p2pkhAddress = org.bitcoinj.core.Address.fromKey(params, ecKey, org.bitcoinj.script.Script.ScriptType.P2PKH)
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

                    val txHex = org.bitcoinj.core.Utils.HEX.encode(tx.bitcoinSerialize())

                    val actualChangeReturned = if (changeSats > TransactionManager.DUST_LIMIT_SATS) changeSats else 0L
                    val exactTotalSentFromWallet = totalInputSats - actualChangeReturned
                    val exactMiningFeeSats = totalInputSats - totalOutputsSats - actualChangeReturned

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _uiState.value = SendUiState.Loading(getStringRes(currentLanguage, "broadcasting"))
                    }

                    val broadcastResult = NetworkSyncRepository.broadcastTransaction(context, txHex, isMainnet)

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (broadcastResult.isSuccess) {
                            val txid = broadcastResult.getOrThrow()
                            SoundEffectsHelper.playSendTone(context)

                            val existingTransactions = StorageUtils.getTransactions(context).toMutableList()
                            val combinedNote = notes.filter { it.isNotBlank() }.joinToString(" | ")

                            val destinationAddress = if (cleanRecipients.size > 1) {
                                getStringRes(currentLanguage, "multiple_addresses", cleanRecipients.size)
                            } else {
                                cleanRecipients.firstOrNull()?.address ?: ""
                            }

                            val newSentTx = WalletTransaction(
                                txid = txid,
                                timestamp = System.currentTimeMillis(),
                                amountSats = exactTotalSentFromWallet,
                                type = getStringRes(currentLanguage, "sent"),
                                address = destinationAddress,
                                feeSats = exactMiningFeeSats,
                                note = combinedNote,
                                btcPriceUsd = btcPriceUsd
                            )

                            existingTransactions.add(0, newSentTx)
                            StorageUtils.saveTransactions(context, existingTransactions)

                            _uiState.value = SendUiState.Success(txid, txHex)
                        } else {
                            val errorMsg = broadcastResult.exceptionOrNull()?.message ?: getStringRes(currentLanguage, "network_rejected")
                            _uiState.value = SendUiState.Error(errorMsg, txHex)
                        }
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _uiState.value = SendUiState.Error(e.message ?: getStringRes(currentLanguage, "unexpected_error"), null)
                    }
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = SendUiState.Idle
    }
}