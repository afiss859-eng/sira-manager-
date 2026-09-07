package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.theme.*
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

    val cashRegisterBalance = remember(cashTx) { cashTx.filter { it.channel == CashChannel.ESPECES }.sumOf { if (it.type.isCredit) it.amount else -(it.amount + it.fee) } }
    val orangeMoneyBalance = remember(cashTx) { cashTx.filter { it.channel == CashChannel.ORANGE_MONEY }.sumOf { if (it.type.isCredit) it.amount else -(it.amount + it.fee) } }
    val moovMoneyBalance = remember(cashTx) { cashTx.filter { it.channel == CashChannel.MOOV_MONEY }.sumOf { if (it.type.isCredit) it.amount else -(it.amount + it.fee) } }
    val corisBalance = remember(cashTx) { cashTx.filter { it.channel == CashChannel.CORIS_MONEY }.sumOf { if (it.type.isCredit) it.amount else -(it.amount + it.fee) } }
    val totalCashflow = cashRegisterBalance + orangeMoneyBalance + moovMoneyBalance + corisBalance
    val filteredTransactions = remember(cashTx, selectedChannelFilter) { selectedChannelFilter?.let { channel -> cashTx.filter { it.channel == channel } } ?: cashTx }

    Scaffold(
        containerColor = SleekBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewCashOpClick, containerColor = SleekBluePrimary, contentColor = Color.White, shape = SiraPillShape, elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 5.dp), modifier = Modifier.testTag("fab_new_cash_op")) {
                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nouvelle opération", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).testTag("cashflow_screen")) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text("Trésorerie", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, color = SleekTextPrimary)
                Spacer(Modifier.height(3.dp))
                Text("Une vue simple de vos liquidités et mouvements.", fontSize = 13.sp, color = SleekTextSecondary)
                Spacer(Modifier.height(15.dp))
                Surface(shape = SiraScreenShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.7f)), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(19.dp)) {
                        Text("SOLDE TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp, color = SleekTextTertiary)
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(numberFormat.format(totalCashflow.toLong()), fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, color = SleekTextPrimary)
                            Spacer(Modifier.width(6.dp)); Text("FCFA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary, modifier = Modifier.padding(bottom = 5.dp))
                        }
                        Spacer(Modifier.height(15.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { BalanceCell("Espèces", cashRegisterBalance, SleekBluePrimary, Modifier.weight(1f)); BalanceCell("Orange", orangeMoneyBalance, Color(0xFFFF7900), Modifier.weight(1f)) }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { BalanceCell("Moov", moovMoneyBalance, SleekSecondary, Modifier.weight(1f)); BalanceCell("Coris", corisBalance, SleekTertiary, Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(13.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) { item { CashFilterChip("Tous", selectedChannelFilter == null) { selectedChannelFilter = null } }; items(CashChannel.values()) { channel -> CashFilterChip(channel.label, selectedChannelFilter == channel) { selectedChannelFilter = channel } } }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("MOUVEMENTS RÉCENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = SleekTextTertiary); Text("${filteredTransactions.size}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SleekBluePrimary) }
            }
            if (filteredTransactions.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(28.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(shape = CircleShape, color = SleekSurfaceVariant, modifier = Modifier.size(54.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SleekTextTertiary, modifier = Modifier.size(25.dp)) } }
                            Spacer(Modifier.height(10.dp)); Text("Aucun mouvement", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary); Text("Les opérations de caisse apparaîtront ici.", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp + 82.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(filteredTransactions, key = { it.id }) { tx -> CashTransactionRow(tx, numberFormat, dateFormat) } }
            }
        }
    }
}

@Composable
private fun BalanceCell(title: String, amount: Double, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.08f), border = BorderStroke(1.dp, color.copy(alpha = 0.1f)), modifier = modifier) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) { Text(title, fontSize = 10.sp, color = SleekTextSecondary); Spacer(Modifier.height(2.dp)); Text("${NumberFormat.getIntegerInstance(Locale.FRENCH).format(amount.toLong())} F", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color) }
    }
}

@Composable
private fun CashFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium) }, shape = SiraPillShape, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SleekBlueContainer, selectedLabelColor = SleekBlueOnContainer, containerColor = SleekSurface), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = SleekOutline.copy(alpha = 0.65f), selectedBorderColor = SleekBluePrimary.copy(alpha = 0.22f)))
}

@Composable
private fun CashTransactionRow(tx: CashTransaction, numberFormat: NumberFormat, dateFormat: SimpleDateFormat) {
    val isCredit = tx.type.isCredit
    val accent = if (isCredit) SleekSuccess else SleekError
    Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.62f)), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(13.dp), color = accent.copy(alpha = 0.1f), modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp)) } }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) { Text(tx.description, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary, maxLines = 1); Text("${tx.channel.label} • ${dateFormat.format(Date(tx.timestamp))}" + if (!tx.beneficiaryOrPayer.isNullOrBlank()) " • ${tx.beneficiaryOrPayer}" else "", fontSize = 10.sp, color = SleekTextTertiary, maxLines = 1) }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) { Text("${if (isCredit) "+" else "-"}${numberFormat.format(tx.amount.toLong())} F", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent); if (tx.fee > 0) Text("Frais ${numberFormat.format(tx.fee.toLong())} F", fontSize = 9.sp, color = SleekTextTertiary) }
        }
    }
}
