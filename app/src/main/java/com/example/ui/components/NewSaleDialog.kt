package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ProductQrStats
import com.example.data.entity.*
import com.example.engine.PaymentEngine
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleDialog(products: List<Product>, customers: List<Customer>, currentPaymentEngine: PaymentEngine, onDismiss: () -> Unit, onCompleteSale: (Sale, List<SaleItem>) -> Unit) {
    val context = LocalContext.current
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerNameInput by remember { mutableStateOf("Client de passage") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.ESPECES) }
    var notes by remember { mutableStateOf("") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var productSearch by remember { mutableStateOf("") }
    var scannerOpen by remember { mutableStateOf(false) }
    var scannerStatus by remember { mutableStateOf<String?>(null) }
    val cart = remember { mutableStateMapOf<Long, Int>() }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }
    val cartItems = remember(cart.toMap(), products) {
        cart.mapNotNull { (prodId, qty) -> products.find { it.id == prodId }?.let { product ->
            if (qty > 0) SaleItem(saleId = 0, productId = product.id, productName = product.name, quantity = qty, unitPrice = product.salePrice, unitCost = product.purchasePrice, subtotal = qty * product.salePrice) else null
        } }
    }
    val visibleProducts = remember(products, productSearch) { products.filter { productSearch.isBlank() || it.name.contains(productSearch, true) || it.barcode?.contains(productSearch, true) == true } }
    val totalAmount = cartItems.sumOf { it.subtotal }
    val totalCost = cartItems.sumOf { it.quantity * it.unitCost }
    val estimatedProfit = totalAmount - totalCost
    val totalItems = cartItems.sumOf { it.quantity }
    LaunchedEffect(scannerStatus) { if (scannerStatus != null) { kotlinx.coroutines.delay(1400); scannerStatus = null } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(.96f).fillMaxHeight(.94f).testTag("new_sale_dialog"), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(20.dp, 18.dp, 12.dp, 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("Nouvelle vente", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Scannez un QR ou ajoutez les articles puis encaissez", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Fermer") } }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            ExposedDropdownMenuBox(expanded = customerDropdownExpanded, onExpandedChange = { customerDropdownExpanded = it }) {
                                OutlinedTextField(value = selectedCustomer?.fullName ?: customerNameInput, onValueChange = { customerNameInput = it; selectedCustomer = null }, label = { Text("Client") }, leadingIcon = { Icon(Icons.Default.PersonOutline, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(customerDropdownExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true, shape = RoundedCornerShape(14.dp))
                                ExposedDropdownMenu(customerDropdownExpanded, { customerDropdownExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Client de passage", fontWeight = FontWeight.SemiBold) }, leadingIcon = { Icon(Icons.Default.PersonOutline, null) }, onClick = { selectedCustomer = null; customerNameInput = "Client de passage"; customerDropdownExpanded = false })
                                    customers.forEach { cust -> DropdownMenuItem(text = { Column { Text(cust.fullName, fontWeight = FontWeight.SemiBold); Text("${cust.phone} • ${cust.kycStatus.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, leadingIcon = { Icon(Icons.Default.Person, null) }, onClick = { selectedCustomer = cust; customerNameInput = cust.fullName; customerDropdownExpanded = false }) }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = productSearch, onValueChange = { productSearch = it }, modifier = Modifier.weight(1f).testTag("product_search_input"), singleLine = true, shape = RoundedCornerShape(14.dp), leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (productSearch.isNotEmpty()) IconButton(onClick = { productSearch = "" }) { Icon(Icons.Default.Clear, "Effacer") } }, placeholder = { Text("Produit, code ou QR") })
                                Spacer(Modifier.width(8.dp))
                                FilledIconButton(onClick = { scannerStatus = null; scannerOpen = true }, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.QrCodeScanner, "Scanner un code-barres") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Produits", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); Text("${visibleProducts.size} disponibles", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(visibleProducts, key = { it.id }) { product ->
                        val qty = cart[product.id] ?: 0; val unavailable = product.quantity <= 0
                        Surface(shape = RoundedCornerShape(18.dp), color = if (qty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .24f) else MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, if (qty > 0) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else MaterialTheme.colorScheme.outline.copy(alpha = .12f)), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge); Text("${numberFormat.format(product.salePrice.toLong())} FCFA • Stock ${product.quantity}", style = MaterialTheme.typography.labelSmall, color = if (unavailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(36.dp)) { IconButton(onClick = { if (qty > 1) cart[product.id] = qty - 1 else cart.remove(product.id) }, enabled = qty > 0) { Icon(Icons.Default.Remove, "Retirer") } }
                                    Text(qty.toString(), Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) { IconButton(onClick = { if (!unavailable && qty < product.quantity) cart[product.id] = qty + 1 }, enabled = !unavailable && qty < product.quantity) { Icon(Icons.Default.Add, "Ajouter", tint = Color.White) } }
                                }
                            }
                        }
                    }
                }
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Règlement", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Text("$totalItems article(s) • ${numberFormat.format(totalAmount.toLong())} FCFA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("${numberFormat.format(totalAmount.toLong())} FCFA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) }
                        LazyRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(PaymentMethod.values().toList()) { method -> FilterChip(selected = selectedPaymentMethod == method, onClick = { selectedPaymentMethod = method }, leadingIcon = { Icon(when (method) { PaymentMethod.ESPECES -> Icons.Default.Payments; PaymentMethod.ORANGE_MONEY, PaymentMethod.MOOV_MONEY, PaymentMethod.CORIS_MONEY -> Icons.Default.PhoneAndroid; PaymentMethod.A_CREDIT -> Icons.Default.Schedule }, null, Modifier.size(16.dp)) }, label = { Text(method.label, fontSize = 12.sp) }) } }
                        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Bénéfice estimé", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("+${numberFormat.format(estimatedProfit.toLong())} FCFA", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary) }; Button(onClick = { val sale = Sale(reference = "VTE-${System.currentTimeMillis().toString().takeLast(6)}", customerId = selectedCustomer?.id, customerName = selectedCustomer?.fullName ?: customerNameInput.trim().ifBlank { "Client de passage" }, totalAmount = totalAmount, profitAmount = estimatedProfit, paymentMethod = selectedPaymentMethod, status = SaleStatus.PAYE, notes = notes.ifBlank { null }); onCompleteSale(sale, cartItems) }, enabled = cartItems.isNotEmpty(), modifier = Modifier.testTag("confirm_sale_button"), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Valider la vente", fontWeight = FontWeight.Bold) } }
                    }
                }
            }
        }
    }
    if (scannerOpen) BarcodeScannerDialog(statusMessage = scannerStatus, onBarcodeScanned = { scannedCode ->
        val normalizedCode = scannedCode.trim()
        val product = products.firstOrNull { it.barcode?.trim().equals(normalizedCode, true) }
        when {
            product == null -> scannerStatus = "! Code $normalizedCode : produit introuvable"
            product.quantity <= 0 -> scannerStatus = "! ${product.name} : stock épuisé"
            (cart[product.id] ?: 0) >= product.quantity -> scannerStatus = "! ${product.name} : stock maximum atteint"
            else -> { cart[product.id] = (cart[product.id] ?: 0) + 1; ProductQrStats.recordScan(context, normalizedCode); scannerStatus = "✓ ${product.name} ajouté • quantité ${cart[product.id]}" }
        }
    }, onDismiss = { scannerOpen = false })
}