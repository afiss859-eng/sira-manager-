package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.CashChannel
import com.example.data.entity.CashOpType
import com.example.data.entity.CashTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashOperationDialog(
    onDismiss: () -> Unit,
    onSave: (CashTransaction) -> Unit
) {
    var opType by remember { mutableStateOf(CashOpType.DEPENSE) }
    var channel by remember { mutableStateOf(CashChannel.ESPECES) }
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("0") }
    var beneficiary by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var channelExpanded by remember { mutableStateOf(false) }
    var opTypeExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("cash_operation_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Opération Caisse / Transfert",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type selector dropdown
                    ExposedDropdownMenuBox(
                        expanded = opTypeExpanded,
                        onExpandedChange = { opTypeExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = opType.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Nature de l'opération") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = opTypeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = opTypeExpanded,
                            onDismissRequest = { opTypeExpanded = false }
                        ) {
                            listOf(
                                CashOpType.DEPENSE,
                                CashOpType.DEPOT,
                                CashOpType.RETRAIT,
                                CashOpType.TRANSFERT_ENTRANT,
                                CashOpType.TRANSFERT_SORTANT
                            ).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        opType = type
                                        opTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Channel selector dropdown
                    ExposedDropdownMenuBox(
                        expanded = channelExpanded,
                        onExpandedChange = { channelExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = channel.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Canal / Compte") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = channelExpanded,
                            onDismissRequest = { channelExpanded = false }
                        ) {
                            CashChannel.values().forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text(ch.label) },
                                    onClick = {
                                        channel = ch
                                        channelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Montant (FCFA) *") },
                            modifier = Modifier.weight(1.3f).testTag("cash_amount_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = feeText,
                            onValueChange = { feeText = it },
                            label = { Text("Frais (FCFA)") },
                            modifier = Modifier.weight(0.9f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = beneficiary,
                        onValueChange = { beneficiary = it },
                        label = { Text("Bénéficiaire ou Donneur d'ordre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone associé (Orange/Moov)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Motif * (ex: Achat fournitures, Transport)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            val fee = feeText.toDoubleOrNull() ?: 0.0
                            val ref = "TX-${System.currentTimeMillis().toString().takeLast(6)}"
                            val tx = CashTransaction(
                                type = opType,
                                channel = channel,
                                amount = amount,
                                fee = fee,
                                reference = ref,
                                beneficiaryOrPayer = beneficiary.ifBlank { null },
                                phone = phone.ifBlank { null },
                                description = description.trim().ifBlank { opType.label }
                            )
                            onSave(tx)
                        },
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.weight(1f).testTag("save_cash_op_button")
                    ) {
                        Text("Valider")
                    }
                }
            }
        }
    }
}
