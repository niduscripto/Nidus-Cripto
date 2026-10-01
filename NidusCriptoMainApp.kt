package com.nidus.cripto.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nidus.cripto.NetworkSyncRepository
import com.nidus.cripto.R
import com.nidus.cripto.RealWalletManager
import com.nidus.cripto.SecurityUtils
import com.nidus.cripto.SoundEffectsHelper
import com.nidus.cripto.StorageUtils
import com.nidus.cripto.UnspentOutput
import com.nidus.cripto.WalletTransaction
import com.nidus.cripto.contacts.AddressBookManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

private val GoldAccent = Color(0xFFFFD700)
private val DarkBackground = Color(0xFF121212)
private val CardBackground = Color(0xFF1E1E1E)
private val TextGray = Color(0xFFA0A0A0)
private val OfflineRed = Color(0xFFE53935)

enum class Screen {
    Home,
    Send,
    History,
    Options,
    Portfolio
}

private fun getStringRes(lang: String, key: String): String {
    return when (lang) {
        "en" -> when (key) {
            "offline_mode" -> "Offline Mode - Showing local cached balance"
            "syncing" -> "Syncing..."
            "no_internet" -> "No internet connection"
            "send" -> "Send"
            "history" -> "History"
            "options" -> "Options"
            "total_balance" -> "TOTAL BALANCE"
            "receive_segwit_main" -> "SEGWIT ADDRESS (MAINNET)"
            "receive_segwit_test" -> "SEGWIT ADDRESS (TESTNET)"
            "tap_to_share" -> "Tap QR to share"
            "copy" -> "Copy"
            "list" -> " List"
            "new" -> " New"
            "show_address_options" -> "▲ SHOW ADDRESS & OPTIONS"
            "request_amount_note" -> "▼ REQUEST AMOUNT & NOTE"
            "configure_specific_charge" -> "Configure Specific Charge"
            "amount_btc" -> "Amount (BTC)"
            "note_service" -> "Note / Service (Optional)"
            "request" -> "REQUEST"
            "clear" -> "CLEAR"
            "my_addresses" -> "MY ADDRESSES"
            "accounts" -> "Accounts"
            "home" -> "Home"
            "scan" -> "Scan"
            "tx_history" -> "TRANSACTION HISTORY"
            "filter_by_txid" -> "Filter by TXID"
            "no_transactions" -> "No transactions recorded"
            "sync_appear_here" -> "Your movements will appear here synchronized"
            "received" -> "Received"
            "sent" -> "Sent"
            "note" -> "Note"
            "refresh" -> "Refresh"
            "logo" -> "Logo"
            "scan_qr" -> "Scan QR"
            "copy_address" -> "Copy Address"
            "error_loading" -> "An error occurred while loading"
            "no_tx_with_tx" -> "No transactions with that TXID"
            "verify_txid" -> "Verify the entered identifier"
            "new_version" -> "New version available"
            "new_version_text" -> "¡Remake"
            "new_version_descript" -> "An update is available at niduscripto.com"
            "new_version_button1" -> "UPDATE"
            "new_version_button2" -> "Later"
            "enter_btc_amount" -> "Enter an amount in BTC"
            "address_copied_toast" -> "BTC Address copied"
            "portfolio" -> "Investment Portfolio"
            "total_wallet_value" -> "TOTAL PORTFOLIO VALUE"
            "synchronized_network" -> "Synchronized from Network"
            "btc_entries_history" -> "Bitcoin Entries by History"
            "no_entries_registered" -> "No entries recorded in history"
            "bitcoin_entry_no_note" -> "🟢 Bitcoin Entry (No Note)"
            "date" -> "Date"
            "current_price_label" -> "Last Received"
            "bitcoin_price" -> "Current Price: "
            "bitcoin_price_last" -> "Average Price: "
            else -> key
        }
        else -> when (key) {
            "offline_mode" -> "Modo Offline - Mostrando saldo en caché local"
            "syncing" -> "Sincronizando..."
            "no_internet" -> "Sin conexión a internet"
            "send" -> "Enviar"
            "history" -> "Historial"
            "options" -> "Opciones"
            "total_balance" -> "BALANCE TOTAL"
            "receive_segwit_main" -> "DIRECCIÓN SEGWIT (MAINNET)"
            "receive_segwit_test" -> "DIRECCIÓN SEGWIT (TESTNET)"
            "tap_to_share" -> "Toca el QR para compartir"
            "copy" -> "Copiar"
            "list" -> " Lista"
            "new" -> " Nueva"
            "show_address_options" -> "▲ MOSTRAR DIRECCIÓN Y OPCIONES"
            "request_amount_note" -> "▼ SOLICITAR CANTIDAD Y NOTA"
            "configure_specific_charge" -> "Configurar Cobro Específico"
            "amount_btc" -> "Monto (BTC)"
            "note_service" -> "Nota / Servicio (Opcional)"
            "request" -> "SOLICITAR"
            "clear" -> "LIMPIAR"
            "my_addresses" -> "MIS DIRECCIONES"
            "accounts" -> "Cuentas"
            "home" -> "Inicio"
            "scan" -> "Escanear"
            "tx_history" -> "HISTORIAL DE TRANSACCIONES"
            "filter_by_txid" -> "Filtrar por TXID"
            "no_transactions" -> "No hay transacciones registradas"
            "sync_appear_here" -> "Tus movimientos aparecerán aquí sincronizados"
            "received" -> "Recibido"
            "sent" -> "Enviado"
            "note" -> "Nota"
            "refresh" -> "Actualizar"
            "logo" -> "Logo"
            "scan_qr" -> "Escanear QR"
            "copy_address" -> "Copiar Dirección"
            "error_loading" -> "Ocurrió un error al cargar"
            "no_tx_with_tx" -> "No hay transacciones con ese tx"
            "verify_txid" -> "Verifica el identificador ingresado"
            "new_version" -> "Nueva versión disponible"
            "new_version_text" -> "¡Nueva Versión"
            "new_version_descript" -> "Hay una actualización disponible en niduscripto.com"
            "new_version_button1" -> "ACTUALIZAR"
            "new_version_button2" -> "Más tarde"
            "enter_btc_amount" -> "Ingrese una cantidad en BTC"
            "address_copied_toast" -> "Dirección BTC copiada"
            "portfolio" -> "Portafolio de Inversión"
            "total_wallet_value" -> "VALOR TOTAL DEL PORTAFOLIO"
            "synchronized_network" -> "Sincronizado de la Red"
            "btc_entries_history" -> "Entradas de BTC por Historial"
            "no_entries_registered" -> "No hay entradas registradas en el historial"
            "bitcoin_entry_no_note" -> "🟢 Entrada de Bitcoin (Sin Nota)"
            "date" -> "Fecha"
            "current_price_label" -> "Último Recibido"
            "bitcoin_price" -> "Precio Actual: "
            "bitcoin_price_last" -> "Precio Promedio: "
            else -> key
        }
    }
}

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val updateUrl: String,
    val releaseNotes: String
)

fun parseRobustDouble(input: String): Double {
    if (input.isBlank()) return 0.0
    val cleanInput = input.trim().replace(" ", "").replace(",", ".")
    return cleanInput.toDoubleOrNull() ?: 0.0
}

private const val PREFS_BALANCE_CACHE = "nidus_balance_cache"

private fun saveCachedBalance(context: Context, address: String, satoshis: Long) {
    if (address.isNotEmpty()) {
        val prefs = context.getSharedPreferences(PREFS_BALANCE_CACHE, Context.MODE_PRIVATE)
        prefs.edit().putLong(address, satoshis).apply()
    }
}

private fun getCachedBalance(context: Context, address: String): Long {
    if (address.isEmpty()) return 0L
    val prefs = context.getSharedPreferences(PREFS_BALANCE_CACHE, Context.MODE_PRIVATE)
    return prefs.getLong(address, 0L)
}

@Composable
fun rememberIsNetworkAvailable(): State<Boolean> {
    val context = LocalContext.current
    val isConnected = remember { mutableStateOf(checkInitialNetwork(context)) }

    DisposableEffect(context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected.value = true
            }

            override fun onLost(network: Network) {
                isConnected.value = false
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return isConnected
}

private fun checkInitialNetwork(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NidusCriptoMainApp(
    userPassword: String = "",
    onBiometricStateChanged: (Boolean) -> Unit = {},
    onLogout: () -> Unit = {},
    initialOpenHistory: Boolean = false,
    initialTxFilter: String? = null
) {
    val context = LocalContext.current
    val isNetworkAvailable by rememberIsNetworkAvailable()
    val coroutineScope = rememberCoroutineScope()

    val currentLanguage by remember { mutableStateOf(StorageUtils.getLanguage(context)) }

    var isMainnet by remember { mutableStateOf(StorageUtils.isMainnet(context)) }
    var currentScreen by remember { mutableStateOf(if (initialOpenHistory || !initialTxFilter.isNullOrBlank()) Screen.History else Screen.Home) }
    var activeTxFilter by remember { mutableStateOf(initialTxFilter ?: "") }

    var isBiometricEnabled by remember { mutableStateOf(StorageUtils.isBiometricEnabled(context)) }
    var currentBalanceSatoshis by remember { mutableLongStateOf(0L) }

    var globalBtcPriceUsd by remember {
        mutableStateOf(parseRobustDouble(StorageUtils.getLastBtcPrice(context) ?: "0.0"))
    }

    val initialLocalPrice = StorageUtils.getLastBtcPrice(context)
    val validatedInitialPrice = remember(initialLocalPrice) {
        val parsed = initialLocalPrice?.toDoubleOrNull() ?: 0.0
        if (parsed > 0.0 && parsed < 1_000_000.0) initialLocalPrice else "0.00"
    }

    var btcGlobalPriceText by remember {
        mutableStateOf(
            if (!validatedInitialPrice.isNullOrEmpty() && validatedInitialPrice != "0.00") "$validatedInitialPrice USD" else getStringRes(currentLanguage, "syncing")
        )
    }

    var updateDialogData by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    var lastManualSyncTime by remember { mutableStateOf(0L) }
    var showMainScanner by remember { mutableStateOf(false) }
    var pendingScannedUri by remember { mutableStateOf<String?>(null) }
    val activeAccentColor = if (isMainnet) GoldAccent else Color.Magenta

    LaunchedEffect(isNetworkAvailable) {
        if (!isNetworkAvailable) {
            val local = StorageUtils.getLastBtcPrice(context)
            val parsedLocal = local?.toDoubleOrNull() ?: 0.0
            val safeLocal = if (parsedLocal > 0.0 && parsedLocal < 1_000_000.0) {
                BigDecimal(parsedLocal)
                    .setScale(2, RoundingMode.CEILING)
                    .toPlainString()
            } else "0.00"

            btcGlobalPriceText = if (safeLocal != "0.00") "$safeLocal USD" else getStringRes(currentLanguage, "offline_mode")
            val numericPrice = parseRobustDouble(safeLocal)
            if (numericPrice > 0.0) {
                globalBtcPriceUsd = numericPrice
            }
            return@LaunchedEffect
        }

        val fetchBtcPriceAction: suspend () -> Unit = {
            try {
                val priceResult = withTimeoutOrNull(4000L) {
                    val url = URL("https://pro-api.coinmarketcap.com/public-api/v1/simple/price?ids=1&convert=USD")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 3000
                        readTimeout = 3000
                        setRequestProperty("Accept", "application/json")
                    }

                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().readText()
                        val rawPriceStr = text.substringAfter("\"price\":").substringBefore("]").substringBefore("}").trim()
                        val numericVal = rawPriceStr.toDoubleOrNull() ?: parseRobustDouble(rawPriceStr)

                        if (numericVal > 1.0 && numericVal < 1_000_000.0) {
                            BigDecimal(numericVal)
                                .setScale(2, RoundingMode.CEILING)
                                .toPlainString()
                        } else null
                    } else null
                }

                if (priceResult != null) {
                    StorageUtils.saveLastBtcPrice(context, priceResult)
                    val numericPrice = parseRobustDouble(priceResult)
                    withContext(Dispatchers.Main) {
                        btcGlobalPriceText = "$priceResult USD"
                        globalBtcPriceUsd = numericPrice
                    }
                }
            } catch (_: Exception) {
                val local = StorageUtils.getLastBtcPrice(context)
                val parsedLocal = local?.toDoubleOrNull() ?: 0.0
                if (parsedLocal > 1.0 && parsedLocal < 1_000_000.0) {
                    val numericPrice = parseRobustDouble(local!!)
                    withContext(Dispatchers.Main) {
                        btcGlobalPriceText = "$local USD"
                        globalBtcPriceUsd = numericPrice
                    }
                }
            }
        }

        withContext(Dispatchers.IO) {
            while (isActive && isNetworkAvailable) {
                if (btcGlobalPriceText.equals(getStringRes(currentLanguage, "syncing"))) {
                    fetchBtcPriceAction()
                } else {
                    delay(2 * 60 * 1000L)
                    fetchBtcPriceAction()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://niduscripto.com/version.json")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().readText()
                    val jsonObj = JSONObject(jsonStr)

                    val remoteVersionCode = jsonObj.optInt("version_code", 0)
                    val remoteVersionName = jsonObj.optString("latest_version", "1.0.0")
                    val remoteUpdateUrl = jsonObj.optString("update_url", "https://niduscripto.com/NidusCripto-1.0.0.apk")

                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pInfo.versionCode
                    }

                    if (remoteVersionCode > currentVersionCode) {
                        val info = UpdateInfo(
                            versionCode = remoteVersionCode,
                            versionName = remoteVersionName,
                            updateUrl = remoteUpdateUrl,
                            releaseNotes = getStringRes(currentLanguage, "new_version") + " (${remoteVersionName})."
                        )
                        withContext(Dispatchers.Main) {
                            updateDialogData = info
                            showUpdateDialog = true
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val performSync: (Boolean) -> Unit = { isManual ->
        val currentTime = System.currentTimeMillis()
        if (isManual && currentTime - lastManualSyncTime < 5000L) {
        } else {
            if (isManual) lastManualSyncTime = currentTime
            coroutineScope.launch(Dispatchers.IO) {
                val savedAddresses = StorageUtils.getAddresses(context)
                val targetPrefix = if (isMainnet) "bc1q" else "tb1q"
                val activeAddresses = savedAddresses.filter { it.startsWith(targetPrefix) }

                if (activeAddresses.isNotEmpty()) {
                    if (isNetworkAvailable) {
                        val previousLocalTotalBalance = activeAddresses.sumOf { addr ->
                            getCachedBalance(context, addr)
                        }

                        val totalWalletBalance = activeAddresses.sumOf { addr ->
                            val bal = NetworkSyncRepository.syncBalance(context, addr, isMainnet)
                            saveCachedBalance(context, addr, bal)
                            bal
                        }

                        NetworkSyncRepository.fetchAndSyncWalletTransactions(context, savedAddresses, isMainnet, globalBtcPriceUsd)

                        withContext(Dispatchers.Main) {
                            currentBalanceSatoshis = totalWalletBalance
                            if (totalWalletBalance > previousLocalTotalBalance) {
                                SoundEffectsHelper.playReceiveTone(context)
                            }
                        }
                    } else {
                        val cachedTotal = activeAddresses.sumOf { addr ->
                            getCachedBalance(context, addr)
                        }
                        withContext(Dispatchers.Main) {
                            currentBalanceSatoshis = cachedTotal
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        currentBalanceSatoshis = 0L
                    }
                }
            }
        }
    }

    LaunchedEffect(isMainnet) {
        val savedAddresses = StorageUtils.getAddresses(context)
        val targetPrefix = if (isMainnet) "bc1q" else "tb1q"
        val activeAddresses = savedAddresses.filter { it.startsWith(targetPrefix) }
        currentBalanceSatoshis = if (activeAddresses.isNotEmpty()) {
            activeAddresses.sumOf { getCachedBalance(context, it) }
        } else {
            0L
        }
    }

    LaunchedEffect(isMainnet, isNetworkAvailable) {
        performSync(false)
        if (isNetworkAvailable) {
            while (isActive) {
                delay(3 * 60 * 1000L)
                performSync(false)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NidusBottomNavigationBar(
                currentScreen = currentScreen,
                activeAccentColor = activeAccentColor,
                currentLanguage = currentLanguage,
                onTabSelected = { selected -> currentScreen = selected },
                onScanClicked = { showMainScanner = true }
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            AnimatedVisibility(
                visible = !isNetworkAvailable,
                enter = slideInVertically(),
                exit = slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OfflineRed)
                        .padding(vertical = 6.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = getStringRes(currentLanguage, "no_internet"),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = getStringRes(currentLanguage, "offline_mode"),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            GlobalAppHeader(
                activeAccentColor = activeAccentColor,
                currentLanguage = currentLanguage,
                onRefreshClicked = {
                    if (isNetworkAvailable) {
                        performSync(true)
                        Toast.makeText(context, getStringRes(currentLanguage, "syncing"), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, getStringRes(currentLanguage, "no_internet"), Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentScreen) {
                    Screen.Home -> {
                        HomeScreenContent(
                            userPassword = userPassword,
                            isMainnet = isMainnet,
                            isNetworkAvailable = isNetworkAvailable,
                            currentBalanceSatoshis = currentBalanceSatoshis,
                            globalBtcPriceUsd = globalBtcPriceUsd,
                            btcGlobalPriceText = btcGlobalPriceText,
                            currentLanguage = currentLanguage,
                            onPortfolioClicked = {
                                currentScreen = Screen.Portfolio
                            }
                        )
                    }

                    Screen.Send -> {
                        SafeScreenWrapper(screenTitle = getStringRes(currentLanguage, "send"), currentLanguage = currentLanguage) {
                            val effectivePin = userPassword.ifEmpty { StorageUtils.getSavedPin(context) ?: "" }
                            val encryptedSeed = StorageUtils.getEncryptedSeed(context) ?: ""
                            val decryptedMnemonic = try {
                                SecurityUtils.decrypt(encryptedSeed, effectivePin)
                            } catch (_: Exception) {
                                ""
                            }

                            val privateKeyWif = if (decryptedMnemonic.isNotEmpty()) {
                                RealWalletManager.derivePrivateKeyFromMnemonic(decryptedMnemonic, 0, isMainnet)
                            } else {
                                ""
                            }

                            val currentAddresses = StorageUtils.getAddresses(context).filter {
                                if (isMainnet) it.startsWith("bc1q") else it.startsWith("tb1q")
                            }
                            val myChangeAddress = currentAddresses.firstOrNull() ?: ""

                            var userUtxos by remember { mutableStateOf<List<UnspentOutput>>(emptyList()) }

                            LaunchedEffect(currentAddresses, isMainnet) {
                                if (currentAddresses.isNotEmpty()) {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            coroutineScope {
                                                val deferredUtxos = currentAddresses.map { addr ->
                                                    async {
                                                        BlockchainService.fetchUtxosForAddress(addr, isMainnet = isMainnet)
                                                    }
                                                }
                                                val combinedUtxos = deferredUtxos.awaitAll().flatten()
                                                withContext(Dispatchers.Main) {
                                                    userUtxos = combinedUtxos
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                            MultiSendScreen(
                                isMainnet = isMainnet,
                                availableBalanceSats = currentBalanceSatoshis,
                                feeRateSatPerVb = 10,
                                initialScannedData = pendingScannedUri,
                                onInitialDataConsumed = { pendingScannedUri = null },
                                userPrivateKeyWif = privateKeyWif,
                                userUtxos = userUtxos,
                                btcPriceUsd = globalBtcPriceUsd,
                                myChangeAddress = myChangeAddress,
                                onNavigateToTransaction = { txid ->
                                    activeTxFilter = txid
                                    currentScreen = Screen.History
                                }
                            )
                        }
                    }

                    Screen.History -> {
                        SafeScreenWrapper(getStringRes(currentLanguage, "history"), currentLanguage = currentLanguage) {
                            HistoryScreenContent(
                                isMainnet = isMainnet,
                                activeAccentColor = activeAccentColor,
                                globalBtcPriceUsd = globalBtcPriceUsd,
                                initialTxFilter = activeTxFilter,
                                currentLanguage = currentLanguage,
                                onFilterChanged = { newFilter -> activeTxFilter = newFilter },
                                onTriggerSync = { performSync(true) }
                            )
                        }
                    }

                    Screen.Options -> {
                        SafeScreenWrapper(getStringRes(currentLanguage, "options"), currentLanguage = currentLanguage) {
                            OptionsScreen(
                                isMainnet = isMainnet,
                                onToggleNetwork = { newMainnetState ->
                                    isMainnet = newMainnetState
                                    StorageUtils.setMainnet(context, newMainnetState)
                                    StorageUtils.saveAddresses(context, emptyList())
                                    performSync(true)

                                    val savedAddresses = StorageUtils.getAddresses(context)
                                    val targetPrefix = if (newMainnetState) "bc1q" else "tb1q"
                                    val targetAddress = savedAddresses.find { it.startsWith(targetPrefix) } ?: ""

                                    if (targetAddress.isNotEmpty()) {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            BlockchainService.fetchUtxosForAddress(targetAddress, newMainnetState)
                                        }
                                    }
                                },
                                isBiometricEnabled = isBiometricEnabled,
                                onToggleBiometric = { isBiometricEnabled = it },
                                onImportWalletRequested = { },
                                currentBalanceSatoshis = currentBalanceSatoshis,
                                onSyncRequested = { performSync(true) }
                            )
                        }
                    }

                    Screen.Portfolio -> {
                        SafeScreenWrapper(getStringRes(currentLanguage, "portfolio"), currentLanguage) {
                            PortfolioScreenContent(
                                currentBalanceSatoshis = currentBalanceSatoshis,
                                globalBtcPriceUsd = globalBtcPriceUsd,
                                activeAccentColor = activeAccentColor,
                                currentLanguage = currentLanguage,
                                isMainnet = isMainnet
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMainScanner) {
        PremiumScannerDialog(
            accentColor = activeAccentColor,
            onQrScanned = { scannedData ->
                showMainScanner = false
                pendingScannedUri = scannedData
                currentScreen = Screen.Send
            },
            onDismiss = { showMainScanner = false }
        )
    }

    if (showUpdateDialog && updateDialogData != null) {
        val info = updateDialogData!!
        AlertDialog(
            onDismissRequest = {},
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = getStringRes(currentLanguage, "new_version_text") + " ${info.versionName}!",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = getStringRes(currentLanguage, "new_version_descript"),
                        color = TextGray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = info.releaseNotes,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.updateUrl))
                        context.startActivity(intent)
                        showUpdateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)
                ) {
                    Text(text = getStringRes(currentLanguage, "new_version_button1"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(text = getStringRes(currentLanguage, "new_version_button2"), color = TextGray)
                }
            }
        )
    }
}

@Composable
private fun GlobalAppHeader(
    activeAccentColor: Color,
    currentLanguage: String,
    onRefreshClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = getStringRes(currentLanguage, "logo"),
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Text(
                text = "NIDUS CRIPTO",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
        }

        IconButton(
            onClick = onRefreshClicked,
            modifier = Modifier
                .size(40.dp)
                .background(CardBackground, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = getStringRes(currentLanguage, "refresh"),
                tint = activeAccentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    userPassword: String,
    isMainnet: Boolean,
    isNetworkAvailable: Boolean,
    currentBalanceSatoshis: Long,
    globalBtcPriceUsd: Double,
    btcGlobalPriceText: String,
    currentLanguage: String,
    onPortfolioClicked: () -> Unit = {}
) {
    val context = LocalContext.current

    val mainnetAddresses = remember { mutableStateListOf<String>() }
    val testnetAddresses = remember { mutableStateListOf<String>() }

    var selectedAddressIndex by remember { mutableStateOf(0) }
    var showAddressSheet by remember { mutableStateOf(false) }

    var showPaymentPanel by remember { mutableStateOf(false) }
    var requestAmountInput by remember { mutableStateOf("") }
    var requestNoteInput by remember { mutableStateOf("") }
    var customPaymentUri by remember { mutableStateOf<String?>(null) }

    val currentAddresses = if (isMainnet) mainnetAddresses else testnetAddresses
    val activeAccentColor = if (isMainnet) GoldAccent else Color.Magenta

    var qrBitmapState by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(isMainnet, userPassword) {
        selectedAddressIndex = 0
        customPaymentUri = null

        val effectivePin = userPassword.ifEmpty { StorageUtils.getSavedPin(context) ?: "" }
        var savedAddresses = StorageUtils.getAddresses(context)
        val targetPrefix = if (isMainnet) "bc1q" else "tb1q"

        if (savedAddresses.none { it.startsWith(targetPrefix) } && effectivePin.isNotEmpty()) {
            val newDerivedAddress = withContext(Dispatchers.IO) {
                RealWalletManager.deriveRealSegWitAddress(
                    context,
                    effectivePin,
                    0,
                    isMainnet
                )
            }

            if (newDerivedAddress.isNotEmpty()) {
                savedAddresses = (savedAddresses + newDerivedAddress).distinct()
                StorageUtils.saveAddresses(context, savedAddresses)
            }
        }

        if (isMainnet) {
            val filtered = savedAddresses.filter { it.startsWith("bc1q") }
            mainnetAddresses.clear()
            mainnetAddresses.addAll(filtered)
        } else {
            val filtered = savedAddresses.filter { it.startsWith("tb1q") }
            testnetAddresses.clear()
            testnetAddresses.addAll(filtered)
        }
    }

    val baseAddress = remember(mainnetAddresses.size, testnetAddresses.size, isMainnet, selectedAddressIndex) {
        val currentList = if (isMainnet) mainnetAddresses else testnetAddresses
        val fromList = currentList.getOrNull(selectedAddressIndex)

        if (!fromList.isNullOrEmpty()) {
            fromList
        } else {
            val saved = StorageUtils.getAddresses(context)
            val prefix = if (isMainnet) "bc1q" else "tb1q"
            saved.filter { it.startsWith(prefix) }.getOrNull(selectedAddressIndex) ?: getStringRes(currentLanguage, "syncing")
        }
    }

    val qrContent = customPaymentUri ?: if (baseAddress.startsWith("bc1") || baseAddress.startsWith("tb1")) {
        "bitcoin:$baseAddress"
    } else {
        baseAddress
    }

    LaunchedEffect(qrContent, isMainnet) {
        if (baseAddress.startsWith("bc1") || baseAddress.startsWith("tb1")) {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.app_icon)
            qrBitmapState = withContext(Dispatchers.Default) {
                RealWalletManager.generateElegantBtcQr(
                    content = qrContent,
                    logoBitmap = logo,
                    isMainnet = isMainnet
                )
            }
        } else {
            qrBitmapState = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(getStringRes(currentLanguage, "total_balance"), fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = getStringRes(currentLanguage, "bitcoin_price") + btcGlobalPriceText,
                        fontSize = 14.sp,
                        color = if (btcGlobalPriceText.contains("Offline") || btcGlobalPriceText.contains("Sin conexión")) OfflineRed else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val btcValue = currentBalanceSatoshis / 100_000_000.0
                val usdValue = btcValue * globalBtcPriceUsd

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = String.format(Locale.US, "%.8f BTC", btcValue),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "≈ %.2f USD", usdValue),
                            fontSize = 13.sp,
                            color = TextGray
                        )
                    }

                    IconButton(
                        onClick = onPortfolioClicked,
                        modifier = Modifier
                            .size(38.dp)
                            .background(DarkBackground, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = getStringRes(currentLanguage, "portfolio"),
                            tint = activeAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isMainnet) getStringRes(currentLanguage, "receive_segwit_main") else getStringRes(currentLanguage, "receive_segwit_test"),
                fontSize = 12.sp,
                color = activeAccentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val currentQr = qrBitmapState
            if (currentQr != null) {
                Image(
                    bitmap = currentQr.asImageBitmap(),
                    contentDescription = getStringRes(currentLanguage, "scan_qr"),
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Solicitud de Bitcoin")
                                putExtra(Intent.EXTRA_TEXT, qrContent)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir Solicitud"))
                        }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .background(DarkBackground, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = activeAccentColor, strokeWidth = 2.dp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(getStringRes(currentLanguage, "tap_to_share"), fontSize = 11.sp, color = TextGray)
            Spacer(modifier = Modifier.height(10.dp))

            if (!showPaymentPanel) {
                Surface(
                    color = DarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (baseAddress.isNotEmpty() && !baseAddress.startsWith("Generando")) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Dirección BTC", baseAddress)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, getStringRes(currentLanguage, "address_copied_toast"), Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(baseAddress, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ContentCopy, contentDescription = getStringRes(currentLanguage, "copy_address"), tint = activeAccentColor, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val coroutineScope = rememberCoroutineScope()
                var isGeneratingAddress by remember { mutableStateOf(false) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showAddressSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = activeAccentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(getStringRes(currentLanguage, "list"), color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        enabled = !isGeneratingAddress,
                        onClick = {
                            isGeneratingAddress = true
                            coroutineScope.launch {
                                val newAddress = withContext(Dispatchers.IO) {
                                    val nextIndex = currentAddresses.size
                                    RealWalletManager.deriveRealSegWitAddress(context, userPassword, nextIndex, isMainnet)
                                }

                                if (newAddress.isNotEmpty()) {
                                    currentAddresses.add(newAddress)
                                    val allSaved = StorageUtils.getAddresses(context) + newAddress
                                    StorageUtils.saveAddresses(context, allSaved.distinct())
                                    selectedAddressIndex = currentAddresses.size - 1
                                    customPaymentUri = null
                                }
                                isGeneratingAddress = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)
                    ) {
                        if (isGeneratingAddress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(getStringRes(currentLanguage, "new"), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = { showPaymentPanel = !showPaymentPanel },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (showPaymentPanel) getStringRes(currentLanguage, "show_address_options") else getStringRes(currentLanguage, "request_amount_note"),
                    color = activeAccentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showPaymentPanel) {
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBackground)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(getStringRes(currentLanguage, "configure_specific_charge"), fontSize = 12.sp, color = activeAccentColor, fontWeight = FontWeight.Bold)

                    val requestBtcVal = parseRobustDouble(requestAmountInput)
                    val requestUsdVal = requestBtcVal * globalBtcPriceUsd

                    OutlinedTextField(
                        value = requestAmountInput,
                        onValueChange = { newValue ->
                            val formatted = newValue.replace(',', '.')
                            if (formatted.isEmpty() || formatted.matches(Regex("^\\d*\\.?\\d*$"))) {
                                requestAmountInput = formatted
                            }
                        },
                        label = { Text(getStringRes(currentLanguage, "amount_btc"), color = TextGray, fontSize = 11.sp) },
                        trailingIcon = {
                            Text(
                                text = String.format(Locale.US, "≈ $%.2f", requestUsdVal),
                                color = activeAccentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeAccentColor,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = requestNoteInput,
                        onValueChange = { requestNoteInput = it },
                        label = { Text(getStringRes(currentLanguage, "note_service"), color = TextGray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeAccentColor,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val amount = requestAmountInput.trim().replace(',', '.')
                                val note = requestNoteInput.trim()

                                if (amount.isNotEmpty()) {
                                    val uriBuilder = StringBuilder("bitcoin:$baseAddress?amount=$amount")
                                    if (note.isNotEmpty()) {
                                        uriBuilder.append("&message=${Uri.encode(note)}")
                                    }
                                    customPaymentUri = uriBuilder.toString()
                                } else {
                                    Toast.makeText(context, getStringRes(currentLanguage, "enter_btc_amount"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)
                        ) {
                            Text(getStringRes(currentLanguage, "request"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                requestAmountInput = ""
                                requestNoteInput = ""
                                customPaymentUri = null
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, activeAccentColor)
                        ) {
                            Text(getStringRes(currentLanguage, "clear"), color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddressSheet) {
        val addressBalances = remember { mutableStateMapOf<String, Long>() }

        LaunchedEffect(currentAddresses) {
            currentAddresses.forEach { addr ->
                addressBalances[addr] = getCachedBalance(context, addr)
            }

            if (isNetworkAvailable) {
                withContext(Dispatchers.IO) {
                    currentAddresses.forEach { addr ->
                        val bal = NetworkSyncRepository.syncBalance(context, addr, isMainnet)
                        saveCachedBalance(context, addr, bal)
                        addressBalances[addr] = bal
                    }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAddressSheet = false },
            containerColor = CardBackground,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(TextGray, CircleShape)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${getStringRes(currentLanguage, "my_addresses")} (${if (isMainnet) "MAINNET" else "TESTNET"})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeAccentColor
                    )
                    Text(
                        text = "${currentAddresses.size} ${getStringRes(currentLanguage, "accounts")}",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxHeight(0.6f)
                ) {
                    itemsIndexed(currentAddresses) { index, address ->
                        val isSelected = index == selectedAddressIndex
                        val satoshis = addressBalances[address] ?: getCachedBalance(context, address)
                        val btcVal = satoshis / 100_000_000.0
                        val usdVal = btcVal * globalBtcPriceUsd

                        val addressBalanceBtc = String.format(Locale.US, "%.8f", btcVal)
                        val addressBalanceUsd = String.format(Locale.US, "%.2f", usdVal)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) activeAccentColor.copy(alpha = 0.12f) else DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) activeAccentColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAddressIndex = index
                                    customPaymentUri = null
                                    showAddressSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isSelected) activeAccentColor else CardBackground,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${address.take(10)}...${address.takeLast(8)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$addressBalanceBtc BTC ≈ $$addressBalanceUsd USD",
                                        fontSize = 11.sp,
                                        color = if (isSelected) activeAccentColor else TextGray
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Seleccionado",
                                        tint = activeAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun NidusBottomNavigationBar(
    currentScreen: Screen,
    activeAccentColor: Color,
    currentLanguage: String,
    onTabSelected: (Screen) -> Unit,
    onScanClicked: () -> Unit
) {
    NavigationBar(
        containerColor = DarkBackground,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.Home,
            onClick = { onTabSelected(Screen.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = getStringRes(currentLanguage, "home")) },
            label = { Text(getStringRes(currentLanguage, "home"), fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = activeAccentColor,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray,
                indicatorColor = activeAccentColor
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.Send,
            onClick = { onTabSelected(Screen.Send) },
            icon = { Icon(Icons.Default.Send, contentDescription = getStringRes(currentLanguage, "send")) },
            label = { Text(getStringRes(currentLanguage, "send"), fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = activeAccentColor,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray,
                indicatorColor = activeAccentColor
            )
        )

        NavigationBarItem(
            selected = false,
            onClick = onScanClicked,
            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = getStringRes(currentLanguage, "scan"), modifier = Modifier.size(24.dp)) },
            label = { Text(getStringRes(currentLanguage, "scan"), fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = activeAccentColor,
                unselectedTextColor = activeAccentColor
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.History,
            onClick = { onTabSelected(Screen.History) },
            icon = { Icon(Icons.Default.History, contentDescription = getStringRes(currentLanguage, "history")) },
            label = { Text(getStringRes(currentLanguage, "history"), fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = activeAccentColor,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray,
                indicatorColor = activeAccentColor
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.Options,
            onClick = { onTabSelected(Screen.Options) },
            icon = { Icon(Icons.Default.Settings, contentDescription = getStringRes(currentLanguage, "options")) },
            label = { Text(getStringRes(currentLanguage, "options"), fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = activeAccentColor,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray,
                indicatorColor = activeAccentColor
            )
        )
    }
}

@Composable
private fun SafeScreenWrapper(screenTitle: String, currentLanguage: String, content: @Composable () -> Unit) {
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    if (hasError) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${getStringRes(currentLanguage, "error_loading")} $screenTitle",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreenContent(
    isMainnet: Boolean,
    activeAccentColor: Color,
    globalBtcPriceUsd: Double,
    initialTxFilter: String = "",
    currentLanguage: String,
    onFilterChanged: (String) -> Unit = {},
    onTriggerSync: () -> Unit = {}
) {
    val context = LocalContext.current
    var allTransactions by remember(isMainnet) { mutableStateOf(StorageUtils.getTransactions(context)) }
    var searchQuery by remember(initialTxFilter) { mutableStateOf(initialTxFilter) }

    LaunchedEffect(initialTxFilter) {
        searchQuery = initialTxFilter
        onTriggerSync()
        allTransactions = StorageUtils.getTransactions(context)
    }

    val filteredTransactions = remember(allTransactions, searchQuery) {
        if (searchQuery.isBlank()) {
            allTransactions
        } else {
            allTransactions.filter { it.txid.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    var selectedTxForEdit by remember { mutableStateOf<WalletTransaction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getStringRes(currentLanguage, "tx_history"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = activeAccentColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newValue ->
                    searchQuery = newValue
                    onFilterChanged(newValue)
                },
                label = { Text(getStringRes(currentLanguage, "filter_by_txid"), color = TextGray, fontSize = 10.sp) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeAccentColor,
                    unfocusedBorderColor = TextGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
            )

            if (searchQuery.isNotBlank()) {
                Button(
                    onClick = {
                        searchQuery = ""
                        onFilterChanged("")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(getStringRes(currentLanguage, "clear"), color = activeAccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            allTransactions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(getStringRes(currentLanguage, "no_transactions"), color = TextGray, fontSize = 13.sp)
                        Text(getStringRes(currentLanguage, "sync_appear_here"), color = TextGray.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
            filteredTransactions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(getStringRes(currentLanguage, "no_tx_with_tx"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(getStringRes(currentLanguage, "verify_txid"), color = TextGray, fontSize = 11.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(filteredTransactions) { _, tx ->
                        val isReceive = tx.type.equals("Recibido", ignoreCase = true) ||
                                tx.type.equals("Received", ignoreCase = true) ||
                                tx.type.equals("RECEIVE", ignoreCase = true)

                        val btcVal = tx.amountSats / 100_000_000.0
                        val usdVal = btcVal * tx.btcPriceUsd
                        val amountStr = String.format(Locale.US, "%s%.8f BTC", if (isReceive) "+" else "-", btcVal)
                        val lastPriceBtc = String.format(Locale.US, "%.2f USD", tx.btcPriceUsd)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val urlExplorer = if (isMainnet) {
                                        "https://blockstream.info/tx/${tx.txid}"
                                    } else {
                                        "https://blockstream.info/testnet/tx/${tx.txid}"
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlExplorer))
                                    context.startActivity(intent)
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (isReceive) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFE53935).copy(alpha = 0.2f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isReceive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (isReceive) Color(0xFF4CAF50) else Color(0xFFE53935),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = if (isReceive) getStringRes(currentLanguage, "received") else getStringRes(currentLanguage, "sent"),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = amountStr,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isReceive) Color(0xFF4CAF50) else Color.White
                                        )
                                        Text(
                                            text = String.format(Locale.US, "≈ %.2f USD", usdVal),
                                            fontSize = 12.sp,
                                            color = TextGray
                                        )
                                    }
                                }

                                Text(
                                    text = "${tx.txid.take(28)}........${tx.txid.takeLast(28)}",
                                    fontSize = 9.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Normal
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val noteText = if (!tx.note.isNullOrBlank()) {
                                        "${getStringRes(currentLanguage, "note")}: ${tx.note}"
                                    } else {
                                        "${getStringRes(currentLanguage, "note")}: -"
                                    }

                                    Text(
                                        text = noteText,
                                        fontSize = 11.sp,
                                        color = if (!tx.note.isNullOrBlank()) activeAccentColor else TextGray,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    IconButton(
                                        onClick = { selectedTxForEdit = tx },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar Nota",
                                            tint = activeAccentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lastPriceBtc,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50),
                                        textAlign = TextAlign.Start
                                    )
                                    Text(
                                        text = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date(tx.timestamp)),
                                        fontSize = 12.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTxForEdit?.let { tx ->
        var editedNote by remember { mutableStateOf(tx.note ?: "") }

        AlertDialog(
            onDismissRequest = { selectedTxForEdit = null },
            containerColor = CardBackground,
            title = {
                Text(
                    text = if (currentLanguage == "en") "Edit Transaction Note" else "Editar nota de transacción",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (currentLanguage == "en") "Enter a new note for this transaction:" else "Ingrese una nueva nota para esta transacción:",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = editedNote,
                        onValueChange = { editedNote = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeAccentColor,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        StorageUtils.updateTransactionNote(context, tx.txid, editedNote)
                        allTransactions = StorageUtils.getTransactions(context)
                        selectedTxForEdit = null
                    }
                ) {
                    Text(
                        text = if (currentLanguage == "en") "Save" else "Guardar",
                        color = activeAccentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedTxForEdit = null }
                ) {
                    Text(
                        text = if (currentLanguage == "en") "Cancel" else "Cancelar",
                        color = TextGray
                    )
                }
            }
        )
    }
}

@Composable
private fun PortfolioScreenContent(
    currentBalanceSatoshis: Long,
    globalBtcPriceUsd: Double,
    activeAccentColor: Color,
    currentLanguage: String,
    isMainnet: Boolean
) {
    val context = LocalContext.current
    var allTransactions by remember { mutableStateOf(StorageUtils.getTransactions(context)) }

    var contactsList by remember { mutableStateOf(AddressBookManager.getContacts(context, isMainnet)) }
    val savedAddresses = StorageUtils.getAddresses(context)
    val targetPrefix = if (isMainnet) "bc1q" else "tb1q"
    val currentAddresses = remember(savedAddresses, isMainnet) {
        savedAddresses.filter { it.startsWith(targetPrefix) }
    }

    val portfolioBtc = currentBalanceSatoshis / 100_000_000.0
    val currentPortfolioUsdX = portfolioBtc * globalBtcPriceUsd
    val currentPortfolioUsd = BigDecimal(currentPortfolioUsdX).setScale(2, RoundingMode.HALF_UP).toDouble()

    data class AddressGroup(
        val address: String,
        val totalSats: Long,
        val lastBuyPrice: Double,
        val lastTimestamp: Long,
        val sortIndex: Int
    )

    val addressGroups = remember(currentAddresses, allTransactions, globalBtcPriceUsd) {
        currentAddresses.mapIndexed { index, addr ->
            val finalSats = getCachedBalance(context, addr)

            val txsForAddress = allTransactions.filter { tx -> tx.address.equals(addr, ignoreCase = true) }
            val latestTx = txsForAddress.maxByOrNull { tx -> tx.timestamp }

            AddressGroup(
                address = addr,
                totalSats = finalSats,
                lastBuyPrice = latestTx?.btcPriceUsd ?: globalBtcPriceUsd,
                lastTimestamp = latestTx?.timestamp ?: 0L,
                sortIndex = index + 1
            )
        }
    }

    val receivedTransactions = remember(allTransactions) {
        allTransactions.filter { tx ->
            tx.type.equals("Recibido", true) || tx.type.equals("Received", true) || tx.type.equals("RECEIVE", true)
        }
    }
    val sentTransactions = remember(allTransactions) {
        allTransactions.filter { tx ->
            tx.type.equals("Enviado", true) || tx.type.equals("Sent", true) || tx.type.equals("SEND", true)
        }
    }
    val totalReceivedSats = remember(receivedTransactions) { receivedTransactions.sumOf { it.amountSats } }
    val totalSentSats = remember(sentTransactions) { sentTransactions.sumOf { it.amountSats } }

    val address = ""
    val weightedAverageCostData = remember(allTransactions, address) {
        val receivedTxs = allTransactions.filter { tx ->
            val isReceive = tx.type.equals("Recibido", true) ||
                    tx.type.equals("Received", true) ||
                    tx.type.equals("RECEIVE", true)
            val matchesAddress = address.isEmpty() || tx.address.equals(address, true)
            isReceive && matchesAddress
        }
        var totalSatoshis: Long = 0
        var totalCostUSD: Double = 0.0
        for (tx in receivedTxs) {
            val sats = tx.amountSats
            val priceAtTx = tx.btcPriceUsd

            totalSatoshis += sats
            totalCostUSD += (sats.toDouble() / 100_000_000.0) * priceAtTx
        }
        val totalBtc = totalSatoshis.toDouble() / 100_000_000.0
        val averageCostPerBtc = if (totalBtc > 0) totalCostUSD / totalBtc else 0.0
        Triple(totalSatoshis, totalCostUSD, averageCostPerBtc)
    }
    val (totalSats, totalCostUSD, avgCostPerBtcX) = weightedAverageCostData

    val avgCostPerBtc = BigDecimal(avgCostPerBtcX).setScale(2, RoundingMode.HALF_UP).toDouble()
    val totalAmountSumX = portfolioBtc * avgCostPerBtc
    val totalAmountSum = BigDecimal(totalAmountSumX).setScale(2, RoundingMode.HALF_UP).toDouble()
    val totalProfitX = currentPortfolioUsd - totalAmountSum
    val totalProfit = BigDecimal(totalProfitX).setScale(2, RoundingMode.HALF_UP).toDouble()
    val isTotalPositiveA = totalProfit >= 0
    val totalSignPrefixA = if (isTotalPositiveA) "+" else ""
    val totalProfitTextA = String.format(Locale.US, "%s%.2f USD", totalSignPrefixA, totalProfit)
    val totalProfitColorA = if (isTotalPositiveA) Color(0xFF4CAF50) else Color(0xFFE53935)

    val totalPercentageProfit = if (totalAmountSum != 0.0) {
        try {
            val totalPercentageProfitX = (totalProfit / currentPortfolioUsd) * 100
            BigDecimal(totalPercentageProfitX).setScale(2, RoundingMode.HALF_UP).toDouble()
        } catch (e: Exception) {
            0.0
        }
    } else {
        0.0
    }

    val isTotalPositiveP = totalPercentageProfit >= 0
    val totalSignPrefixP = if (isTotalPositiveP) "+" else ""
    val totalPercentageProfitText = String.format(Locale.US, " (%s%.2f%%)", totalSignPrefixP, totalPercentageProfit)
    val totalPercentageProfitColor = if (isTotalPositiveP) Color(0xFF4CAF50) else Color(0xFFE53935)

    var selectedAddressForEdit by remember { mutableStateOf<String?>(null) }
    val textTotal = " 💼"
    val textCompra = " 🛒"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = activeAccentColor
                )
                Text(
                    text = getStringRes(currentLanguage, "portfolio"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = getStringRes(currentLanguage, "total_wallet_value"),
                    fontSize = 12.sp,
                    color = TextGray,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US,getStringRes(currentLanguage, "bitcoin_price") + "%.2f USD", globalBtcPriceUsd)}\n${String.format(Locale.US,getStringRes(currentLanguage, "bitcoin_price_last") + "%.2f USD", avgCostPerBtc)}",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$textTotal ${String.format(Locale.US, "%.2f USD", currentPortfolioUsd)}\n$textCompra ${String.format(Locale.US, "%.2f USD", totalAmountSum)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = totalProfitTextA,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = totalProfitColorA,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = String.format(Locale.US, "≈ %.8f BTC 📥", totalReceivedSats / 100_000_000.0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Text(
                            text = String.format(Locale.US, "≈ %.8f BTC 📤", totalSentSats / 100_000_000.0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Text(
                            text = String.format(Locale.US, "≈ %.8f BTC 💼", portfolioBtc),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = totalPercentageProfitText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = totalPercentageProfitColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (currentLanguage == "en") "Portfolio Holdings" else "Carteras del Portafolio",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (addressGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getStringRes(currentLanguage, "no_entries_registered"),
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        } else {
            addressGroups.forEach { group ->
                val groupBtcVal = group.totalSats / 100_000_000.0
                val currentGroupLastUsdValueX = groupBtcVal * avgCostPerBtc
                val currentGroupLastUsdValue = BigDecimal(currentGroupLastUsdValueX).setScale(2, RoundingMode.HALF_UP).toDouble()
                val currentGroupUsdValueX = groupBtcVal * globalBtcPriceUsd
                val currentGroupUsdValue = BigDecimal(currentGroupUsdValueX).setScale(2, RoundingMode.HALF_UP).toDouble()

                val profitChangeX = currentGroupUsdValue - currentGroupLastUsdValue
                val profitChange = BigDecimal(profitChangeX).setScale(2, RoundingMode.HALF_UP).toDouble()
                val isPositive = profitChange >= 0
                val signPrefix = if (isPositive) "+" else ""
                val profitChangeText = String.format(Locale.US, " %s%.2f USD", signPrefix, profitChange)

                val percentajeProfit = if (currentGroupUsdValue != 0.0) {
                    try {
                        val percentajeProfitX = ((currentGroupUsdValue - currentGroupLastUsdValue) / currentGroupUsdValue) * 100
                        BigDecimal(percentajeProfitX).setScale(2, RoundingMode.HALF_UP).toDouble()
                    } catch (e: Exception) {
                        0.0
                    }
                } else {
                    0.0
                }

                val isPositiveA = percentajeProfit >= 0
                val signPrefixA = if (isPositiveA) "+" else ""
                val percentajeProfitText = String.format(Locale.US, " (%s%.2f%%)", signPrefixA, percentajeProfit)

                val usdValueText = "$textTotal ${String.format(Locale.US, "%.2f", globalBtcPriceUsd)} ${String.format(Locale.US, "≈ %.2f USD", currentGroupUsdValue)}\n$textCompra ${String.format(Locale.US, "%.2f", avgCostPerBtc)} ${String.format(Locale.US, "≈ %.2f USD", currentGroupLastUsdValue)}"

                val contactMatch = contactsList.find { it.address.equals(group.address, ignoreCase = true) }
                val customName = contactMatch?.name?.takeIf { it.isNotBlank() }

                val titleLabel = customName ?: if (currentLanguage == "en") "Wallet #${group.sortIndex}" else "Cartera #${group.sortIndex}"
                val addressSubtitle = group.address

                PortfolioItemCard(
                    title = titleLabel,
                    address = addressSubtitle,
                    btcAmount = String.format(Locale.US, "%.8f BTC", groupBtcVal),
                    usdValueText = usdValueText,
                    profitChangeText = profitChangeText,
                    percentajeProfitText = percentajeProfitText,
                    isPositive = isPositive,
                    isPositivea = isPositiveA,
                    buyPrice = if (group.lastTimestamp > 0L) java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date(group.lastTimestamp)) else "-",
                    accentColor = activeAccentColor,
                    currentLanguage = currentLanguage,
                    onEditClick = {
                        selectedAddressForEdit = group.address
                    }
                )
            }
        }
    }

    selectedAddressForEdit?.let { targetAddress ->
        val existingContact = contactsList.find { it.address.equals(targetAddress, ignoreCase = true) }
        var editedLabel by remember { mutableStateOf(existingContact?.name ?: "") }

        AlertDialog(
            onDismissRequest = { selectedAddressForEdit = null },
            containerColor = CardBackground,
            title = {
                Text(
                    text = if (currentLanguage == "en") "Edit Address Label" else "Editar etiqueta de dirección",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (currentLanguage == "en") "Enter a label for address ${targetAddress.take(8)}..." else "Ingrese una etiqueta para la dirección ${targetAddress.take(8)}...",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = editedLabel,
                        onValueChange = { editedLabel = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeAccentColor,
                            unfocusedBorderColor = TextGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AddressBookManager.saveContact(context, editedLabel.trim(), targetAddress, isMainnet)
                        contactsList = AddressBookManager.getContacts(context, isMainnet)
                        selectedAddressForEdit = null
                    }
                ) {
                    Text(
                        text = if (currentLanguage == "en") "Save" else "Guardar",
                        color = activeAccentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedAddressForEdit = null }
                ) {
                    Text(
                        text = if (currentLanguage == "en") "Cancel" else "Cancelar",
                        color = TextGray
                    )
                }
            }
        )
    }
}

@Composable
private fun PortfolioItemCard(
    title: String,
    address: String,
    btcAmount: String,
    usdValueText: String,
    profitChangeText: String,
    percentajeProfitText: String,
    isPositive: Boolean,
    isPositivea: Boolean,
    buyPrice: String,
    accentColor: Color,
    currentLanguage: String,
    onEditClick: () -> Unit
) {
    val profitChangeTextColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFE53935)
    val percentajeProfitTextColor = if (isPositivea) Color(0xFF4CAF50) else Color(0xFFE53935)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (currentLanguage == "en") "Edit Label" else "Editar Etiqueta",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

            }

            Text(
                text = address,
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.Normal
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${getStringRes(currentLanguage, "current_price_label")}: ${buyPrice}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = btcAmount,
                    fontSize = 12.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = profitChangeText,
                        fontSize = 12.sp,
                        color = profitChangeTextColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = usdValueText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Start
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = percentajeProfitText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = percentajeProfitTextColor,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}