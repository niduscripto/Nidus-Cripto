package com.nidus.cripto.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.nidus.cripto.BiometricHelper
import com.nidus.cripto.StorageUtils
import com.nidus.cripto.contacts.AddressBookManager
import kotlin.system.exitProcess
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.text.input.KeyboardType

private val DarkBackground = Color(0xFF0D0D0D)
private val CardBackground = Color(0xFF1E1E1E)
private val ErrorRed = Color(0xFFCF6679)
private val TextGray = Color(0xFFA0A0A0)

enum class ExportStep {
    OPTIONS,
    PIN_VERIFICATION,
    SHOW_SEED,
    ADDRESS_BOOK,
    IMPORT_WALLET,
    MODIFY_PIN,
    RESET_WALLET
}

private fun getStringRes(lang: String, key: String): String {
    val map = mapOf(
        "options_title" to Pair("Opciones y Configuración", "Options & Settings"),
        "faucet_title" to Pair("Obtener saldo del grifo", "Get faucet balance"),
        "faucet_subtitle" to Pair("Este saldo es para hacer pruebas en la aplicación en modo Testnet y necesitará colocar su dirección testnet donde desea recibir sus BTC", "This balance is for testing the app in Testnet mode and you will need to provide your testnet address to receive BTC"),
        "lang_title" to Pair("Idioma de la Aplicación", "App Language"),
        "lang_active_es" to Pair("Idioma activo: Español", "Active language: Spanish"),
        "lang_active_en" to Pair("Idioma activo: English", "Active language: English"),
        "net_title" to Pair("Red de Bitcoin", "Bitcoin Network"),
        "net_mainnet" to Pair("Modo Activo: Mainnet (Real)", "Active Mode: Mainnet (Real)"),
        "net_testnet" to Pair("Modo Activo: Testnet (Pruebas)", "Active Mode: Testnet (Test)"),
        "bio_title" to Pair("Autenticación Biométrica", "Biometric Authentication"),
        "bio_subtitle" to Pair("Usar huella digital para acceder a la aplicación", "Use fingerprint to access the application"),
        "mod_pin_title" to Pair("Modificar PIN", "Modify PIN"),
        "mod_pin_subtitle" to Pair("Cambiar el PIN de seguridad actual de la billetera", "Change the current wallet security PIN"),
        "address_book_title" to Pair("Libreta de Direcciones", "Address Book"),
        "address_book_subtitle" to Pair("Añadir, ver y eliminar contactos guardados", "Add, view, and delete saved contacts"),
        "export_wallet_title" to Pair("Exportar Billetera", "Export Wallet"),
        "export_wallet_subtitle" to Pair("Ver su frase secreta de respaldo", "View your backup secret phrase"),
        "import_wallet_title" to Pair("Importar Billetera", "Import Wallet"),
        "import_wallet_subtitle" to Pair("Restaurar una billetera existente usando palabras clave y nuevo PIN", "Restore an existing wallet using keywords and a new PIN"),
        "reset_wallet_title" to Pair("Restablecer Billetera", "Reset Wallet"),
        "reset_wallet_subtitle" to Pair("Borrar permanentemente todos los datos y crear desde cero", "Permanently delete all data and start from scratch"),

        "sec_config" to Pair("Seguridad y Configuración", "Security & Settings"),
        "accept_restart" to Pair("Aceptar y Reiniciar", "Accept & Restart"),
        "bio_verif" to Pair("Verificación Biométrica", "Biometric Verification"),
        "bio_verif_sub" to Pair("Escanee su huella para ver su frase secreta", "Scan your fingerprint to view your secret phrase"),
        "confirm_identity" to Pair("Confirmar Identidad", "Confirm Identity"),
        "bio_confirm_sub" to Pair("Escanee su huella para activar la autenticación biométrica", "Scan your fingerprint to enable biometric authentication"),
        "bio_enabled_toast" to Pair("Autenticación biométrica activada", "Biometric authentication enabled"),
        "bio_disabled_toast" to Pair("Autenticación biométrica desactivada", "Biometric authentication disabled"),
        "verif_cancelled" to Pair("Verificación cancelada", "Verification cancelled"),
        "restart_lang_es" to Pair("El idioma ha sido cambiado a Español. La aplicación se reiniciará.", "Language changed to Spanish. The app will restart."),
        "restart_lang_en" to Pair("Language changed to English. The app will restart.", "Language changed to English. The app will restart."),
        "restart_net" to Pair("La red ha sido modificada. La aplicación se cerrará por seguridad para iniciar en la red seleccionada.", "The network has been modified. The app will close for security to start on the selected network."),
        "incorrect_pin" to Pair("PIN Incorrecto", "Incorrect PIN"),

        "import_step_1" to Pair("Importar Billetera (1/2)", "Import Wallet (1/2)"),
        "import_step_2" to Pair("Definir PIN Seguro (2/2)", "Set Secure PIN (2/2)"),
        "boxes_tab" to Pair("Casillas", "Boxes"),
        "block_tab" to Pair("Bloque", "Block"),
        "paste_phrase" to Pair("Pegue su frase completa:", "Paste your full phrase:"),
        "complete_words" to Pair("Complete las %d palabras:", "Complete the %d words:"),
        "paste_btn" to Pair("Pegar", "Paste"),
        "continue_pin" to Pair("CONTINUAR A CONFIGURAR PIN", "CONTINUE TO SET PIN"),
        "create_pin_sub" to Pair("Cree un nuevo PIN de 6 dígitos para encriptar su billetera", "Create a new 6-digit PIN to encrypt your wallet"),
        "new_pin_label" to Pair("Nuevo PIN (6 dígitos)", "New PIN (6-digit)"),
        "confirm_pin_label" to Pair("Confirme su PIN", "Confirm your PIN"),
        "finish_import_btn" to Pair("FINALIZAR E IMPORTAR BILLETERA", "FINISH & IMPORT WALLET"),
        "import_success_msg" to Pair("La billetera ha sido importada exitosamente y encriptada con su nuevo PIN. La aplicación se cerrará para iniciar de forma segura.", "The wallet has been successfully imported and encrypted with your new PIN. The app will close to start securely."),
        "complete_all_words" to Pair("Complete las %d palabras clave", "Complete the %d keywords"),
        "pins_mismatch" to Pair("Los PINs no coinciden o no tienen 6 dígitos", "PINs do not match or are not 6 digits"),

        "mod_pin_screen_title" to Pair("MODIFICAR PIN DE SEGURIDAD", "MODIFY SECURITY PIN"),
        "enter_current_pin" to Pair("Ingrese su PIN actual de 6 dígitos", "Enter your current 6-digit PIN"),
        "current_pin_label" to Pair("PIN Actual", "Current PIN"),
        "verify_pin_btn" to Pair("VERIFICAR PIN", "VERIFY PIN"),
        "current_pin_error" to Pair("El PIN actual es incorrecto", "Current PIN is incorrect"),
        "enter_new_pin_msg" to Pair("Ingrese su nuevo PIN y confírmelo", "Enter your new PIN and confirm it"),
        "confirm_new_pin_label" to Pair("Confirme Nuevo PIN", "Confirm New PIN"),
        "save_restart_btn" to Pair("GUARDAR Y REINICIAR", "SAVE & RESTART"),
        "update_cred_error" to Pair("Error al actualizar las credenciales", "Error updating credentials"),
        "new_pins_mismatch" to Pair("Los PINs nuevos no coinciden o no tienen 6 dígitos", "New PINs do not match or are not 6 digits"),
        "pin_modified_success" to Pair("El PIN de seguridad ha sido modificado exitosamente. La billetera se ha restablecido y la aplicación se cerrará.", "The security PIN has been successfully modified. The wallet has been reset and the app will close."),
        "cancel_btn" to Pair("CANCELAR", "CANCEL"),

        "danger_zone" to Pair("ZONA DE PELIGRO: RESTABLECER", "DANGER ZONE: RESET"),
        "reset_warning" to Pair("Esta acción eliminará de forma PERMANENTE todos los archivos, claves y saldos guardados en este dispositivo. Si no tiene respaldada su frase semilla, perderá sus fondos para siempre.", "This action will PERMANENTLY delete all files, keys, and balances saved on this device. If you don't have your seed phrase backed up, you will lose your funds forever."),
        "type_delete_prompt" to Pair("Escriba 'BORRAR' para confirmar", "Type 'DELETE' to confirm"),
        "type_delete_target" to Pair("BORRAR", "DELETE"),
        "type_delete_toast" to Pair("Debe escribir BORRAR exactamente", "You must type DELETE exactly"),
        "reset_btn" to Pair("BORRAR TODO Y REINICIAR", "DELETE ALL & RESTART"),
        "reset_success_msg" to Pair("La billetera ha sido restablecida permanentemente. Todos los datos del dispositivo fueron borrados. La aplicación se cerrará.", "The wallet has been permanently reset. All device data was wiped. The app will close."),

        "app_name_brand" to Pair("NIDUS CRIPTO", "NIDUS CRYPTO"),
        "enter_6_pin" to Pair("Ingrese su PIN de 6 dígitos", "Enter your 6-digit PIN"),

        "secret_phrase_title" to Pair("SU FRASE SECRETA", "YOUR SECRET PHRASE"),
        "secret_phrase_sub" to Pair("Guarde estas %d palabras en un lugar seguro. Son la única forma de recuperar su billetera.", "Save these %d words in a safe place. They are the only way to recover your wallet."),
        "warning_share" to Pair("ADVERTENCIA: Nunca comparta estas palabras con nadie.", "WARNING: Never share these words with anyone."),
        "load_seed_error" to Pair("No se pudo cargar la frase secreta de la billetera.", "Could not load wallet secret phrase."),
        "hide_words" to Pair("Ocultar Palabras", "Hide Words"),
        "show_words" to Pair("Mostrar Palabras", "Show Words"),
        "copy_words" to Pair("Copiar Palabras", "Copy Words"),
        "saved_words_btn" to Pair("HE GUARDADO MIS PALABRAS", "I HAVE SAVED MY WORDS"),

        "add_contact_title" to Pair("Agregar Nuevo Contacto", "Add New Contact"),
        "name_alias_label" to Pair("Nombre / Alias", "Name / Alias"),
        "btc_address_label" to Pair("Dirección BTC", "BTC Address"),
        "contact_saved_toast" to Pair("Contacto guardado", "Contact saved"),
        "invalid_data_toast" to Pair("Ingrese datos válidos", "Enter valid data"),
        "add_contact_btn" to Pair("AGREGAR CONTACTO", "ADD CONTACT"),
        "saved_contacts" to Pair("Contactos Guardados", "Saved Contacts"),
        "no_contacts" to Pair("No hay contactos en esta red.", "No contacts on this network."),
        "back_desc" to Pair("Regresar", "Back"),
        "delete_desc" to Pair("Eliminar", "Delete"),
        "clip_data" to Pair("Frase Secreta", "Secret Phrase"),
        "copy_seed" to Pair("Frase Semilla Copiada", "Seed Phrase Copied")
    )
    val entry = map[key] ?: Pair(key, key)
    return if (lang == "en") entry.second else entry.first
}

@Composable
fun OptionsScreen(
    isMainnet: Boolean,
    onToggleNetwork: (Boolean) -> Unit,
    isBiometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onImportWalletRequested: (String) -> Unit,
    currentBalanceSatoshis: Long,
    onSyncRequested: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(ExportStep.OPTIONS) }

    var currentLanguage by remember { mutableStateOf(StorageUtils.getLanguage(context)) }
    var languageMenuExpanded by remember { mutableStateOf(false) }

    val accentColor = if (isMainnet) Color(0xFFFFD700) else Color(0xFFE040FB)
    val borderColor = if (isMainnet) Color(0xFF3A320B) else Color(0xFF4A004A)

    var pinInput by remember { mutableStateOf("") }
    var seedWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var isWordsRevealed by remember { mutableStateOf(false) }

    var pendingBiometricState by remember { mutableStateOf(true) }
    var isVerifyingForExport by remember { mutableStateOf(false) }

    var showRestartDialog by remember { mutableStateOf(false) }
    var restartDialogMessage by remember { mutableStateOf("") }

    val loadSeedWords = {
        val mnemonic = StorageUtils.getMnemonic(context) ?: ""
        seedWords = mnemonic.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    }

    val killAppCompletely = {
        StorageUtils.clearTempMnemonic(context)
        Toast.makeText(context, restartDialogMessage, Toast.LENGTH_SHORT).show()

        android.os.Handler(context.mainLooper).postDelayed({
            val activity = context.findFragmentActivity() ?: (context as? Activity)
            activity?.finishAffinity()
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }, 1200)
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CardBackground,
            title = { Text(getStringRes(currentLanguage, "sec_config"), color = accentColor, fontWeight = FontWeight.Bold) },
            text = { Text(restartDialogMessage, color = Color.White, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { killAppCompletely() },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(getStringRes(currentLanguage, "accept_restart"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val startExportFlow = {
        val fragmentActivity = context.findFragmentActivity()
        isVerifyingForExport = true

        if (isBiometricEnabled && BiometricHelper.isBiometricAvailable(context) && fragmentActivity != null) {
            BiometricHelper.showBiometricPrompt(
                activity = fragmentActivity,
                title = getStringRes(currentLanguage, "bio_verif"),
                subtitle = getStringRes(currentLanguage, "bio_verif_sub"),
                onSuccess = {
                    loadSeedWords()
                    isWordsRevealed = false
                    currentStep = ExportStep.SHOW_SEED
                },
                onError = {
                    pinInput = ""
                    currentStep = ExportStep.PIN_VERIFICATION
                }
            )
        } else {
            pinInput = ""
            currentStep = ExportStep.PIN_VERIFICATION
        }
    }

    val requestAuthenticationForBiometricToggle = { targetState: Boolean ->
        pendingBiometricState = targetState
        isVerifyingForExport = false
        val fragmentActivity = context.findFragmentActivity()

        if (targetState) {
            if (BiometricHelper.isBiometricAvailable(context) && fragmentActivity != null) {
                BiometricHelper.showBiometricPrompt(
                    activity = fragmentActivity,
                    title = getStringRes(currentLanguage, "confirm_identity"),
                    subtitle = getStringRes(currentLanguage, "bio_confirm_sub"),
                    onSuccess = {
                        onToggleBiometric(true)
                        StorageUtils.setBiometricEnabled(context, true)
                        Toast.makeText(context, getStringRes(currentLanguage, "bio_enabled_toast"), Toast.LENGTH_SHORT).show()
                    },
                    onError = {
                        Toast.makeText(context, getStringRes(currentLanguage, "verif_cancelled"), Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                onToggleBiometric(true)
                StorageUtils.setBiometricEnabled(context, true)
            }
        } else {
            pinInput = ""
            currentStep = ExportStep.PIN_VERIFICATION
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        when (currentStep) {
            ExportStep.OPTIONS -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = getStringRes(currentLanguage, "options_title"),
                        color = accentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (currentBalanceSatoshis == 0L && !isMainnet) {
                        OptionCardItem(
                            title = getStringRes(currentLanguage, "faucet_title"),
                            subtitle = getStringRes(currentLanguage, "faucet_subtitle"),
                            icon = Icons.Default.WaterDrop,
                            accentColor = accentColor,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://coinfaucet.eu/en/btc-testnet/"))
                                context.startActivity(intent)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OptionCardItem(
                        title = getStringRes(currentLanguage, "lang_title"),
                        subtitle = if (currentLanguage == "en") getStringRes("en", "lang_active_en") else getStringRes("es", "lang_active_es"),
                        icon = Icons.Default.Language,
                        accentColor = accentColor,
                        trailingContent = {
                            Box {
                                OutlinedButton(
                                    onClick = { languageMenuExpanded = true },
                                    border = BorderStroke(1.dp, accentColor),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = currentLanguage.uppercase(),
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                DropdownMenu(
                                    expanded = languageMenuExpanded,
                                    onDismissRequest = { languageMenuExpanded = false },
                                    modifier = Modifier.background(CardBackground)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Español (ES)", color = if (currentLanguage == "es") accentColor else Color.White) },
                                        onClick = {
                                            languageMenuExpanded = false
                                            if (currentLanguage != "es") {
                                                currentLanguage = "es"
                                                StorageUtils.setLanguage(context, "es")
                                                restartDialogMessage = getStringRes("es", "restart_lang_es")
                                                showRestartDialog = true
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("English (EN)", color = if (currentLanguage == "en") accentColor else Color.White) },
                                        onClick = {
                                            languageMenuExpanded = false
                                            if (currentLanguage != "en") {
                                                currentLanguage = "en"
                                                StorageUtils.setLanguage(context, "en")
                                                restartDialogMessage = getStringRes("en", "restart_lang_en")
                                                showRestartDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OptionCardItem(
                        title = getStringRes(currentLanguage, "net_title"),
                        subtitle = if (isMainnet) getStringRes(currentLanguage, "net_mainnet") else getStringRes(currentLanguage, "net_testnet"),
                        icon = Icons.Default.ViewModule,
                        accentColor = accentColor,
                        trailingContent = {
                            Switch(
                                checked = isMainnet,
                                onCheckedChange = { newState ->
                                    onToggleNetwork(newState)
                                    StorageUtils.setMainnet(context, newState)

                                    restartDialogMessage = getStringRes(currentLanguage, "restart_net")
                                    showRestartDialog = true
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = accentColor,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OptionCardItem(
                        title = getStringRes(currentLanguage, "bio_title"),
                        subtitle = getStringRes(currentLanguage, "bio_subtitle"),
                        icon = Icons.Default.Fingerprint,
                        accentColor = accentColor,
                        trailingContent = {
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { newState ->
                                    requestAuthenticationForBiometricToggle(newState)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = accentColor,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OptionCardItem(
                        title = getStringRes(currentLanguage, "mod_pin_title"),
                        subtitle = getStringRes(currentLanguage, "mod_pin_subtitle"),
                        icon = Icons.Default.LockReset,
                        accentColor = accentColor,
                        onClick = { currentStep = ExportStep.MODIFY_PIN }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OptionCardItem(
                        title = getStringRes(currentLanguage, "address_book_title"),
                        subtitle = getStringRes(currentLanguage, "address_book_subtitle"),
                        icon = Icons.Default.Contacts,
                        accentColor = accentColor,
                        onClick = { currentStep = ExportStep.ADDRESS_BOOK }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OptionCardItem(
                        title = getStringRes(currentLanguage, "export_wallet_title"),
                        subtitle = getStringRes(currentLanguage, "export_wallet_subtitle"),
                        icon = Icons.Default.Key,
                        accentColor = accentColor,
                        onClick = { startExportFlow() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OptionCardItem(
                        title = getStringRes(currentLanguage, "import_wallet_title"),
                        subtitle = getStringRes(currentLanguage, "import_wallet_subtitle"),
                        icon = Icons.Default.Download,
                        accentColor = accentColor,
                        onClick = { currentStep = ExportStep.IMPORT_WALLET }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OptionCardItem(
                        title = getStringRes(currentLanguage, "reset_wallet_title"),
                        subtitle = getStringRes(currentLanguage, "reset_wallet_subtitle"),
                        icon = Icons.Default.Refresh,
                        accentColor = accentColor,
                        onClick = { currentStep = ExportStep.RESET_WALLET }
                    )
                }
            }

            ExportStep.PIN_VERIFICATION -> {
                PinVerificationScreen(
                    pinInput = pinInput,
                    accentColor = accentColor,
                    currentLanguage = currentLanguage,
                    onPinChange = { newPin ->
                        if (newPin.length <= 6) {
                            pinInput = newPin
                            if (pinInput.length == 6) {
                                val savedPin = StorageUtils.getSavedPin(context) ?: ""
                                if (pinInput == savedPin) {
                                    if (isVerifyingForExport) {
                                        loadSeedWords()
                                        isWordsRevealed = false
                                        currentStep = ExportStep.SHOW_SEED
                                    } else {
                                        StorageUtils.setBiometricEnabled(context, false)
                                        onToggleBiometric(false)
                                        Toast.makeText(context, getStringRes(currentLanguage, "bio_disabled_toast"), Toast.LENGTH_SHORT).show()
                                        currentStep = ExportStep.OPTIONS
                                    }
                                } else {
                                    Toast.makeText(context, getStringRes(currentLanguage, "incorrect_pin"), Toast.LENGTH_SHORT).show()
                                    pinInput = ""
                                }
                            }
                        }
                    },
                    onCancel = { currentStep = ExportStep.OPTIONS }
                )
            }

            ExportStep.SHOW_SEED -> {
                ExportSeedScreen(
                    words = seedWords,
                    isRevealed = isWordsRevealed,
                    accentColor = accentColor,
                    currentLanguage = currentLanguage,
                    onToggleReveal = { isWordsRevealed = !isWordsRevealed },
                    onClose = { currentStep = ExportStep.OPTIONS }
                )
            }

            ExportStep.ADDRESS_BOOK -> {
                AddressBookFullScreen(
                    isMainnet = isMainnet,
                    accentColor = accentColor,
                    borderColor = borderColor,
                    currentLanguage = currentLanguage,
                    onBack = { currentStep = ExportStep.OPTIONS }
                )
            }

            ExportStep.IMPORT_WALLET -> {
                ImportWalletWithPinFullScreen(
                    accentColor = accentColor,
                    borderColor = borderColor,
                    currentLanguage = currentLanguage,
                    onBack = { currentStep = ExportStep.OPTIONS },
                    onImportConfirmedWithPin = { mnemonic, newPin ->
                        StorageUtils.importWallet(context, mnemonic, newPin, isMainnet)

                        restartDialogMessage = getStringRes(currentLanguage, "import_success_msg")
                        showRestartDialog = true
                    }
                )
            }

            ExportStep.MODIFY_PIN -> {
                ModifyPinFullScreen(
                    accentColor = accentColor,
                    currentLanguage = currentLanguage,
                    onBack = { currentStep = ExportStep.OPTIONS },
                    onPinModifiedSuccessfully = {
                        restartDialogMessage = getStringRes(currentLanguage, "pin_modified_success")
                        showRestartDialog = true
                    }
                )
            }

            ExportStep.RESET_WALLET -> {
                ResetWalletFullScreen(
                    currentLanguage = currentLanguage,
                    onBack = { currentStep = ExportStep.OPTIONS },
                    onResetConfirmed = {
                        StorageUtils.clearAllData(context)
                        restartDialogMessage = getStringRes(currentLanguage, "reset_success_msg")
                        showRestartDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun ImportWalletWithPinFullScreen(
    accentColor: Color,
    borderColor: Color,
    currentLanguage: String,
    onBack: () -> Unit,
    onImportConfirmedWithPin: (String, String) -> Unit
) {
    val context = LocalContext.current
    var wordCount by remember { mutableIntStateOf(12) }
    var words by remember { mutableStateOf(List(24) { "" }) }
    var rawInput by remember { mutableStateOf("") }
    var isManualRawMode by remember { mutableStateOf(false) }

    var stepPhase by remember { mutableStateOf(1) }
    var collectedMnemonic by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }

    fun processMnemonicString(input: String) {
        val cleanList = input.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (cleanList.size == 12 || cleanList.size == 24) {
            wordCount = cleanList.size
        }
        val newList = words.toMutableList()
        for (i in 0 until minOf(cleanList.size, 24)) {
            newList[i] = cleanList[i]
        }
        words = newList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            IconButton(
                onClick = {
                    if (stepPhase > 1) stepPhase = 1 else onBack()
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(CardBackground, CircleShape)
                    .border(1.dp, borderColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = getStringRes(currentLanguage, "back_desc"),
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = when(stepPhase) {
                    1 -> getStringRes(currentLanguage, "import_step_1")
                    else -> getStringRes(currentLanguage, "import_step_2")
                },
                color = accentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        when (stepPhase) {
            1 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(CardBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TabButton(
                            text = getStringRes(currentLanguage, "boxes_tab"),
                            icon = Icons.Default.ViewModule,
                            isSelected = !isManualRawMode,
                            accentColor = accentColor,
                            onClick = { isManualRawMode = false },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = getStringRes(currentLanguage, "block_tab"),
                            icon = Icons.Default.EditNote,
                            isSelected = isManualRawMode,
                            accentColor = accentColor,
                            onClick = { isManualRawMode = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = wordCount == 12,
                            onClick = { wordCount = 12 },
                            label = { Text("12", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.height(36.dp)
                        )
                        FilterChip(
                            selected = wordCount == 24,
                            onClick = { wordCount = 24 },
                            label = { Text("24", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isManualRawMode) getStringRes(currentLanguage, "paste_phrase") else String.format(getStringRes(currentLanguage, "complete_words"), wordCount),
                        color = TextGray,
                        fontSize = 13.sp
                    )

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val pastedText = clipData.getItemAt(0).text.toString()
                                processMnemonicString(pastedText)
                                rawInput = pastedText
                            }
                        },
                        border = BorderStroke(1.dp, accentColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(getStringRes(currentLanguage, "paste_btn"), color = accentColor, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isManualRawMode) {
                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = {
                            rawInput = it
                            processMnemonicString(it)
                        },
                        placeholder = { Text("ej. apple banana cherry...", color = TextGray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else {
                    val activeWords = words.take(wordCount)
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in activeWords.indices step 2) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val index1 = i
                                OutlinedTextField(
                                    value = activeWords[index1],
                                    onValueChange = { newWord ->
                                        val updatedList = words.toMutableList()
                                        updatedList[index1] = newWord.trim().lowercase()
                                        words = updatedList
                                    },
                                    leadingIcon = { Text("${index1 + 1}.", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor, unfocusedBorderColor = Color(0xFF2B2B2B),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                        focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                val index2 = i + 1
                                if (index2 < activeWords.size) {
                                    OutlinedTextField(
                                        value = activeWords[index2],
                                        onValueChange = { newWord ->
                                            val updatedList = words.toMutableList()
                                            updatedList[index2] = newWord.trim().lowercase()
                                            words = updatedList
                                        },
                                        leadingIcon = { Text("${index2 + 1}.", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp)) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = accentColor, unfocusedBorderColor = Color(0xFF2B2B2B),
                                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                            focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val finalMnemonic = words.take(wordCount).joinToString(" ").trim()
                        val cleanList = finalMnemonic.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                        if (cleanList.size == wordCount) {
                            collectedMnemonic = finalMnemonic
                            stepPhase = 2
                        } else {
                            Toast.makeText(context, String.format(getStringRes(currentLanguage, "complete_all_words"), wordCount), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(getStringRes(currentLanguage, "continue_pin"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            2 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = getStringRes(currentLanguage, "create_pin_sub"), color = TextGray, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text(getStringRes(currentLanguage, "new_pin_label"), color = TextGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPinInput = it },
                        label = { Text(getStringRes(currentLanguage, "confirm_pin_label"), color = TextGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = {
                            if (newPinInput.length == 6 && newPinInput == confirmPinInput) {
                                onImportConfirmedWithPin(collectedMnemonic, newPinInput)
                            } else {
                                Toast.makeText(context, getStringRes(currentLanguage, "pins_mismatch"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(getStringRes(currentLanguage, "finish_import_btn"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ModifyPinFullScreen(
    accentColor: Color,
    currentLanguage: String,
    onBack: () -> Unit,
    onPinModifiedSuccessfully: () -> Unit
) {
    val context = LocalContext.current
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = getStringRes(currentLanguage, "mod_pin_screen_title"), color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        when(step) {
            1 -> {
                Text(text = getStringRes(currentLanguage, "enter_current_pin"), color = TextGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) oldPin = it },
                    label = { Text(getStringRes(currentLanguage, "current_pin_label"), color = TextGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        val saved = StorageUtils.getSavedPin(context) ?: ""
                        if (oldPin == saved) {
                            step = 2
                        } else {
                            Toast.makeText(context, getStringRes(currentLanguage, "current_pin_error"), Toast.LENGTH_SHORT).show()
                            oldPin = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(getStringRes(currentLanguage, "verify_pin_btn"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            2 -> {
                Text(text = getStringRes(currentLanguage, "enter_new_pin_msg"), color = TextGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text(getStringRes(currentLanguage, "new_pin_label"), color = TextGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text(getStringRes(currentLanguage, "confirm_new_pin_label"), color = TextGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (newPin.length == 6 && newPin == confirmPin) {
                            val success = StorageUtils.updatePinAndReencryptSeed(context, newPin)
                            if (success) {
                                onPinModifiedSuccessfully()
                            } else {
                                Toast.makeText(context, getStringRes(currentLanguage, "update_cred_error"), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, getStringRes(currentLanguage, "new_pins_mismatch"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(getStringRes(currentLanguage, "save_restart_btn"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text(getStringRes(currentLanguage, "cancel_btn"), color = TextGray)
        }
    }
}

@Composable
private fun ResetWalletFullScreen(
    currentLanguage: String,
    onBack: () -> Unit,
    onResetConfirmed: () -> Unit
) {
    val context = LocalContext.current
    var confirmedText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = getStringRes(currentLanguage, "danger_zone"), color = ErrorRed, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = getStringRes(currentLanguage, "reset_warning"),
            color = TextGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = confirmedText,
            onValueChange = { confirmedText = it },
            label = { Text(getStringRes(currentLanguage, "type_delete_prompt"), color = TextGray, fontSize = 11.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ErrorRed, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = CardBackground, unfocusedContainerColor = CardBackground),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (confirmedText.trim().uppercase() == getStringRes(currentLanguage, "type_delete_target")) {
                    onResetConfirmed()
                } else {
                    Toast.makeText(context, getStringRes(currentLanguage, "type_delete_toast"), Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(getStringRes(currentLanguage, "reset_btn"), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onBack) {
            Text(getStringRes(currentLanguage, "cancel_btn"), color = TextGray)
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) accentColor else Color.Transparent,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.Black else TextGray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isSelected) Color.Black else TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PinVerificationScreen(
    pinInput: String,
    accentColor: Color,
    currentLanguage: String,
    onPinChange: (String) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text(
                text = getStringRes(currentLanguage, "app_name_brand"),
                color = accentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = getStringRes(currentLanguage, "enter_6_pin"),
                color = TextGray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 0 until 6) {
                    val isFilled = i < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                width = 1.dp,
                                color = if (isFilled) accentColor else Color.Gray,
                                shape = CircleShape
                            )
                            .background(
                                color = if (isFilled) accentColor else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 20.dp)
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
                            Spacer(modifier = Modifier.size(60.dp))
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E1E1E),
                                modifier = Modifier
                                    .size(60.dp)
                                    .clickable {
                                        if (btn == "DEL") {
                                            if (pinInput.isNotEmpty()) {
                                                onPinChange(pinInput.dropLast(1))
                                            }
                                        } else {
                                            if (pinInput.length < 6) {
                                                onPinChange(pinInput + btn)
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (btn == "DEL") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = getStringRes(currentLanguage, "delete_desc"),
                                            tint = Color.White
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

            TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
                Text(getStringRes(currentLanguage, "cancel_btn"), color = TextGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ExportSeedScreen(
    words: List<String>,
    isRevealed: Boolean,
    accentColor: Color,
    currentLanguage: String,
    onToggleReveal: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = getStringRes(currentLanguage, "secret_phrase_title"),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = String.format(getStringRes(currentLanguage, "secret_phrase_sub"), if (words.isNotEmpty()) words.size else 12),
            color = TextGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = getStringRes(currentLanguage, "warning_share"),
            color = ErrorRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (words.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in words.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WordChipItem(
                            index = i + 1,
                            word = words[i],
                            isRevealed = isRevealed,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (i + 1 < words.size) {
                            WordChipItem(
                                index = i + 2,
                                word = words[i + 1],
                                isRevealed = isRevealed,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getStringRes(currentLanguage, "load_seed_error"),
                    color = ErrorRed,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onToggleReveal,
            border = BorderStroke(1.dp, accentColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .width(220.dp)
                .height(44.dp)
        ) {
            Icon(
                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRevealed) getStringRes(currentLanguage, "hide_words") else getStringRes(currentLanguage, "show_words"),
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                if (words.isNotEmpty()) {
                    val fullSeed = words.joinToString(" ")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(getStringRes(currentLanguage, "clip_data"), fullSeed)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, getStringRes(currentLanguage, "copy_seed"), Toast.LENGTH_SHORT).show()
                }
            },
            border = BorderStroke(1.dp, accentColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .width(220.dp)
                .height(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getStringRes(currentLanguage, "copy_words"),
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = getStringRes(currentLanguage, "saved_words_btn"),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun WordChipItem(
    index: Int,
    word: String,
    isRevealed: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index. ",
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isRevealed) word else "••••••",
                color = if (isRevealed) Color.White else TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OptionCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2B2B2B), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
    }
}

@Composable
private fun AddressBookFullScreen(
    isMainnet: Boolean,
    accentColor: Color,
    borderColor: Color,
    currentLanguage: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(AddressBookManager.getContacts(context, isMainnet)) }
    var nameInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(CardBackground, CircleShape)
                    .border(1.dp, borderColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = getStringRes(currentLanguage, "back_desc"),
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = getStringRes(currentLanguage, "address_book_title"),
                color = accentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = getStringRes(currentLanguage, "add_contact_title"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(getStringRes(currentLanguage, "name_alias_label"), color = TextGray, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF141414),
                        unfocusedContainerColor = Color(0xFF141414)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = { Text(getStringRes(currentLanguage, "btc_address_label"), color = TextGray, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF141414),
                        unfocusedContainerColor = Color(0xFF141414)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (AddressBookManager.saveContact(context, nameInput, addressInput, isMainnet)) {
                            nameInput = ""
                            addressInput = ""
                            contacts = AddressBookManager.getContacts(context, isMainnet)
                            Toast.makeText(context, getStringRes(currentLanguage, "contact_saved_toast"), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, getStringRes(currentLanguage, "invalid_data_toast"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(getStringRes(currentLanguage, "add_contact_btn"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = getStringRes(currentLanguage, "saved_contacts"),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(getStringRes(currentLanguage, "no_contacts"), color = TextGray, fontSize = 13.sp)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                contacts.forEach { contact ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF2B2B2B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contact.address,
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = {
                                    AddressBookManager.deleteContact(context, contact.id)
                                    contacts = AddressBookManager.getContacts(context, isMainnet)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = getStringRes(currentLanguage, "delete_desc"),
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
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