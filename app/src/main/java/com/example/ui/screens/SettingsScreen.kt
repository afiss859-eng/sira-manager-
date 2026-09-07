package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.SiraRole
import com.example.data.entity.*
import com.example.printer.SiraBluetoothPrinter
import com.example.ui.components.GoogleAuthDialog
import com.example.ui.components.TermsAndPrivacyDialog
import com.example.ui.theme.*
import com.example.viewmodel.SiraNavTab
import com.example.viewmodel.SiraViewModel

@Composable
fun SettingsScreen(viewModel: SiraViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val profile by viewModel.merchantProfile.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val printStatus by viewModel.printStatus.collectAsState()
    val defaultPrinterMac by viewModel.defaultPrinterMac.collectAsState()
    val licenseConfig by viewModel.licenseConfig.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showCustomizationDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var bluetoothPermissionDenied by remember { mutableStateOf(false) }

    val printerEngine = remember { SiraBluetoothPrinter(context) }
    val pairedPrinters = remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    val requestBluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) {
            pairedPrinters.value = printerEngine.pairedPrinters()
            bluetoothPermissionDenied = false
        } else bluetoothPermissionDenied = true
    }

    fun openPrinterConfiguration() {
        if (Build.VERSION.SDK_INT >= 31) {
            requestBluetoothPermission.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
        } else pairedPrinters.value = printerEngine.pairedPrinters()
        showPrinterDialog = true
    }

    if (showCustomizationDialog) {
        com.example.ui.components.AppCustomizationDialog(viewModel = viewModel, licenseConfig = licenseConfig, onDismiss = { showCustomizationDialog = false })
    }

    LazyColumn(modifier = Modifier.fillMaxSize().testTag("settings_screen"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF0F172A), border = BorderStroke(1.dp, Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.sira_logo), contentDescription = "Logo Officiel SIRA", modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Organisation SIRA", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text("Chemin d'aujourd'hui, Avenir de demain", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text("Tous droits réservés à SIRA", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SleekBluePrimary)
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SleekOutline), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(46.dp).background(if (currentUser.role == SiraRole.SUPER_ADMIN) SleekBluePrimary else SleekSecondary, CircleShape), contentAlignment = Alignment.Center) {
                                Text(profile.merchantName.take(2).uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column { Text(profile.merchantName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary); Text(profile.email, style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary) }
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFDCFCE7)) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(if (currentUser.role == SiraRole.SUPER_ADMIN) "Admin Google" else "Commerçant Google", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp)); Text("Base Locale : sira_user_${currentUser.id}.db", fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = SleekBluePrimary); Text("Boutique : ${profile.shopName} • ${profile.city}", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAuthDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Changer compte", fontSize = 11.sp) }
                        Button(onClick = { showEditProfileDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary), shape = RoundedCornerShape(10.dp)) { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Infos boutique", fontSize = 11.sp) }
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SleekOutline), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Print, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(10.dp)); Column { Text("Imprimante Bluetooth", fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(defaultPrinterMac?.let { "Configurée : $it" } ?: "Aucune imprimante par défaut", fontSize = 11.sp, color = SleekTextSecondary) } }
                        Button(onClick = { openPrinterConfiguration() }, colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("Configurer", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(modifier = Modifier.height(8.dp)); Text("Après validation d'une vente, SIRA imprime automatiquement le reçu sur cette imprimante.", fontSize = 11.sp, color = SleekTextSecondary)
                    printStatus?.let { Spacer(modifier = Modifier.height(6.dp)); Text(it, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (it.startsWith("Reçu")) Color(0xFF15803D) else Color(0xFFB45309)) }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = SleekBlueContainer.copy(alpha = 0.5f), border = BorderStroke(1.dp, SleekBluePrimary.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(10.dp)); Column(modifier = Modifier.weight(1f)) { Text("Licence & Personnalisation App", fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("Clé : ${licenseConfig.key.ifBlank { "Aucune" }} • ${licenseConfig.appName}", fontSize = 11.sp, color = SleekTextSecondary) }; Button(onClick = { showCustomizationDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("Personnaliser", fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
            }
        }
        if (currentUser.role == SiraRole.SUPER_ADMIN) item { Surface(shape = RoundedCornerShape(18.dp), color = SleekBlueContainer.copy(alpha = 0.5f), border = BorderStroke(1.dp, SleekBluePrimary.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(10.dp)); Column(modifier = Modifier.weight(1f)) { Text("Portail Administrateur Cloud", fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("Gestion des marchands, rôles & licences API", fontSize = 11.sp, color = SleekTextSecondary) }; Button(onClick = { viewModel.selectTab(SiraNavTab.ADMIN_CENTER) }, colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("Accéder", fontSize = 11.sp, fontWeight = FontWeight.Bold) } } } }
        item { Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SleekOutline), modifier = Modifier.fillMaxWidth().clickable { showTermsDialog = true }) { Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Gavel, contentDescription = null, tint = SleekBluePrimary); Spacer(modifier = Modifier.width(10.dp)); Column { Text("Conditions Générales & Politique de Confidentialité", fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text("Tous droits réservés à SIRA • Souveraineté des données locales", fontSize = 10.sp, color = SleekTextSecondary) } }; Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SleekTextSecondary) } } }
        item { Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SleekOutline), modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Storage, contentDescription = null, tint = SleekBluePrimary); Spacer(modifier = Modifier.width(8.dp)); Text("Base Locale & Synchronisation", fontWeight = FontWeight.Bold, fontSize = 13.sp) }; Button(onClick = { viewModel.performCloudSync() }, enabled = !isSyncing, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White) else Text("Synchroniser", fontSize = 11.sp, fontWeight = FontWeight.Bold) } }; Spacer(modifier = Modifier.height(8.dp)); Text(syncMessage ?: "Base locale SQLite opérationnelle (zéro simulation).", fontSize = 11.sp, color = SleekTextSecondary); Spacer(modifier = Modifier.height(12.dp)); OutlinedButton(onClick = { showResetConfirmDialog = true }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)), border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Remise à zéro complète (Vierge)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } } } }
    }

    if (showPrinterDialog) AlertDialog(onDismissRequest = { showPrinterDialog = false }, icon = { Icon(Icons.Default.Print, contentDescription = null, tint = SleekBluePrimary) }, title = { Text("Choisir l'imprimante") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bluetoothPermissionDenied) { Text("L'autorisation Bluetooth est nécessaire pour lire les appareils associés.", color = Color(0xFFB91C1C), fontSize = 12.sp); OutlinedButton(onClick = { openPrinterConfiguration() }) { Text("Redemander l'autorisation") } }
        if (pairedPrinters.value.isEmpty() && !bluetoothPermissionDenied) Text("Aucune imprimante associée détectée. Associez d'abord l'imprimante dans les réglages Bluetooth Android.", fontSize = 12.sp, color = SleekTextSecondary)
        pairedPrinters.value.forEach { device -> Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { viewModel.setDefaultBluetoothPrinter(device.address); showPrinterDialog = false }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Print, contentDescription = null, tint = SleekBluePrimary); Spacer(modifier = Modifier.width(10.dp)); Column(modifier = Modifier.weight(1f)) { Text(device.name ?: "Imprimante sans nom", fontWeight = FontWeight.SemiBold); Text(device.address, fontSize = 10.sp, color = SleekTextSecondary) }; if (defaultPrinterMac == device.address) Icon(Icons.Default.CheckCircle, contentDescription = "Sélectionnée", tint = Color(0xFF15803D)) } }
    } }, confirmButton = { TextButton(onClick = { showPrinterDialog = false }) { Text("Fermer") } })

    if (showResetConfirmDialog) AlertDialog(onDismissRequest = { showResetConfirmDialog = false }, icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFDC2626)) }, title = { Text("Réinitialiser à blanc ?") }, text = { Text("Cette action effacera toutes les données pour repartir sur une base 100% vierge, sans aucune donnée factice ou simulée.") }, confirmButton = { Button(onClick = { viewModel.resetAllDataToBlank(); showResetConfirmDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))) { Text("Confirmer la remise à blanc", color = Color.White) } }, dismissButton = { OutlinedButton(onClick = { showResetConfirmDialog = false }) { Text("Annuler") } })

    if (showEditProfileDialog) { var newShopName by remember { mutableStateOf(profile.shopName) }; var newMerchantName by remember { mutableStateOf(profile.merchantName) }; var newPhone by remember { mutableStateOf(profile.phone) }; var newCity by remember { mutableStateOf(profile.city) }; var newIfu by remember { mutableStateOf(profile.ifuNumber) }; AlertDialog(onDismissRequest = { showEditProfileDialog = false }, title = { Text("Modifier Profil Boutique") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = newShopName, onValueChange = { newShopName = it }, label = { Text("Nom du commerce") }, singleLine = true); OutlinedTextField(value = newMerchantName, onValueChange = { newMerchantName = it }, label = { Text("Nom du commerçant") }, singleLine = true); OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Téléphone") }, singleLine = true); OutlinedTextField(value = newCity, onValueChange = { newCity = it }, label = { Text("Ville") }, singleLine = true); OutlinedTextField(value = newIfu, onValueChange = { newIfu = it }, label = { Text("N° IFU") }, singleLine = true) } }, confirmButton = { Button(onClick = { viewModel.updateMerchantProfile(profile.copy(shopName = newShopName, merchantName = newMerchantName, phone = newPhone, city = newCity, ifuNumber = newIfu)); showEditProfileDialog = false }) { Text("Sauvegarder") } }, dismissButton = { OutlinedButton(onClick = { showEditProfileDialog = false }) { Text("Annuler") } }) }
    if (showAuthDialog) GoogleAuthDialog(authManager = viewModel.authManager, onDismiss = { showAuthDialog = false }, onUserSwitched = { viewModel.switchUser(it) })
    if (showTermsDialog) TermsAndPrivacyDialog(onDismiss = { showTermsDialog = false })
}
