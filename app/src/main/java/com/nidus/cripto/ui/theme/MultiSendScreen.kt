package com.nidus.cripto.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nidus.cripto.BiometricHelper
import com.nidus.cripto.RealWalletManager
import com.nidus.cripto.SendViewModel
import com.nidus.cripto.Recipient
import com.nidus.cripto.SendUiState
import com.nidus.cripto.StorageUtils
import com.nidus.cripto.TransactionManager
import com.nidus.cripto.UnspentOutput
import com.nidus.cripto.contacts.AddressBookManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

private val GoldAccent = Color(0xFFFFD700)
private val DarkBackground = Color(0xFF121212)
private val CardBackground = Color(0xFF1E1E1E)
private val TextGray = Color(0xFFA0A0A0)

private fun getStringRes(lang: String, key: String): String {
    return when (lang) {
        "en" -> when (key) {
            "multi_payment_mass" -> "Multi-payment (Mass Send)"
            "send_simple" -> "Send (Simple)"
            "process_multi_payment" -> "PROCESS MULTI-PAYMENT"
            "process_send" -> "PROCESS SEND"
            "add_recipients_verify" -> "Add recipients and verify totals."
            "recipient" -> "Recipient"
            "address_book" -> "Address Book"
            "delete" -> "Delete"
            "btc_address" -> "BTC Address"
            "paste" -> "Paste"
            "scan" -> "Scan"
            "amount_btc" -> "Amount (BTC)"
            "internal_note_optional" -> "Internal Note (Optional)"
            "empty_wallet_max" -> "Empty Wallet (Max)"
            "wallet_emptied" -> "Wallet emptied"
            "insufficient_balance_fees" -> "Insufficient balance to cover fees"
            "add_another_recipient" -> "Add another recipient"
            "summary_and_fees" -> "Summary and Fees"
            "expand_summary" -> "Expand summary"
            "sum" -> "Sum:"
            "mining" -> "Mining"
            "app_fee" -> "App Fee $0.25:"
            "omitted" -> "(Omitted)"
            "total_btc" -> "Total (BTC):"
            "total_usd" -> "Total (USD):"
            "complete_valid_addresses" -> "Complete valid addresses and amounts"
            "successful_transaction" -> "Successful Transaction!"
            "transaction_signed_broadcasted" -> "The transaction was successfully signed and broadcasted."
            "txid_copied" -> "TXID copied"
            "transaction_details" -> "Transaction Details:"
            "type" -> "Type:"
            "multi_payment_records" -> "Multi-payment"
            "simple_send_record" -> "Simple Send (1 record)"
            "total_sent_amount" -> "Total Sent Amount:"
            "mining_fee" -> "Mining Fee:"
            "general_total" -> "General Total:"
            "go_to_transaction" -> "Go to Transaction"
            "network_issues" -> "Network Issues"
            "issue_details" -> "Issue Details:"
            "issue_details_copied" -> "Issue details copied"
            "copy" -> "Copy"
            "understood" -> "Understood"
            "select_contact" -> "Select Contact"
            "no_saved_contacts" -> "No saved contacts."
            "cancel" -> "Cancel"
            "enter_pin_title" -> "Security PIN Required"
            "enter_pin_subtitle" -> "Enter your 6-digit PIN to authorize the transaction"
            "incorrect_pin" -> "Incorrect PIN"
            "app_name_brand" -> "NIDUS CRYPTO"
            "bio_verif_title" -> "Biometric Verification"
            "bio_verif_subtitle" -> "Scan your fingerprint to complete the transaction"
            "bitcoin_price" -> "Price: "
            else -> key
        }
        else -> when (key) {
            "multi_payment_mass" -> "Multi-pago (Envío Masivo)"
            "send_simple" -> "Envío (Simple)"
            "process_multi_payment" -> "PROCESAR MULTI-PAGO"
            "process_send" -> "PROCESAR ENVÍO"
            "add_recipients_verify" -> "Agregue destinatarios y verifique los totales."
            "recipient" -> "Destinatario"
            "address_book" -> "Libreta"
            "delete" -> "Eliminar"
            "btc_address" -> "Dirección BTC"
            "paste" -> "Pegar"
            "scan" -> "Escanear"
            "amount_btc" -> "Monto (BTC)"
            "internal_note_optional" -> "Nota interna (Opcional)"
            "empty_wallet_max" -> "Vaciar Billetera (Max)"
            "wallet_emptied" -> "Billetera vaciada"
            "insufficient_balance_fees" -> "Saldo insuficiente para cubrir las comisiones"
            "add_another_recipient" -> "Agregar otro destinatario"
            "summary_and_fees" -> "Resumen y Comisiones"
            "expand_summary" -> "Desplegar resumen"
            "sum" -> "Suma:"
            "mining" -> "Minería"
            "app_fee" -> "App Fee $0.25:"
            "omitted" -> "(Omitida)"
            "total_btc" -> "Total (BTC):"
            "total_usd" -> "Total (USD):"
            "complete_valid_addresses" -> "Complete las direcciones y montos válidos"
            "successful_transaction" -> "¡Transacción Exitosa!"
            "transaction_signed_broadcasted" -> "La transacción se firmó y difundió con éxito."
            "txid_copied" -> "TXID copiado"
            "transaction_details" -> "Detalle de la Transacción:"
            "type" -> "Tipo:"
            "multi_payment_records" -> "Multi-pago"
            "simple_send_record" -> "Envío Simple (1 registro)"
            "total_sent_amount" -> "Monto Total Enviado:"
            "mining_fee" -> "Fee de Minería:"
            "general_total" -> "Total General:"
            "go_to_transaction" -> "Ir a Transacción"
            "network_issues" -> "Problemas con la red"
            "issue_details" -> "Detalle del inconveniente:"
            "issue_details_copied" -> "Detalle del inconveniente copiado"
            "copy" -> "Copiar"
            "understood" -> "Entendido"
            "select_contact" -> "Seleccionar Contacto"
            "no_saved_contacts" -> "No hay contactos guardados."
            "cancel" -> "Cancelar"
            "enter_pin_title" -> "PIN de Seguridad Requerido"
            "enter_pin_subtitle" -> "Ingrese su PIN de 6 dígitos para autorizar la transacción"
            "incorrect_pin" -> "PIN Incorrecto"
            "app_name_brand" -> "NIDUS CRIPTO"
            "bio_verif_title" -> "Verificación Biométrica"
            "bio_verif_subtitle" -> "Escanee su huella para completar la transacción"
            "bitcoin_price" -> "Precio: "
            else -> key
        }
    }
}

data class RecipientInput(
    val address: String = "",
    val amountBtc: String = "",
    val note: String = ""
)

data class ParsedBtcUri(
    val address: String,
    val amountBtc: String? = null,
    val note: String? = null
)

fun parseBitcoinUri(input: String): ParsedBtcUri {
    val cleanInput = input.trim()

    if (!cleanInput.startsWith("bitcoin:", ignoreCase = true)) {
        return ParsedBtcUri(address = cleanInput)
    }

    return try {
        val withoutScheme = cleanInput.substring(8)
        val address = withoutScheme.substringBefore("?")

        var amount: String? = null
        var note: String? = null

        if (withoutScheme.contains("?")) {
            val queryString = withoutScheme.substringAfter("?")
            val pairs = queryString.split("&")

            for (pair in pairs) {
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].lowercase()
                    val rawValue = parts[1]
                    val decodedValue = try {
                        URLDecoder.decode(rawValue, StandardCharsets.UTF_8.name())
                    } catch (e: Exception) {
                        rawValue
                    }

                    when (key) {
                        "amount" -> amount = decodedValue
                        "message", "label", "memo" -> {
                            if (note == null) note = decodedValue
                        }
                    }
                }
            }
        }

        ParsedBtcUri(
            address = address,
            amountBtc = amount,
            note = note
        )
    } catch (e: Exception) {
        ParsedBtcUri(address = cleanInput)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSendScreen(
    isMainnet: Boolean,
    availableBalanceSats: Long,
    feeRateSatPerVb: Int = 10,
    initialScannedData: String? = null,
    onInitialDataConsumed: () -> Unit = {},
    userPrivateKeyWif: String = "",
    userUtxos: List<UnspentOutput> = emptyList(),
    btcPriceUsd: Double = 0.0,
    myChangeAddress: String = "",
    viewModel: SendViewModel = viewModel(),
    onNavigateToTransaction: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(StorageUtils.getLanguage(context)) }
    val isNetworkAvailable by rememberIsNetworkAvailable()

    val recipients = remember { mutableStateListOf(RecipientInput()) }
    val uiState by viewModel.uiState.collectAsState()

    var savedContacts by remember(isMainnet) {
        mutableStateOf(AddressBookManager.getContacts(context, isMainnet))
    }

    var selectedRecipientIndexForContact by remember { mutableStateOf<Int?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var activeQrTargetIndex by remember { mutableStateOf<Int?>(null) }
    var isSummaryExpanded by remember { mutableStateOf(false) }

    var showPinScreen by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    val activeAccentColor = if (isMainnet) GoldAccent else Color.Magenta
    val isMulti = recipients.size > 1

    val screenTitle = if (isMulti) getStringRes(currentLanguage, "multi_payment_mass") else getStringRes(currentLanguage, "send_simple")
    val actionButtonText = if (isMulti) getStringRes(currentLanguage, "process_multi_payment") else getStringRes(currentLanguage, "process_send")

    val effectiveUtxos = userUtxos

    val isAppFeeAddressOwnedByWallet = remember(effectiveUtxos, isMainnet) {
        try {
            val mnemonic = StorageUtils.getMnemonic(context) ?: ""
            if (mnemonic.isNotBlank()) {
                (0..5).any { idx ->
                    val derivedAddr = RealWalletManager.deriveAddressFromMnemonic(mnemonic, addressIndex = idx, isMainnet = isMainnet)
                    if(isMainnet) {
                        derivedAddr.equals(TransactionManager.APP_FEE_ADDRESS_MAINNET, ignoreCase = true)
                    }else{
                        derivedAddr.equals(TransactionManager.APP_FEE_ADDRESS_TESTNET, ignoreCase = true)
                    }
                }
            } else false
        } catch (e: Exception) {
            false
        }
    }

    var globalBtcPriceUsd by remember {
        mutableStateOf(parseRobustDouble(StorageUtils.getLastBtcPrice(context) ?: "0.0"))
    }

    var btcGlobalPriceText by remember {
        mutableStateOf(
            if (globalBtcPriceUsd != 0.00) "$globalBtcPriceUsd USD" else " 0.00 USD"
        )
    }

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

    val totalAmountsBtc = recipients.sumOf { it.amountBtc.toDoubleOrNull() ?: 0.0 }

    val estimatedVBytes = 10 + (maxOf(1, effectiveUtxos.size) * 68) + ((recipients.size + 1) * 31)
    val miningFeeSats = estimatedVBytes * feeRateSatPerVb
    val miningFeeBtc = miningFeeSats / 100_000_000.0

    val appFeeUsd = if (isAppFeeAddressOwnedByWallet) 0.0 else TransactionManager.APP_FEE_USD
    val appFeeBtc = if (globalBtcPriceUsd > 0.0) appFeeUsd / globalBtcPriceUsd else 0.0

    val totalBalanceSats = effectiveUtxos.sumOf { it.valueSats }
    val currentTotalSats = (totalAmountsBtc * 100_000_000).toLong() + miningFeeSats + (appFeeBtc * 100_000_000).toLong()

    val totalBtcSatsFinal = if (totalBalanceSats > 0 && kotlin.math.abs(totalBalanceSats - currentTotalSats) < 500) {
        totalBalanceSats
    } else {
        currentTotalSats
    }

    val totalBtcFinal = totalBtcSatsFinal / 100_000_000.0
    val totalFiatFinal = totalBtcFinal * globalBtcPriceUsd

    fun updateRecipientFromParsed(index: Int, parsed: ParsedBtcUri) {
        if (index in recipients.indices) {
            val current = recipients[index]
            recipients[index] = current.copy(
                address = parsed.address,
                amountBtc = parsed.amountBtc ?: current.amountBtc,
                note = parsed.note ?: current.note
            )
        }
    }

    LaunchedEffect(initialScannedData) {
        if (!initialScannedData.isNullOrEmpty()) {
            val parsed = parseBitcoinUri(initialScannedData)
            if (recipients.isEmpty()) {
                recipients.add(RecipientInput())
            }
            updateRecipientFromParsed(0, parsed)
            onInitialDataConsumed()
        }
    }

    val executeSendTransaction = {
        val mnemonic = StorageUtils.getMnemonic(context) ?: ""
        val mappedRecipients = recipients.map {
            val cleanAddr = it.address.trim()
            val sats = (it.amountBtc.toDouble() * 100_000_000).toLong()
            Recipient(address = cleanAddr, amountSats = sats)
        }
        val recipientNotes = recipients.map { it.note }

        viewModel.processAndSendTransaction(
            recipients = mappedRecipients,
            notes = recipientNotes,
            utxos = effectiveUtxos,
            seedMnemonic = mnemonic,
            fallbackWif = userPrivateKeyWif,
            isMainnet = isMainnet,
            btcPriceUsd = globalBtcPriceUsd,
            changeAddress = myChangeAddress,
            feeRateSatPerVb = feeRateSatPerVb
        )
    }

    val handlePostPinSecurityAndSend = {
        val fragmentActivity = context.findFragmentActivity()
        val isBioEnabled = StorageUtils.isBiometricEnabled(context)

        if (isBioEnabled && BiometricHelper.isBiometricAvailable(context) && fragmentActivity != null) {
            BiometricHelper.showBiometricPrompt(
                activity = fragmentActivity,
                title = getStringRes(currentLanguage, "bio_verif_title"),
                subtitle = getStringRes(currentLanguage, "bio_verif_subtitle"),
                onSuccess = {
                    executeSendTransaction()
                },
                onError = { _ ->
                    Toast.makeText(context, "Verificación biométrica cancelada", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            executeSendTransaction()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = screenTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeAccentColor
                    )
                    Text(
                        text = getStringRes(currentLanguage, "add_recipients_verify"),
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }

            items(recipients.indices.toList()) { index ->
                val recipient = recipients[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${getStringRes(currentLanguage, "recipient")} #${index + 1}",
                                color = activeAccentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        savedContacts = AddressBookManager.getContacts(context, isMainnet)
                                        selectedRecipientIndexForContact = index
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContactPage,
                                        contentDescription = getStringRes(currentLanguage, "address_book"),
                                        tint = activeAccentColor
                                    )
                                }

                                if (recipients.size > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { recipients.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = getStringRes(currentLanguage, "delete"),
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = recipient.address,
                            onValueChange = { input ->
                                if (input.startsWith("bitcoin:", ignoreCase = true)) {
                                    val parsed = parseBitcoinUri(input)
                                    updateRecipientFromParsed(index, parsed)
                                } else {
                                    recipients[index] = recipient.copy(address = input.trim())
                                }
                            },
                            label = { Text(getStringRes(currentLanguage, "btc_address"), color = TextGray, fontSize = 10.sp) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clipData = clipboard.primaryClip
                                            if (clipData != null && clipData.itemCount > 0) {
                                                val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                                                if (pastedText.isNotEmpty()) {
                                                    val parsed = parseBitcoinUri(pastedText)
                                                    updateRecipientFromParsed(index, parsed)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = getStringRes(currentLanguage, "paste"), tint = activeAccentColor, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            activeQrTargetIndex = index
                                            showScanner = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = getStringRes(currentLanguage, "scan"), tint = activeAccentColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeAccentColor,
                                unfocusedBorderColor = TextGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val itemBtc = recipient.amountBtc.toDoubleOrNull() ?: 0.0
                            val itemFiat = itemBtc * globalBtcPriceUsd

                            OutlinedTextField(
                                value = recipient.amountBtc,
                                onValueChange = { newValue ->
                                    val formatted = newValue.replace(',', '.')
                                    if (formatted.isEmpty() || formatted.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        recipients[index] = recipient.copy(amountBtc = formatted)
                                    }
                                },
                                label = { Text(getStringRes(currentLanguage, "amount_btc"), color = TextGray, fontSize = 10.sp) },
                                trailingIcon = {
                                    Text(
                                        text = String.format("≈ $%.2f", itemFiat),
                                        color = activeAccentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeAccentColor,
                                    unfocusedBorderColor = TextGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 52.dp)
                            )
                        }

                        OutlinedTextField(
                            value = recipient.note,
                            onValueChange = { newNote -> recipients[index] = recipient.copy(note = newNote) },
                            label = { Text(getStringRes(currentLanguage, "internal_note_optional"), color = TextGray, fontSize = 10.sp) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeAccentColor,
                                unfocusedBorderColor = TextGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp)
                        )
                    }
                }
            }

            if (recipients.size == 1) {
                item {
                    Button(
                        onClick = {
                            val totalBalanceSats = effectiveUtxos.sumOf { it.valueSats }
                            val extraOutputForAppFee = if (appFeeBtc > 0.0) 1 else 0
                            val totalOutputsCount = recipients.size + extraOutputForAppFee
                            val estimatedVBytes = (effectiveUtxos.size * 68) + (totalOutputsCount * 31) + 10 + 30
                            val totalFeeSats = estimatedVBytes * feeRateSatPerVb
                            val availableForSendSats = totalBalanceSats - totalFeeSats - (appFeeBtc * 100_000_000).toLong()

                            if (availableForSendSats > 0) {
                                val btcStr = "%.8f".format(availableForSendSats / 100_000_000.0).replace(',', '.')
                                recipients[0] = recipients[0].copy(amountBtc = btcStr)
                                Toast.makeText(context, getStringRes(currentLanguage, "wallet_emptied"), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, getStringRes(currentLanguage, "insufficient_balance_fees"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(getStringRes(currentLanguage, "empty_wallet_max"), color = activeAccentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            item {
                Button(
                    onClick = { recipients.add(RecipientInput()) },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = activeAccentColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(getStringRes(currentLanguage, "add_another_recipient"), color = activeAccentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSummaryExpanded = !isSummaryExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getStringRes(currentLanguage, "summary_and_fees"),
                            color = activeAccentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = getStringRes(currentLanguage, "bitcoin_price") + btcGlobalPriceText,
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { isSummaryExpanded = !isSummaryExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isSummaryExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = getStringRes(currentLanguage, "expand_summary"),
                                tint = activeAccentColor
                            )
                        }
                    }

                    if (isSummaryExpanded) {
                        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 1.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(getStringRes(currentLanguage, "sum"), color = TextGray, fontSize = 10.sp)
                            Text("%.8f BTC".format(totalAmountsBtc), color = Color.White, fontSize = 10.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${getStringRes(currentLanguage, "mining")} ($feeRateSatPerVb sat/vB):", color = TextGray, fontSize = 10.sp)
                            Text("%.8f BTC".format(miningFeeBtc), color = Color.White, fontSize = 10.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(getStringRes(currentLanguage, "app_fee"), color = TextGray, fontSize = 10.sp)
                            Text(if (isAppFeeAddressOwnedByWallet) "0.00000000 BTC ${getStringRes(currentLanguage, "omitted")}" else "%.8f BTC".format(appFeeBtc), color = Color.White, fontSize = 10.sp)
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 1.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(getStringRes(currentLanguage, "total_btc"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("%.8f BTC".format(totalBtcFinal), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(getStringRes(currentLanguage, "total_usd"), color = activeAccentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("≈ $%,.2f USD".format(totalFiatFinal), color = activeAccentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Button(
                onClick = {
                    val invalid = recipients.any {
                        val cleanAddr = it.address.trim()
                        cleanAddr.isEmpty() || it.amountBtc.toDoubleOrNull() == null || it.amountBtc.toDouble() <= 0
                    }
                    if (invalid) {
                        Toast.makeText(context, getStringRes(currentLanguage, "complete_valid_addresses"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val mnemonic = StorageUtils.getMnemonic(context) ?: ""
                    if (mnemonic.isBlank() && userPrivateKeyWif.isBlank()) {
                        return@Button
                    }

                    pinInput = ""
                    showPinScreen = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = uiState !is SendUiState.Loading
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = actionButtonText,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showPinScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .zIndex(10f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = getStringRes(currentLanguage, "app_name_brand"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeAccentColor,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = getStringRes(currentLanguage, "enter_pin_title"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getStringRes(currentLanguage, "enter_pin_subtitle"),
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    for (i in 0 until 6) {
                        val isFilled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(
                                    width = if (isFilled) 2.dp else 1.dp,
                                    color = if (isFilled) activeAccentColor else Color.Gray,
                                    shape = CircleShape
                                )
                                .background(
                                    color = if (isFilled) activeAccentColor else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
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
                        listOf("", "0", "DEL")
                    )

                    for (row in buttons) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            for (btn in row) {
                                if (btn.isEmpty()) {
                                    Spacer(modifier = Modifier.size(65.dp))
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = CardBackground,
                                        modifier = Modifier
                                            .size(65.dp)
                                            .clickable {
                                                if (btn == "DEL") {
                                                    if (pinInput.isNotEmpty()) {
                                                        pinInput = pinInput.dropLast(1)
                                                    }
                                                } else {
                                                    if (pinInput.length < 6) {
                                                        pinInput += btn
                                                        if (pinInput.length == 6) {
                                                            val savedPin = StorageUtils.getSavedPin(context) ?: ""
                                                            if (pinInput == savedPin) {
                                                                showPinScreen = false
                                                                handlePostPinSecurityAndSend()
                                                            } else {
                                                                Toast.makeText(context, getStringRes(currentLanguage, "incorrect_pin"), Toast.LENGTH_SHORT).show()
                                                                pinInput = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (btn == "DEL") {
                                                Icon(
                                                    imageVector = Icons.Default.Backspace,
                                                    contentDescription = "Delete",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = btn,
                                                    color = Color.White,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { showPinScreen = false },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(getStringRes(currentLanguage, "cancel"), color = TextGray, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    when (val state = uiState) {
        is SendUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = activeAccentColor)
                        Text(text = state.message, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        is SendUiState.Success -> {
            AlertDialog(
                onDismissRequest = {
                    viewModel.resetState()
                    recipients.clear()
                    recipients.add(RecipientInput())
                },
                containerColor = CardBackground,
                title = { Text(getStringRes(currentLanguage, "successful_transaction"), color = activeAccentColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(getStringRes(currentLanguage, "transaction_signed_broadcasted"), color = Color.White, fontSize = 12.sp)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("TXID:", color = activeAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    TextButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("TXID", state.txid))
                                            Toast.makeText(context, getStringRes(currentLanguage, "txid_copied"), Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = activeAccentColor, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Text(
                                    text = state.txid,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(getStringRes(currentLanguage, "transaction_details"), color = activeAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                                if (recipients.size > 1) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(getStringRes(currentLanguage, "type"), color = TextGray, fontSize = 10.sp)
                                        Text("${getStringRes(currentLanguage, "multi_payment_records")} (${recipients.size})", color = Color.White, fontSize = 10.sp)
                                    }
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(getStringRes(currentLanguage, "type"), color = TextGray, fontSize = 10.sp)
                                        Text(getStringRes(currentLanguage, "simple_send_record"), color = Color.White, fontSize = 10.sp)
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(getStringRes(currentLanguage, "total_sent_amount"), color = TextGray, fontSize = 10.sp)
                                    Text("%.8f BTC".format(totalAmountsBtc), color = Color.White, fontSize = 10.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(getStringRes(currentLanguage, "mining_fee"), color = TextGray, fontSize = 10.sp)
                                    Text("%.8f BTC".format(miningFeeBtc), color = Color.White, fontSize = 10.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(getStringRes(currentLanguage, "app_fee"), color = TextGray, fontSize = 10.sp)
                                    Text("%.8f BTC".format(appFeeBtc), color = Color.White, fontSize = 10.sp)
                                }
                                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(getStringRes(currentLanguage, "general_total"), color = activeAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("%.8f BTC (≈ $%,.2f)".format(totalBtcFinal, totalFiatFinal), color = activeAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetState()
                            recipients.clear()
                            recipients.add(RecipientInput())
                            onNavigateToTransaction(state.txid)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(getStringRes(currentLanguage, "go_to_transaction"), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        is SendUiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetState() },
                containerColor = CardBackground,
                title = { Text(getStringRes(currentLanguage, "network_issues"), color = Color.Red, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(getStringRes(currentLanguage, "issue_details"), color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    TextButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Error", state.message))
                                            Toast.makeText(context, getStringRes(currentLanguage, "issue_details_copied"), Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(getStringRes(currentLanguage, "copy"), color = Color.Red, fontSize = 10.sp)
                                    }
                                }
                                Text(
                                    text = state.message,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetState() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(getStringRes(currentLanguage, "understood"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        is SendUiState.Idle -> {}
    }

    if (showScanner) {
        PremiumScannerDialog(
            accentColor = activeAccentColor,
            onQrScanned = { rawScanned ->
                activeQrTargetIndex?.let { index ->
                    val parsed = parseBitcoinUri(rawScanned)
                    updateRecipientFromParsed(index, parsed)
                }
                showScanner = false
            },
            onDismiss = { showScanner = false }
        )
    }

    selectedRecipientIndexForContact?.let { targetIndex ->
        AlertDialog(
            onDismissRequest = { selectedRecipientIndexForContact = null },
            containerColor = CardBackground,
            title = { Text(getStringRes(currentLanguage, "select_contact"), color = activeAccentColor, fontWeight = FontWeight.Bold) },
            text = {
                if (savedContacts.isEmpty()) {
                    Text(getStringRes(currentLanguage, "no_saved_contacts"), color = TextGray)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(savedContacts) { contact ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val parsed = parseBitcoinUri(contact.address)
                                        updateRecipientFromParsed(targetIndex, parsed)
                                        selectedRecipientIndexForContact = null
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(contact.address, color = TextGray, fontSize = 10.sp, maxLines = 1)
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = Color.DarkGray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRecipientIndexForContact = null }) {
                    Text(getStringRes(currentLanguage, "cancel"), color = activeAccentColor)
                }
            }
        )
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