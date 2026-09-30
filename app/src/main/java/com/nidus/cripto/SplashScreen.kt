package com.nidus.cripto

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private fun getStringRes(lang: String, key: String, vararg args: Any): String {
    return when (lang) {
        "en" -> when (key) {
            "logo_desc" -> "Splash Logo"
            "app_title" -> "NIDUS CRYPTO"
            "app_subtitle" -> "GOLD CORE WALLET"
            "version_text" -> "Version ${args.firstOrNull() ?: "1.0"}"
            else -> key
        }
        else -> when (key) {
            "logo_desc" -> "Logo Splash"
            "app_title" -> "NIDUS CRIPTO"
            "app_subtitle" -> "GOLD CORE WALLET"
            "version_text" -> "Versión ${args.firstOrNull() ?: "1.0"}"
            else -> key
        }
    }
}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(StorageUtils.getLanguage(context)) }
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "SplashAlpha"
    )

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp)
                .alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = getStringRes(currentLanguage, "logo_desc"),
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = getStringRes(currentLanguage, "app_title"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getStringRes(currentLanguage, "app_subtitle"),
                    fontSize = 11.sp,
                    color = TextGray,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = getStringRes(currentLanguage, "version_text", appVersion),
                fontSize = 11.sp,
                color = TextGray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}