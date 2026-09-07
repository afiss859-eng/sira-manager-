package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CashChannel
import com.example.data.entity.CashTransaction
import com.example.viewmodel.SiraViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CashflowScreen(
    viewModel: SiraViewModel,
    onNewCashOpClick: () -> Unit
) {
    val cashTx by viewModel.cashTransactions.collectAsState()
    var selectedChannelFilter by remember { mutableStateOf<CashChannel?>(null) }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.FRENCH) }

    // Balances per channel
    val cashRegisterBalance = remember(cashTx) {
        cashTx.filter { it.channel == CashChannel.ESPECES }
            .sumOf { if (it.type.isCredit) it.amount else -(it.amount + it.fee) }
    }

    val orangeMoneyBalance = remember(cashTx) {
        cashTx.filter { it.channel == CashChannel.ORANGE_MONEY }
            .sumOf { if (it.type.isCredit) it.amount else -(it.amount + it.fee) }
    }

    val moovMoneyBalance = remember(cashTx) {
        cashTx.filter { it.channel == CashChannel.MOOV_MONEY }
            .sumOf { if (it.type.isCredit) it.amount else -(it.amount + it.fee) }
    }

    val corisBalance = remember(cashTx) {
        cashTx.filter { it.channel == CashChannel.CORIS_MONEY }
            .sumOf { if (it.type.isCredit) it.amount else -(it.amount + it.fee) }
    }

    val totalCashflow = cashRegisterBalance + orangeMoneyBalance + moovMoneyBalance + corisBalance

    val filteredTransactions = remember(cashTx, selectedChannelFilter) {
        if (selectedChannelFilter == null) cashTx
        else cashTx.filter { it.channel == selectedChannelFilter }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewCashOpClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_cash_op")
            ) {
                Icon(Icons.Default.AddCard, contentDescription = "Opération Caisse")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("cashflow_screen")
        ) {
            // Header Balance Card
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trésorerie & Soldes Multi-Canaux",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Total liquidités disponibles : ${numberFormat.format(totalCashflow.toLong())} FCFA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Channel balances grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChannelBalanceMiniCard(
                            title = "Espèces",
                            amount = "${numberFormat.format(cashRegisterBalance.toLong())} F",
                            modifier = Modifier.weight(1f)
                        )
                        ChannelBalanceMiniCard(
                            title = "Orange M.",
                            amount = "${numberFormat.format(orangeMoneyBalance.toLong())} F",
                            modifier = Modifier.weight(1f)
                        )
                        ChannelBalanceMiniCard(
                            title = "Moov M.",
                            amount = "${numberFormat.format(moovMoneyBalance.toLong())} F",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Channel Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedChannelFilter == null,
                        onClick = { selectedChannelFilter = null },
                        label = { Text("Tous les comptes") }
                    )
                }
                items(CashChannel.values()) { channel ->
                    FilterChip(
                        selected = selectedChannelFilter == channel,
                        onClick = { selectedChannelFilter = channel },
                        label = { Text(channel.label) }
                    )
                }
            }

            // Transactions List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORIQUE DES MOUVEMENTS (${filteredTransactions.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun mouvement de caisse enregistré.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        CashTransactionRow(
                            tx = tx,
                            numberFormat = numberFormat,
                            dateFormat = dateFormat
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelBalanceMiniCard(
    title: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CashTransactionRow(
    tx: CashTransaction,
    numberFormat: NumberFormat,
    dateFormat: SimpleDateFormat
) {
    val isCredit = tx.type.isCredit

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCredit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isCredit) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = tx.description,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${tx.channel.label} • ${dateFormat.format(Date(tx.timestamp))}" +
                                if (!tx.beneficiaryOrPayer.isNullOrBlank()) " • ${tx.beneficiaryOrPayer}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else "-"}${numberFormat.format(tx.amount.toLong())} F",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCredit) Color(0xFF15803D) else Color(0xFFB91C1C)
                )
                if (tx.fee > 0) {
                    Text(
                        text = "Frais: ${numberFormat.format(tx.fee.toLong())} F",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
