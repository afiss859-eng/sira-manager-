package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.*
import com.example.engine.PaymentEngine
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleDialog(
    products: List<Product>,
    customers: List<Customer>,
    currentPaymentEngine: PaymentEngine,
    onDismiss: () -> Unit,
    onCompleteSale: (Sale, List<SaleItem>) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerNameInput by remember { mutableStateOf("Client de passage") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.ESPECES) }
    var notes by remember { mutableStateOf("") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    // Map of productId to quantity to sell
    val cart = remember { mutableStateMapOf<Long, Int>() }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

    // Calculate total
    val cartItems = remember(cart.toMap(), products) {
        cart.mapNotNull { (prodId, qty) ->
            val product = products.find { it.id == prodId }
            if (product != null && qty > 0) {
                SaleItem(
                    saleId = 0,
                    productId = product.id,
                    productName = product.name,
                    quantity = qty,
                    unitPrice = product.salePrice,
                    unitCost = product.purchasePrice,
                    subtotal = qty * product.salePrice
                )
            } else null
        }
    }

    val totalAmount = cartItems.sumOf { it.subtotal }
    val totalCost = cartItems.sumOf { it.quantity * it.unitCost }
    val estimatedProfit = totalAmount - totalCost

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("new_sale_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ShoppingCartCheckout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nouvelle Vente / Encaissement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Customer selection
                ExposedDropdownMenuBox(
                    expanded = customerDropdownExpanded,
                    onExpandedChange = { customerDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.fullName ?: customerNameInput,
                        onValueChange = {
                            customerNameInput = it
                            selectedCustomer = null
                        },
                        label = { Text("Client (Sélectionner ou saisir)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Client de passage (Comptant)") },
                            onClick = {
                                selectedCustomer = null
                                customerNameInput = "Client de passage"
                                customerDropdownExpanded = false
                            }
                        )
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(cust.fullName, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${cust.phone} • ${cust.kycStatus.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCustomer = cust
                                    customerNameInput = cust.fullName
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Product selection list
                Text(
                    text = "Sélectionnez les articles :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(products) { product ->
                        val inCartQty = cart[product.id] ?: 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (inCartQty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (inCartQty > 0) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${numberFormat.format(product.salePrice.toLong())} FCFA • Dispo: ${product.quantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (product.quantity <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Quick +/- stepper
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (inCartQty > 0) cart[product.id] = inCartQty - 1
                                        },
                                        enabled = inCartQty > 0
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Moins")
                                    }

                                    Text(
                                        text = inCartQty.toString(),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            if (inCartQty < product.quantity) {
                                                cart[product.id] = inCartQty + 1
                                            }
                                        },
                                        enabled = inCartQty < product.quantity
                                    ) {
                                        Icon(
                                            Icons.Default.AddCircle,
                                            contentDescription = "Plus",
                                            tint = if (inCartQty < product.quantity) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Payment Method Selector
                Text(
                    text = "Mode de règlement :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.values().forEach { method ->
                        FilterChip(
                            selected = selectedPaymentMethod == method,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(method.label, fontSize = 11.sp) }
                        )
                    }
                }

                // Payment Engine warning note if electronic
                if (selectedPaymentMethod != PaymentMethod.ESPECES && selectedPaymentMethod != PaymentMethod.A_CREDIT) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Moteur actif : ${currentPaymentEngine.displayName}. " +
                                       if (!currentPaymentEngine.isConfigured()) "Mode manuel : confirmez la réception du SMS avant remise de la marchandise."
                                       else "Intégration connectée.",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Total Summary Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${cartItems.sumOf { it.quantity }} article(s) au panier",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Bénéfice estimé : +${numberFormat.format(estimatedProfit.toLong())} F",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Text(
                            text = "${numberFormat.format(totalAmount.toLong())} FCFA",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Bottom Buttons
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
                            val reference = "VTE-${System.currentTimeMillis().toString().takeLast(6)}"
                            val sale = Sale(
                                reference = reference,
                                customerId = selectedCustomer?.id,
                                customerName = selectedCustomer?.fullName ?: customerNameInput.trim().ifBlank { "Client de passage" },
                                totalAmount = totalAmount,
                                profitAmount = estimatedProfit,
                                paymentMethod = selectedPaymentMethod,
                                status = SaleStatus.PAYE,
                                notes = notes.ifBlank { null }
                            )
                            onCompleteSale(sale, cartItems)
                        },
                        enabled = cartItems.isNotEmpty(),
                        modifier = Modifier.weight(1.5f).testTag("confirm_sale_button")
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Encaisser & Facture")
                    }
                }
            }
        }
    }
}
