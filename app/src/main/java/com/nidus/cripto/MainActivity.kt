package com.nidus.cripto

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.text.style.TextOverflow
import androidx.fragment.app.FragmentActivity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import com.nidus.cripto.ui.BlockchainService
import com.nidus.cripto.ui.NidusCriptoMainApp

enum class AppScreen {
    SPLASH,
    TERMS,
    PERMISSIONS_EXPLANATION,
    SEED,
    PIN_ENTRY,
    MAIN_APP
}

private fun getStringRes(lang: String, key: String, vararg args: Any): String {
    return when (lang) {
        "en" -> when (key) {
            "permissions_title" -> "APPLICATION PERMISSIONS"
            "permissions_subtitle" -> "To provide you with a complete and secure experience in Nidus Crypto, we need to enable the following features:"
            "camera_title" -> "Camera"
            "camera_desc" -> "Used exclusively to scan QR codes of recipient addresses when making fast sends."
            "notifications_title" -> "Notifications"
            "notifications_desc" -> "To securely and instantly notify you when you receive funds or your transactions are confirmed."
            "grant_permissions_btn" -> "GRANT PERMISSIONS AND CONTINUE"

            "terms_title" -> "TERMS OF SERVICE AND FEE DISCLOSURE"
            "terms_body" -> "NIDUS CRYPTO GOLD CORE LICENSE AGREEMENT AND FEE BREAKDOWN\n\n1. OWNERSHIP, CUSTODY, AND RISK: This wallet generates cryptographic keys and their 12-word BIP39 seed phrases strictly locally and encrypted (AES-256). You are the sole custodian and absolute responsible party for safeguarding your PIN and backup phrases. Loss of this data implies total and irreversible loss of your commercial assets. Nidus Crypto Labs Inc. has no access to or custody of your keys.\n\n2. COMMERCIAL MONETIZATION MODEL (APP FEE): For support, technical maintenance, and premium infrastructure development, a transparent, fixed, and invariable fee of ${args.firstOrNull() ?: "$0.25"} USD (automatically converted to Satoshis according to the current Bitcoin exchange rate) will apply to each outbound transaction transferred from this app, whether in single sends or multi-payment batches.\n\n3. REGULATED COMMERCIAL EXCEPTIONS: No fee deduction or charge will apply only if the net transfer amount is less than 0.00001 BTC.\n\n4. FEE DESTINATION ADDRESS: Settled fees will be automatically and transparently sent on the blockchain to the developer's authorized treasury under the collection addresses stipulated in the software core.\n\n5. ACCEPTANCE AND CONSENT: By pressing 'Accept and Continue', you declare that you understand, consent to, and authorize the app's fee breakdown and assume all civil and legal responsibility for its use."
            "terms_checkbox" -> "I have read and accept the terms and conditions of the agreement"
            "accept_btn" -> "Accept and Continue"
            "reject_btn" -> "Reject and Exit"

            "seed_title" -> "YOUR SECRET PHRASE"
            "seed_subtitle" -> "Save these 12 words in a secure place. They are the only way to recover your wallet."
            "copy_seed_toast" -> "Phrase copied"
            "copy_seed_btn" -> "Copy Words"
            "save_seed_btn" -> "I HAVE SAVED MY WORDS"
            else -> key
        }
        else -> when (key) {
            "permissions_title" -> "PERMISOS DE LA APLICACIÓN"
            "permissions_subtitle" -> "Para brindarte una experiencia completa y segura en Nidus Cripto, necesitamos habilitar las siguientes funciones:"
            "camera_title" -> "Cámara"
            "camera_desc" -> "Utilizada exclusivamente para escanear códigos QR de direcciones de destino al realizar envíos rápidos."
            "notifications_title" -> "Notificaciones"
            "notifications_desc" -> "Para avisarte al instante de forma segura cuando recibas fondos o se confirmen tus transacciones."
            "grant_permissions_btn" -> "CONCEDER PERMISOS Y CONTINUAR"

            "terms_title" -> "TÉRMINOS DE SERVICIO Y DIVULGACIÓN DE TARIFAS"
            "terms_body" -> "CONTRATO DE LICENCIA Y DESGLOSE DE TARIFAS DE NIDUS CRIPTO GOLD CORE\n\n1. PROPIEDAD, CUSTODIA Y RIESGO: Esta billetera genera claves criptográficas y sus 12 palabras semilla BIP39 de forma estrictamente local y cifrada (AES-256). Usted es el único custodio y responsable absoluto de resguardar su PIN y sus frases de respaldo. La pérdida de estos datos implica la pérdida total e irreversible de sus activos comerciales. Nidus Cripto Labs Inc. no tiene acceso ni custodia de sus llaves.\n\n2. MODELO DE MONETIZACIÓN COMERCIAL (APP FEE): Para el soporte, mantenimiento técnico y desarrollo de la infraestructura premium, se aplicará un cargo transparente, fijo e invariable de ${args.firstOrNull() ?: "$0.25"} USD (convertido automáticamente a Satoshis según la cotización vigente de Bitcoin) sobre cada transacción saliente transferida desde esta aplicación, ya sea en envíos individuales o en bloques de multipagos.\n\n3. EXCEPCIONES COMERCIALES REGULADAS: No se aplicará deducción o cobro de comisiones únicamente si el monto de la transferencia neta es inferior a 0.00001 BTC.\n\n4. DIRECCIÓN DE DESTINO DE COMISIONES: Las tarifas liquidadas se enviarán de forma automatizada y transparente en la cadena de bloques a la tesorería autorizada del desarrollador bajo las direcciones de recaudo estipuladas en el núcleo del software.\n\n5. ACEPTACIÓN Y CONSENTIMIENTO: Al presionar 'Aceptar y Continuar', usted declara comprender, consentir y autorizar el desglose de tarifas de la aplicación y asume toda responsabilidad civil y legal de su uso."
            "terms_checkbox" -> "He leído y acepto los términos y condiciones del contrato"
            "accept_btn" -> "Aceptar y Continuar"
            "reject_btn" -> "Rechazar y Salir"

            "seed_title" -> "SU FRASE SECRETA"
            "seed_subtitle" -> "Guarde estas 12 palabras en un lugar seguro. Son la única forma de recuperar su billetera."
            "copy_seed_toast" -> "Frase copiada"
            "copy_seed_btn" -> "Copiar Palabras"
            "save_seed_btn" -> "HE GUARDADO MIS PALABRAS"
            else -> key
        }
    }
}

class MainActivity : FragmentActivity() {
    var navigateToHistory by mutableStateOf(false)
    var initialTxFilter by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("nidus_wallet_prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("key_app_language")) {
            val systemLanguage = java.util.Locale.getDefault().language
            val defaultLang = if (systemLanguage.equals("es", ignoreCase = true)) "es" else "en"
            StorageUtils.setLanguage(this, defaultLang)
        }

        navigateToHistory = intent?.getStringExtra("navigate_to") == "history" || intent?.getStringExtra("txid") != null
        initialTxFilter = intent?.getStringExtra("txid") ?: intent?.getStringExtra("tx_filter") ?: intent?.getStringExtra("filter")

        window.setBackgroundDrawableResource(android.R.color.black)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            NidusCriptoAppRoot(
                initialNavigateToHistory = navigateToHistory,
                initialTxFilter = initialTxFilter
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent?.getStringExtra("navigate_to") == "history" || intent?.getStringExtra("txid") != null) {
            navigateToHistory = true
        }
        val filter = intent?.getStringExtra("txid") ?: intent?.getStringExtra("tx_filter") ?: intent?.getStringExtra("filter")
        if (!filter.isNullOrBlank()) {
            initialTxFilter = filter
        }
    }
}

@Composable
fun NidusCriptoAppRoot(
    initialNavigateToHistory: Boolean = false,
    initialTxFilter: String? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nidus_prefs", Context.MODE_PRIVATE) }
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var userPassword by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        when (currentScreen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onSplashFinished = {
                        val termsAccepted = prefs.getBoolean("terms_accepted", false)
                        val permissionsAsked = prefs.getBoolean("permissions_asked", false)
                        val hasPin = StorageUtils.getSavedPin(context) != null

                        currentScreen = when {
                            !termsAccepted -> AppScreen.TERMS
                            !permissionsAsked -> AppScreen.PERMISSIONS_EXPLANATION
                            !hasPin -> AppScreen.SEED
                            else -> AppScreen.PIN_ENTRY
                        }
                    }
                )
            }

            AppScreen.TERMS -> {
                TermsAndConditionsScreen(
                    onAccept = {
                        prefs.edit().putBoolean("terms_accepted", true).apply()
                        currentScreen = AppScreen.PERMISSIONS_EXPLANATION
                    },
                    onReject = {
                        (context as? Activity)?.finish()
                    }
                )
            }

            AppScreen.PERMISSIONS_EXPLANATION -> {
                PermissionsExplanationScreen(
                    onContinue = {
                        prefs.edit().putBoolean("permissions_asked", true).apply()
                        currentScreen = if (!StorageUtils.isWalletCreated(context)) {
                            AppScreen.SEED
                        } else {
                            AppScreen.PIN_ENTRY
                        }
                    }
                )
            }

            AppScreen.SEED -> {
                val mnemonic = remember { RealWalletManager.generateMnemonic() }

                SeedDisplayScreen(
                    mnemonic = mnemonic,
                    onConfirmed = {
                        StorageUtils.saveTempMnemonic(context, mnemonic)
                        currentScreen = AppScreen.PIN_ENTRY
                    }
                )
            }

            AppScreen.PIN_ENTRY -> {
                PinEntryScreen(
                    onPinSuccess = { newPin ->
                        StorageUtils.savePin(context, newPin)
                        userPassword = newPin

                        val tempMnemonic = StorageUtils.getTempMnemonic(context) ?: ""

                        if (tempMnemonic.isNotEmpty()) {
                            val encryptedSeed = SecurityUtils.encrypt(tempMnemonic, newPin)
                            StorageUtils.saveEncryptedSeed(context, encryptedSeed)
                            StorageUtils.setWalletCreated(context, true)

                            val mainnetAddr = RealWalletManager.deriveRealSegWitAddress(context, newPin, 0, true)
                            val testnetAddr = RealWalletManager.deriveRealSegWitAddress(context, newPin, 0, false)

                            val initialAddresses = listOfNotNull(
                                mainnetAddr.ifEmpty { null },
                                testnetAddr.ifEmpty { null }
                            )

                            if (initialAddresses.isNotEmpty()) {
                                StorageUtils.saveAddresses(context, initialAddresses)
                            }
                            StorageUtils.clearTempMnemonic(context)
                        } else {
                            val saved = StorageUtils.getAddresses(context)
                            val hasTestnet = saved.any { v -> v.startsWith("tb1q") }

                            if (!hasTestnet) {
                                val testnetAddr = RealWalletManager.deriveRealSegWitAddress(context, newPin, 0, false)
                                if (testnetAddr.isNotEmpty()) {
                                    StorageUtils.saveAddresses(context, (saved + testnetAddr).distinct())
                                }
                            }
                        }

                        currentScreen = AppScreen.MAIN_APP
                    },
                    isBiometricEnabled = StorageUtils.isBiometricEnabled(context)
                )
            }

            AppScreen.MAIN_APP -> {
                val activePassword = userPassword.ifEmpty { StorageUtils.getSavedPin(context) ?: "" }

                LaunchedEffect(Unit) {
                    val savedAddresses = StorageUtils.getAddresses(context)
                    if (savedAddresses.isNotEmpty()) {
                        val primaryAddress = savedAddresses.first()
                        val isMainnet = !primaryAddress.startsWith("tb1")
                        BlockchainService.fetchUtxosForAddress(primaryAddress, isMainnet)
                    }
                }

                NidusCriptoMainApp(
                    userPassword = activePassword,
                    onBiometricStateChanged = { enabled ->
                        StorageUtils.setBiometricEnabled(context, enabled)
                    },
                    initialOpenHistory = initialNavigateToHistory,
                    initialTxFilter = initialTxFilter
                )
            }
        }
    }
}

@Composable
fun PermissionsExplanationScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(StorageUtils.getLanguage(context)) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onContinue()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = getStringRes(currentLanguage, "permissions_title"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = getStringRes(currentLanguage, "permissions_subtitle"),
                fontSize = 12.sp,
                color = TextGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            PermissionFeatureCard(
                icon = Icons.Default.CameraAlt,
                title = getStringRes(currentLanguage, "camera_title"),
                description = getStringRes(currentLanguage, "camera_desc")
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionFeatureCard(
                icon = Icons.Default.Notifications,
                title = getStringRes(currentLanguage, "notifications_title"),
                description = getStringRes(currentLanguage, "notifications_desc")
            )
        }

        Button(
            onClick = {
                val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
        ) {
            Text(
                text = getStringRes(currentLanguage, "grant_permissions_btn"),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PermissionFeatureCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = GoldAccent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, color = TextGray, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
fun TermsAndConditionsScreen(
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(StorageUtils.getLanguage(context)) }
    var isChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = getStringRes(currentLanguage, "terms_title"),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(CardBackground)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = getStringRes(currentLanguage, "terms_body", "\$${TransactionManager.APP_FEE_USD}"),
                    fontSize = 11.sp,
                    color = Color.White,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isChecked = !isChecked }
                .padding(vertical = 2.dp)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = GoldAccent,
                    uncheckedColor = TextGray,
                    checkmarkColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = getStringRes(currentLanguage, "terms_checkbox"),
                fontSize = 11.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAccept,
                enabled = isChecked,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    disabledContainerColor = GoldAccent.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = getStringRes(currentLanguage, "accept_btn"),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onReject,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground)
            ) {
                Text(
                    text = getStringRes(currentLanguage, "reject_btn"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SeedDisplayScreen(mnemonic: String, onConfirmed: () -> Unit) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(StorageUtils.getLanguage(context)) }
    val words = remember(mnemonic) { mnemonic.split(" ") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(getStringRes(currentLanguage, "seed_title"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                getStringRes(currentLanguage, "seed_subtitle"),
                fontSize = 12.sp,
                color = TextGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until words.size step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WordChip(index = i + 1, word = words[i], modifier = Modifier.weight(1f))
                        if (i + 1 < words.size) {
                            WordChip(index = i + 2, word = words[i + 1], modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Frase Semilla", mnemonic)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, getStringRes(currentLanguage, "copy_seed_toast"), Toast.LENGTH_SHORT).show()
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(getStringRes(currentLanguage, "copy_seed_btn"), color = GoldAccent, fontSize = 12.sp)
            }
        }

        Button(
            onClick = {
                StorageUtils.saveTempMnemonic(context, mnemonic)
                onConfirmed()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
        ) {
            Text(
                text = getStringRes(currentLanguage, "save_seed_btn"),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun WordChip(index: Int, word: String, modifier: Modifier = Modifier) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$index. ", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(word, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}