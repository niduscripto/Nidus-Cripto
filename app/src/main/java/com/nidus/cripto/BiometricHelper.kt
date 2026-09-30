package com.nidus.cripto

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    private fun getStringRes(lang: String, key: String): String {
        return when (lang) {
            "en" -> when (key) {
                "bio_title" -> "Nidus Crypto Authentication"
                "bio_subtitle" -> "Scan your fingerprint to access"
                "fingerprint_not_recognized" -> "Fingerprint not recognized"
                "use_pin" -> "Use PIN"
                else -> key
            }
            else -> when (key) {
                "bio_title" -> "Autenticación Nidus Cripto"
                "bio_subtitle" -> "Escanee su huella para acceder"
                "fingerprint_not_recognized" -> "Huella no reconocida"
                "use_pin" -> "Usar PIN"
                else -> key
            }
        }
    }

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String? = null,
        subtitle: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentLanguage = StorageUtils.getLanguage(activity)
        val finalTitle = title ?: getStringRes(currentLanguage, "bio_title")
        val finalSubtitle = subtitle ?: getStringRes(currentLanguage, "bio_subtitle")

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError(getStringRes(currentLanguage, "fingerprint_not_recognized"))
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(finalTitle)
            .setSubtitle(finalSubtitle)
            .setNegativeButtonText(getStringRes(currentLanguage, "use_pin"))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}