package com.sira.orangemoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sira.orangemoney.update.AppAutoUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val store by lazy { OrangeLocalStore(this) }
    private val licenseClient by lazy { OrangeLicenseClient(this) }
    private val updateClient by lazy { AppAutoUpdate(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SiraOrangeMoneyApp(store, licenseClient, updateClient) }
    }
}

private enum class OrangeTab(val label: String) { DASHBOARD("Accueil"), OPERATION("Opération"), HISTORY("Historique"), SECURITY("Sécurité") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiraOrangeMoneyApp(store: OrangeLocalStore, licenseClient: OrangeLicenseClient, updateClient: AppAutoUpdate) {
    var tab by remember { mutableStateOf(OrangeTab.DASHBOARD) }
    var transactions by remember { mutableStateOf(store.all()) }
    var selectedType by remember { mutableStateOf("TRANSFERT") }
    var amount by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var idDocument by remember { mutableStateOf("") }
    var balanceBefore by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var licenseInput by remember { mutableStateOf(licenseClient.cachedLicenseKey.orEmpty()) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var mandatoryUpdate by remember { mutableStateOf<AppAutoUpdate.UpdateInfo?>(null) }
    var preview by remember { mutableStateOf<OrangeTransaction?>(null) }
    val scope = rememberCoroutineScope()
    val money = remember { NumberFormat.getIntegerInstance(Locale.FRANCE) }

    LaunchedEffect(Unit) {
        mandatoryUpdate = withContext(Dispatchers.IO) { runCatching { updateClient.check() }.getOrNull()?.takeIf { it.mandatory } }
    }

    val filtered = remember(transactions, query, selectedType) {
        val needle = query.trim().lowercase(Locale.ROOT)
        transactions.filter { tx ->
            val typeOk = selectedType == "TOUT" || tx.type == selectedType
            val queryOk = needle.isBlank() || listOf(tx.reference, tx.counterparty, tx.customerPhone, tx.recipientPhone, tx.idDocument).any { it.lowercase(Locale.ROOT).contains(needle) }
            typeOk && queryOk
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("SIRA Orange Money", fontWeight = FontWeight.Black)
                        Text("Gestionnaire marchand • contrôle des opérations", fontSize = 11.sp)
                    }
                })
            },
            bottomBar = {
                NavigationBar {
                    OrangeTab.values().forEach { item ->
                        NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Text(item.label.take(1), fontWeight = FontWeight.Bold) }, label = { Text(item.label) })
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface)) {
                when (tab) {
                    OrangeTab.DASHBOARD -> DashboardScreen(transactions, money, onNew = { tab = OrangeTab.OPERATION })
                    OrangeTab.OPERATION -> OperationScreen(
                        type = selectedType,
                        amount = amount,
                        customerName = customerName,
                        customerPhone = customerPhone,
                        recipientPhone = recipientPhone,
                        idDocument = idDocument,
                        balanceBefore = balanceBefore,
                        note = note,
                        status = status,
                        onTypeChange = { selectedType = it },
                        onAmountChange = { amount = it.filter(Char::isDigit) },
                        onCustomerNameChange = { customerName = it.take(80) },
                        onCustomerPhoneChange = { customerPhone = it.filter(Char::isDigit).take(15) },
                        onRecipientPhoneChange = { recipientPhone = it.filter(Char::isDigit).take(15) },
                        onIdDocumentChange = { idDocument = it.take(64) },
                        onBalanceChange = { balanceBefore = it.filter(Char::isDigit) },
                        onNoteChange = { note = it.take(180) },
                        onPreview = {
                            val value = amount.toLongOrNull() ?: 0L
                            if (value <= 0) status = "Le montant est obligatoire."
                            else if (customerPhone.length !in 8..15) status = "Vérifiez le numéro du client."
                            else if (idDocument.isBlank()) status = "Le numéro de pièce d'identité est obligatoire."
                            else preview = buildPreview(selectedType, value, customerName, customerPhone, recipientPhone, idDocument, balanceBefore, note)
                        },
                        onConfirm = {
                            preview?.let { tx ->
                                store.add(tx.amount, tx.fee, tx.type, tx.counterparty, tx.customerPhone, tx.recipientPhone, tx.idDocument, tx.balanceBefore, tx.balanceAfter, tx.status, tx.note)
                                transactions = store.all()
                                preview = null
                                amount = ""; customerName = ""; customerPhone = ""; recipientPhone = ""; idDocument = ""; balanceBefore = ""; note = ""
                                status = "Opération enregistrée avec référence sécurisée."
                                tab = OrangeTab.HISTORY
                            }
                        }
                    )
                    OrangeTab.HISTORY -> HistoryScreen(filtered, query, selectedType, money, { query = it }, { selectedType = it }, onSelect = { preview = it })
                    OrangeTab.SECURITY -> SecurityScreen(licenseInput, { licenseInput = it.take(100) }, licenseClient.cachedLicenseKey, busy, status,
                        onActivate = {
                            busy = true
                            scope.launch {
                                val result = licenseClient.activate(licenseInput)
                                busy = false
                                status = result.fold({ "Licence validée : $it" }, { it.message ?: "Validation impossible." })
                            }
                        }, onClear = { licenseClient.clearLocalLicense(); licenseInput = ""; status = "Licence locale supprimée." })
                }
            }
        }
    }

    preview?.let { tx ->
        AlertDialog(onDismissRequest = { preview = null }, title = { Text("Vérification avant validation") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("${tx.type} • ${money.format(tx.amount)} FCFA", fontWeight = FontWeight.Bold)
                Text("Client : ${tx.counterparty.ifBlank { "Non renseigné" }}")
                Text("Téléphone : ${tx.customerPhone}")
                if (tx.recipientPhone.isNotBlank()) Text("Bénéficiaire : ${tx.recipientPhone}")
                Text("Pièce : ${tx.idDocument}")
                if (tx.balanceBefore > 0) Text("Solde : ${money.format(tx.balanceBefore)} → ${money.format(tx.balanceAfter)} FCFA")
                Text("Commission estimée : ${money.format(tx.fee)} FCFA")
                Text("Aucune transaction Orange Money réelle n’est exécutée par cette fonction locale : elle enregistre et prépare l’opération pour contrôle.", fontSize = 11.sp)
            }
        }, confirmButton = { Button(onClick = { val current = preview; if (current != null) { store.add(current.amount, current.fee, current.type, current.counterparty, current.customerPhone, current.recipientPhone, current.idDocument, current.balanceBefore, current.balanceAfter, current.status, current.note); transactions = store.all(); preview = null; amount = ""; customerName = ""; customerPhone = ""; recipientPhone = ""; idDocument = ""; balanceBefore = ""; note = ""; status = "Opération enregistrée."; tab = OrangeTab.HISTORY } }) { Text("Valider et enregistrer") } }, dismissButton = { TextButton(onClick = { preview = null }) { Text("Modifier") } })
    }

    mandatoryUpdate?.let { info ->
        AlertDialog(onDismissRequest = {}, title = { Text(info.title.ifBlank { "Mise à jour obligatoire" }) }, text = { Text("Version ${info.latestVersion}\n\n${info.notes.take(1200)}") }, confirmButton = { Button(onClick = { scope.launch { withContext(Dispatchers.IO) { updateClient.downloadAndInstall(info) } } }) { Text("Mettre à jour") } })
    }
}

private fun buildPreview(type: String, amount: Long, name: String, customerPhone: String, recipientPhone: String, id: String, balance: String, note: String): OrangeTransaction {
    val before = balance.toLongOrNull()?.coerceAtLeast(0) ?: 0L
    val fee = (amount / 100L).coerceAtLeast(0L)
    val after = when (type) { "TRANSFERT" -> (before - amount - fee).coerceAtLeast(0); "ENCAISSEMENT" -> before + amount - fee; else -> before }
    return OrangeTransaction(reference = "", amount = amount, fee = fee, timestamp = System.currentTimeMillis(), type = type, counterparty = name, customerPhone = customerPhone, recipientPhone = recipientPhone, idDocument = id, balanceBefore = before, balanceAfter = after, status = "À_VALIDER", note = note)
}

@Composable
private fun DashboardScreen(transactions: List<OrangeTransaction>, money: NumberFormat, onNew: () -> Unit) {
    val today = SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date())
    val todayTx = transactions.filter { SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date(it.timestamp)) == today }
    val volume = todayTx.sumOf { it.amount }
    val fees = todayTx.sumOf { it.fee }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Card(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp)) { Text("Tableau de bord", fontSize = 24.sp, fontWeight = FontWeight.Black); Text("Aujourd’hui", color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); Text("${money.format(volume)} FCFA", fontSize = 30.sp, fontWeight = FontWeight.Black); Text("Volume enregistré", fontSize = 12.sp); Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${todayTx.size} opérations"); Text("${money.format(fees)} FCFA de commissions") } } } }
        item { Button(onClick = onNew, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) { Text("+ Nouvelle opération", fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
        item { Text("Dernières opérations", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        items(todayTx.take(6)) { tx -> TransactionRow(tx, money) }
    }
}

@Composable
private fun OperationScreen(type: String, amount: String, customerName: String, customerPhone: String, recipientPhone: String, idDocument: String, balanceBefore: String, note: String, status: String?, onTypeChange: (String) -> Unit, onAmountChange: (String) -> Unit, onCustomerNameChange: (String) -> Unit, onCustomerPhoneChange: (String) -> Unit, onRecipientPhoneChange: (String) -> Unit, onIdDocumentChange: (String) -> Unit, onBalanceChange: (String) -> Unit, onNoteChange: (String) -> Unit, onPreview: () -> Unit, onConfirm: () -> Unit) {
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text("Nouvelle opération", fontSize = 24.sp, fontWeight = FontWeight.Black)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("ENCAISSEMENT", "TRANSFERT").forEachIndexed { i, label -> SegmentedButton(selected = type == label, onClick = { onTypeChange(label) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(if (label == "ENCAISSEMENT") "Encaisser" else "Transférer") } }
        }
        OutlinedTextField(amount, onAmountChange, Modifier.fillMaxWidth(), label = { Text("Montant FCFA") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(customerName, onCustomerNameChange, Modifier.fillMaxWidth(), label = { Text("Nom du client") }, singleLine = true)
        OutlinedTextField(customerPhone, onCustomerPhoneChange, Modifier.fillMaxWidth(), label = { Text("Téléphone du client") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
        OutlinedTextField(recipientPhone, onRecipientPhoneChange, Modifier.fillMaxWidth(), label = { Text("Téléphone bénéficiaire") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
        OutlinedTextField(idDocument, onIdDocumentChange, Modifier.fillMaxWidth(), label = { Text("N° pièce d'identité") }, singleLine = true)
        OutlinedTextField(balanceBefore, onBalanceChange, Modifier.fillMaxWidth(), label = { Text("Solde avant opération (optionnel)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(note, onNoteChange, Modifier.fillMaxWidth(), label = { Text("Note") }, minLines = 2, maxLines = 3)
        Button(onClick = onPreview, enabled = amount.toLongOrNull()?.let { it > 0 } == true, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)) { Text("VÉRIFIER L’OPÉRATION", fontWeight = FontWeight.Black) }
        status?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun HistoryScreen(transactions: List<OrangeTransaction>, query: String, type: String, money: NumberFormat, onQuery: (String) -> Unit, onType: (String) -> Unit, onSelect: (OrangeTransaction) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Historique", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text("Rechercher nom, téléphone, référence, pièce…") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("TOUT", "ENCAISSEMENT", "TRANSFERT").forEach { FilterChip(selected = type == it, onClick = { onType(it) }, label = { Text(it.take(12)) }) } }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(transactions) { tx -> Card(onClick = { onSelect(tx) }) { TransactionRow(tx, money) } } }
    }
}

@Composable
private fun TransactionRow(tx: OrangeTransaction, money: NumberFormat) {
    ListItem(headlineContent = { Text("${tx.type} • ${money.format(tx.amount)} FCFA", fontWeight = FontWeight.Bold) }, supportingContent = { Text("${tx.counterparty.ifBlank { "Client" }} • ${tx.customerPhone}\n${tx.reference} • ${SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date(tx.timestamp))} • Commission ${money.format(tx.fee)} F") })
}

@Composable
private fun SecurityScreen(input: String, onInput: (String) -> Unit, cached: String?, busy: Boolean, status: String?, onActivate: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sécurité", fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Licence séparée, stockage local et validation serveur. Ne partagez jamais votre clé de licence.")
        OutlinedTextField(input, onInput, Modifier.fillMaxWidth(), label = { Text("Clé de licence") }, singleLine = true)
        Button(onClick = onActivate, enabled = input.isNotBlank() && !busy, Modifier.fillMaxWidth()) { Text(if (busy) "Validation…" else "Valider la licence") }
        if (cached != null) Text("Clé locale : ${cached.take(12)}…")
        OutlinedButton(onClick = onClear, Modifier.fillMaxWidth()) { Text("Supprimer la clé de cet appareil") }
        status?.let { Text(it, fontSize = 12.sp) }
    }
}
