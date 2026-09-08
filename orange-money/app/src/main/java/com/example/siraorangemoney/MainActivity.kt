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
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val store by lazy { OrangeLocalStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SiraOrangeMoneyScreen(store) }
    }
}

@Composable
private fun SiraOrangeMoneyScreen(store: OrangeLocalStore) {
    var amount by remember { mutableStateOf("") }
    var transactions by remember { mutableStateOf(store.all()) }
    val commissions = transactions.sumOf { it.fee }

    MaterialTheme {
        Scaffold { padding ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("SIRA Orange Money", style = MaterialTheme.typography.headlineSmall)
                    Text("Caisse indépendante • fonctionne hors ligne", style = MaterialTheme.typography.bodyMedium)
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Commissions", style = MaterialTheme.typography.labelLarge)
                            Text("$commissions FCFA", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter(Char::isDigit) },
                        label = { Text("Montant") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            val value = amount.toLongOrNull() ?: return@Button
                            store.add(value, value / 100L)
                            transactions = store.all()
                            amount = ""
                        },
                        enabled = amount.toLongOrNull()?.let { it > 0L } == true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enregistrer l'opération")
                    }
                }
                item { Text("Historique local", style = MaterialTheme.typography.titleMedium) }
                items(transactions) { tx ->
                    ListItem(
                        headlineContent = { Text(tx.reference) },
                        supportingContent = { Text("${tx.amount} FCFA • commission ${tx.fee} FCFA") }
                    )
                }
            }
        }
    }
}
