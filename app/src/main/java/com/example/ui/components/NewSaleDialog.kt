package com.example.ui.components

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
    var productSearch by remember { mutableStateOf("") }
    val cart = remember { mutableStateMapOf<Long, Int>() }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

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

    val visibleProducts = remember(products, productSearch) {
        products.filter { product ->
            productSearch.isBlank() ||
                product.name.contains(productSearch, ignoreCase = true) ||
                product.barcode?.contains(productSearch, ignoreCase = true) == true
        }
    }

    val totalAmount = cartItems.sumOf { it.subtotal }
    val totalCost = cartItems.sumOf { it.quantity * it.unitCost }
    val estimatedProfit = totalAmount - totalCost
    val totalItems = cartItems.sumOf { it.quantity }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .testTag("new_sale_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nouvelle vente",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ajoutez les articles puis encaissez",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                    label = { Text("Client") },
                                    placeholder = { Text("Client de passage") },
                                    leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = customerDropdownExpanded,
                                    onDismissRequest = { customerDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Client de passage", fontWeight = FontWeight.SemiBold)
                                                Text("Paiement immédiat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
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
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
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

                            OutlinedTextField(
                                value = productSearch,
                                onValueChange = { productSearch = it },
                                modifier = Modifier.fillMaxWidth().testTag("product_search_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (productSearch.isNotEmpty()) {
                                        IconButton(onClick = { productSearch = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Effacer la recherche")
                                        }
                                    }
                                },
                                placeholder = { Text("Rechercher un produit ou scanner son code") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Produits", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${visibleProducts.size} disponibles",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(visibleProducts, key = { it.id }) { product ->
                        val qty = cart[product.id] ?: 0
                        val unavailable = product.quantity <= 0
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (qty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (qty > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${numberFormat.format(product.salePrice.toLong())} FCFA • Stock ${product.quantity}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (unavailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (qty > 1) cart[product.id] = qty - 1 else cart.remove(product.id)
                                            },
                                            enabled = qty > 0
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Retirer")
                                        }
                                    }
                                    Text(
                                        qty.toString(),
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        IconButton(
                                            onClick = { if (!unavailable && qty < product.quantity) cart[product.id] = qty + 1 },
                                            enabled = !unavailable && qty < product.quantity
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Règlement", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${totalItems} article(s) • ${numberFormat.format(totalAmount.toLong())} FCFA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${numberFormat.format(totalAmount.toLong())} FCFA",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(PaymentMethod.values().toList()) { method ->
                                FilterChip(
                                    selected = selectedPaymentMethod == method,
                                    onClick = { selectedPaymentMethod = method },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (method) {
                                                PaymentMethod.ESPECES -> Icons.Default.Payments
                                                PaymentMethod.ORANGE_MONEY, PaymentMethod.MOOV_MONEY, PaymentMethod.CORIS_MONEY -> Icons.Default.PhoneAndroid
                                                PaymentMethod.A_CREDIT -> Icons.Default.Schedule
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(method.label, fontSize = 12.sp) }
                                )
                            }
                        }

                        if (selectedPaymentMethod != PaymentMethod.ESPECES && selectedPaymentMethod != PaymentMethod.A_CREDIT) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${currentPaymentEngine.displayName} • " +
                                            if (!currentPaymentEngine.isConfigured()) "Vérifiez le SMS de confirmation avant remise."
                                            else "Moteur connecté.",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bénéfice estimé", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "+${numberFormat.format(estimatedProfit.toLong())} FCFA",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Button(
                                onClick = {
                                    val sale = Sale(
                                        reference = "VTE-${System.currentTimeMillis().toString().takeLast(6)}",
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
                                modifier = Modifier.testTag("confirm_sale_button"),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Valider la vente", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
