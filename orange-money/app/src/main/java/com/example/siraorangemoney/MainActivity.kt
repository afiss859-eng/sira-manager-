package com.sira.orangemoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val store by lazy { OrangeLocalStore(this) }
    private val licenseClient by lazy { OrangeLicenseClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SiraOrangeMoneyApp(store, licenseClient) }
    }
}

@Composable
private fun SiraOrangeMoneyApp(store: OrangeLocalStore, licenseClient: OrangeLicenseClient) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var transactions by remember { mutableStateOf(store.all()) }
    var query by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("TOUT") }
    var amount by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var licenseInput by remember { mutableStateOf(licenseClient.cachedLicenseKey.orEmpty()) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var mandatoryUpdate by remember { mutableStateOf<Any?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        mandatoryUpdate = withContext(Dispatchers.IO) {
            runCatching { com.sira.orangemoney.update.AppAutoUpdate(licenseClientContext(store)).check() }
                .getOrNull()
                ?.takeIf { it.mandatory }
        }
    }

    val filtered = remember(transactions, query, selectedType) {
        val needle = query.trim().lowercase(Locale.ROOT)
        transactions.filter { tx ->
            val matchesType = selectedType == "TOUT" || tx.type == selectedType
            val matchesQuery = needle.isBlank() || listOf(tx.reference, tx.type, tx.counterparty).any { it.lowercase(Locale.ROOT).contains(needle) }
            matchesType && matchesQuery
        }
    }
    val total = transactions.sumOf { it.amount }
    val fees = transactions.sumOf { it.fee }
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.FRANCE) }
    val tabs = listOf("Portefeuille", "Encaisser", "Transférer", "Historique", "Sécurité")

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("SIRA Orange Money", style = MaterialTheme.typography.titleLarge)
                            Text("Caisse indépendante • hors connexion", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, label ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
                    }
                }
                when (selectedTab) {
                    0 -> WalletScreen(total, fees, transactions, formatter)
                    1 -> OperationScreen(
                        title = "Encaisser",
                        button = "Enregistrer l'encaissement",
                        amount = amount,
                        counterparty = counterparty,
                        onAmountChange = { amount = it.filter(Char::isDigit) },
                        onCounterpartyChange = { counterparty = it.take(80) },
                        onConfirm = {
                            val value = amount.toLongOrNull()
                            if (value == null || value <= 0) status = "Saisissez un montant supérieur à 0."
                            else {
                                store.add(value, value / 100L, "ENCAISSEMENT", counterparty.trim())
                                transactions = store.all()
                                amount = ""; counterparty = ""; status = "Encaissement enregistré localement."
                            }
                        },
                        status = status
                    )
                    2 -> OperationScreen(
                        title = "Transférer",
                        button = "Enregistrer le transfert",
                        amount = amount,
                        counterparty = counterparty,
                        onAmountChange = { amount = it.filter(Char::isDigit) },
                        onCounterpartyChange = { counterparty = it.take(80) },
                        onConfirm = {
                            val value = amount.toLongOrNull()
                            if (value == null || value <= 0) status = "Saisissez un montant supérieur à 0."
                            else {
                                store.add(value, value / 100L, "TRANSFERT", counterparty.trim())
                                transactions = store.all()
                                amount = ""; counterparty = ""; status = "Transfert enregistré localement."
                            }
                        },
                        status = status
                    )
                    3 -> HistoryScreen(filtered, query, selectedType, { query = it }, { selectedType = it }, formatter)
                    4 -> SecurityScreen(
                        licenseInput = licenseInput,
                        onLicenseInputChange = { licenseInput = it.take(80) },
                        cachedKey = licenseClient.cachedLicenseKey,
                        onActivate = {
                            busy = true
                            scope.launch {
                                val result = licenseClient.activate(licenseInput)
                                busy = false
                                status = result.fold({ "Licence active : $it" }, { it.message ?: "Activation impossible." })
                            }
                        },
                        busy = busy,
                        status = status,
                        onClear = {
                            licenseClient.clearLocalLicense()
                            licenseInput = ""
                            status = "Licence locale supprimée."
                        }
                    )
                }
            }
        }
    }

    if (mandatoryUpdate != null) {
        val info = mandatoryUpdate as com.sira.orangemoney.update.AppAutoUpdate.UpdateInfo
        AlertDialog(
            onDismissRequest = {},
            title = { Text(info.title.ifBlank { "Mise à jour obligatoire" }) },
            text = { Text("La version ${info.latestVersion} est obligatoire.\n\n${info.notes.take(1200)}") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { com.sira.orangemoney.update.AppAutoUpdate(licenseClientContext(store)).downloadAndInstall(info) }
                    }
                }) { Text("Mettre à jour maintenant") }
            }
        )
    }
}

private fun licenseClientContext(store: OrangeLocalStore): android.content.Context {
    return store.writableDatabase.path.let { store.javaClass.getDeclaredField("mContext").apply { isAccessible = true }.get(store) as android.content.Context }
}

@Composable
private fun WalletScreen(total: Long, fees: Long, transactions: List<OrangeTransaction>, formatter: NumberFormat) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MetricCard("Volume des opérations", "${formatter.format(total)} FCFA") }
        item { MetricCard("Commissions locales", "${formatter.format(fees)} FCFA") }
        item { Text("Dernières opérations", style = MaterialTheme.typography.titleMedium) }
        items(transactions.take(5)) { tx -> TransactionRow(tx, formatter) }
    }
}

@Composable
private fun OperationScreen(
    title: String,
    button: String,
    amount: String,
    counterparty: String,
    onAmountChange: (String) -> Unit,
    onCounterpartyChange: (String) -> Unit,
    onConfirm: () -> Unit,
    status: String?
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("Mode hors connexion : l'opération est d'abord enregistrée sur l'appareil.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(value = amount, onValueChange = onAmountChange, label = { Text("Montant (FCFA)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = counterparty, onValueChange = onCounterpartyChange, label = { Text("Destinataire / client") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onConfirm, enabled = amount.toLongOrNull()?.let { it > 0L } == true, modifier = Modifier.fillMaxWidth()) { Text(button) }
        status?.let { AssistChip(onClick = {}, label = { Text(it) }) }
    }
}

@Composable
private fun HistoryScreen(
    transactions: List<OrangeTransaction>,
    query: String,
    selectedType: String,
    onQueryChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    formatter: NumberFormat
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = query, onValueChange = onQueryChange, label = { Text("Rechercher") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("TOUT", "ENCAISSEMENT", "TRANSFERT").forEach { type ->
                FilterChip(selected = selectedType == type, onClick = { onTypeChange(type) }, label = { Text(type) })
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(transactions) { tx -> TransactionRow(tx, formatter) }
        }
    }
}

@Composable
private fun SecurityScreen(
    licenseInput: String,
    onLicenseInputChange: (String) -> Unit,
    cachedKey: String?,
    onActivate: () -> Unit,
    busy: Boolean,
    status: String?,
    onClear: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sécurité & licence", style = MaterialTheme.typography.headlineSmall)
        Text("La licence reste séparée de SIRA Business. Une validation récente permet un usage hors connexion.")
        OutlinedTextField(value = licenseInput, onValueChange = onLicenseInputChange, label = { Text("Clé SIRA Orange Money") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onActivate, enabled = licenseInput.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Validation…" else "Activer / vérifier") }
        if (cachedKey != null) Text("Licence locale présente : ${cachedKey.take(12)}…")
        OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("Effacer la licence locale") }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun MetricCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(label, style = MaterialTheme.typography.labelLarge); Text(value, style = MaterialTheme.typography.headlineMedium) } }
}

@Composable
private fun TransactionRow(tx: OrangeTransaction, formatter: NumberFormat) {
    Card(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text("${tx.type} • ${formatter.format(tx.amount)} FCFA") },
            supportingContent = { Text("${tx.counterparty.ifBlank { tx.reference }} • ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(tx.timestamp))}\nCommission ${formatter.format(tx.fee)} FCFA") }
        )
    }
}
