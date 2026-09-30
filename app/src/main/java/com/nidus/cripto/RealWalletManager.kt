package com.nidus.cripto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.bitcoinj.core.Address
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import java.security.SecureRandom
import org.bitcoinj.crypto.MnemonicCode

object RealWalletManager {
    fun deriveRealSegWitAddress(
        context: Context,
        userPin: String,
        addressIndex: Int,
        isMainnet: Boolean
    ): String {
        return try {
            val rawMnemonic = StorageUtils.getMnemonic(context)
            if (rawMnemonic.isNullOrEmpty()) return ""

            val params: NetworkParameters = if (isMainnet) MainNetParams.get() else TestNet3Params.get()
            val seed = DeterministicSeed(rawMnemonic.trim().split("\\s+".toRegex()), null, "", 0L)
            val chain = DeterministicKeyChain.builder().seed(seed).build()

            val coinType = if (isMainnet) 0 else 1
            val path = listOf(
                ChildNumber(84, true),
                ChildNumber(coinType, true),
                ChildNumber(0, true),
                ChildNumber.ZERO,
                ChildNumber(addressIndex)
            )

            val key = chain.getKeyByPath(path, true)
            val address: Address = Address.fromKey(params, key, org.bitcoinj.script.Script.ScriptType.P2WPKH)
            address.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun deriveAddressFromMnemonic(
        mnemonic: String,
        addressIndex: Int,
        isMainnet: Boolean
    ): String {
        return try {
            if (mnemonic.isEmpty()) return ""
            val params: NetworkParameters = if (isMainnet) MainNetParams.get() else TestNet3Params.get()
            val seed = DeterministicSeed(mnemonic.trim().split("\\s+".toRegex()), null, "", 0L)
            val chain = DeterministicKeyChain.builder().seed(seed).build()

            val coinType = if (isMainnet) 0 else 1
            val path = listOf(
                ChildNumber(84, true),
                ChildNumber(coinType, true),
                ChildNumber(0, true),
                ChildNumber.ZERO,
                ChildNumber(addressIndex)
            )

            val key = chain.getKeyByPath(path, true)
            val address: Address = Address.fromKey(params, key, org.bitcoinj.script.Script.ScriptType.P2WPKH)
            address.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun derivePrivateKeyFromMnemonic(
        mnemonic: String,
        addressIndex: Int,
        isMainnet: Boolean
    ): String {
        return try {
            if (mnemonic.isEmpty()) return ""
            val params: NetworkParameters = if (isMainnet) MainNetParams.get() else TestNet3Params.get()
            val seed = DeterministicSeed(mnemonic.trim().split("\\s+".toRegex()), null, "", 0L)
            val chain = DeterministicKeyChain.builder().seed(seed).build()

            val coinType = if (isMainnet) 0 else 1
            val path = listOf(
                ChildNumber(84, true),
                ChildNumber(coinType, true),
                ChildNumber(0, true),
                ChildNumber.ZERO,
                ChildNumber(addressIndex)
            )

            val key = chain.getKeyByPath(path, true)
            key.privateKeyAsHex
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun generateElegantBtcQr(
        content: String,
        logoBitmap: Bitmap? = null,
        isMainnet: Boolean = true
    ): Bitmap? {
        return try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val darkBackground = Color.parseColor("#0D0D0D")
            val accentColor = if (isMainnet) Color.parseColor("#D4AF37") else Color.MAGENTA

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val isBlack = bitMatrix.get(x, y)
                    bmp.setPixel(x, y, if (isBlack) accentColor else darkBackground)
                }
            }

            if (logoBitmap != null) {
                val canvas = Canvas(bmp)
                val overlaySize = width / 5
                val deltaWidth = width - overlaySize
                val deltaHeight = height - overlaySize

                val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, overlaySize, overlaySize, false)
                canvas.drawBitmap(scaledLogo, (deltaWidth / 2).toFloat(), (deltaHeight / 2).toFloat(), null)
            }

            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateMnemonic(): String {
        return try {
            val entropy = ByteArray(16)
            SecureRandom().nextBytes(entropy)
            val mnemonicWords = MnemonicCode.INSTANCE.toMnemonic(entropy)
            mnemonicWords.joinToString(" ")
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}