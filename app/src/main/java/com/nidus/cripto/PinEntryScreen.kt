package com.nidus.cripto

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity

val GoldAccent = Color(0xFFD4AF37)
val DarkBackground = Color(0xFF0D0D0D)
val CardBackground = Color(0xFF181818)
val TextGray = Color(0xFFA0A0A0)

private fun getStringRes(lang: String, key: String): String {
    return when (lang) {
        "en" -> when (key) {
            "app_title" -> "NIDUS CRYPTO"
            "enter_pin_prompt" -> "Enter your 6-digit PIN"
            "confirm_pin_prompt" -> "Confirm your 6-digit PIN"
            "pins_dont_match" -> "PINs do not match. Try again."
            "manual_pin_error" -> "Please enter your PIN manually"
            "fingerprint_label" -> "FINGER"
            "incorrect_pin" -> "Incorrect PIN"
            else -> key
        }
        else -> when (key) {
            "app_title" -> "NIDUS CRIPTO"
            "enter_pin_prompt" -> "Ingrese su nuevo PIN de 6 dígitos"
            "confirm_pin_prompt" -> "Confirme su PIN de 6 dígitos"
            "pins_dont_match" -> "Los PINs no coinciden. Intente de nuevo."
            "manual_pin_error" -> "Por favor ingrese su PIN manualmente"
            "fingerprint_label" -> "HUELLA"
            "incorrect_pin" -> "PIN incorrecto"
            else -> key
        }
    }
}

@Composable
fun GoldStarIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
        val path = Path()
        val cx = size.toPx() / 2
        val cy = size.toPx() / 2
        val outerRadius = size.toPx() / 2
        val innerRadius = outerRadius * 0.4f
        val points = 5
        var angle = -Math.PI / 2

        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = cx + (r * Math.cos(angle)).toFloat()
            val y = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            angle += Math.PI / points
        }
        path.close()
        drawPath(path, color = GoldAccent)
    }
}

@Composable
fun PinEntryScreen(
    onPinSuccess: (String) -> Unit,
    isBiometricEnabled: Boolean
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(StorageUtils.getLanguage(context)) }

    val isCreatingPin = remember { StorageUtils.getSavedPin(context) == null }

    var pin by remember { mutableStateOf("") }
    var firstPinDraft by remember { mutableStateOf("") }
    var isConfirmingPhase by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val launchBiometric = {
        val fragmentActivity = context.findFragmentActivity()
        val savedPin = StorageUtils.getSavedPin(context) ?: ""

        if (fragmentActivity != null) {
            BiometricHelper.showBiometricPrompt(
                activity = fragmentActivity,
                onSuccess = {
                    if (savedPin.isNotEmpty()) {
                        onPinSuccess(savedPin)
                    } else {
                        errorMessage = getStringRes(currentLanguage, "manual_pin_error")
                    }
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        } else {
            android.util.Log.e("NIDUS_BIOMETRIC", "No se encontró FragmentActivity en la jerarquía del Context")
        }
    }

    LaunchedEffect(Unit) {
        if (!isCreatingPin && isBiometricEnabled && BiometricHelper.isBiometricAvailable(context)) {
            launchBiometric()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = getStringRes(currentLanguage, "app_title"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isCreatingPin && isConfirmingPhase) {
                    getStringRes(currentLanguage, "confirm_pin_prompt")
                } else {
                    getStringRes(currentLanguage, "enter_pin_prompt")
                },
                fontSize = 14.sp,
                color = TextGray
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 25.dp)
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < pin.length
                    val starSize by animateFloatAsState(
                        targetValue = if (isFilled) 24f else 18f,
                        label = "starAnimation"
                    )

                    Box(
                        modifier = Modifier
                            .size(25.dp)
                            .border(
                                width = if (isFilled) 2.dp else 1.dp,
                                color = if (isFilled) GoldAccent else TextGray,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            GoldStarIcon(size = starSize.dp)
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage, color = Color.Red, fontSize = 13.sp)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val buttons = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            buttons.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    row.forEach { digit ->
                        when (digit) {
                            "BIO" -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = !isCreatingPin && isBiometricEnabled) {
                                            if (!isCreatingPin && isBiometricEnabled) {
                                                launchBiometric()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!isCreatingPin && isBiometricEnabled) {
                                        Text(
                                            text = getStringRes(currentLanguage, "fingerprint_label"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldAccent
                                        )
                                    }
                                }
                            }
                            "DEL" -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(CardBackground)
                                        .clickable {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⌫",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(CardBackground)
                                        .clickable {
                                            if (pin.length < 6) {
                                                pin += digit
                                                errorMessage = ""

                                                if (pin.length == 6) {
                                                    if (isCreatingPin) {
                                                        if (!isConfirmingPhase) {
                                                            firstPinDraft = pin
                                                            pin = ""
                                                            isConfirmingPhase = true
                                                        } else {
                                                            if (pin == firstPinDraft) {
                                                                StorageUtils.savePin(context, pin)
                                                                onPinSuccess(pin)
                                                            } else {
                                                                errorMessage = getStringRes(currentLanguage, "pins_dont_match")
                                                                pin = ""
                                                                firstPinDraft = ""
                                                                isConfirmingPhase = false
                                                            }
                                                        }
                                                    } else {
                                                        val savedPin = StorageUtils.getSavedPin(context)
                                                        if (savedPin == pin) {
                                                            StorageUtils.savePin(context, pin)
                                                            onPinSuccess(pin)
                                                        } else {
                                                            errorMessage = getStringRes(currentLanguage, "incorrect_pin")
                                                            pin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = digit,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}