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
import com.example.data.entity.Product
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    initialProduct: Product? = null,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Alimentation Générale") }
    var quantityText by remember { mutableStateOf(initialProduct?.quantity?.toString() ?: "10") }
    var purchasePriceText by remember { mutableStateOf(initialProduct?.purchasePrice?.toInt()?.toString() ?: "5000") }
    var salePriceText by remember { mutableStateOf(initialProduct?.salePrice?.toInt()?.toString() ?: "6500") }
    var minAlertStockText by remember { mutableStateOf(initialProduct?.minAlertStock?.toString() ?: "5") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf(
        "Alimentation Générale",
        "Boissons & Eau",
        "Hygiène & Entretien",
        "Textile & Dan Fani",
        "Cosmétique & Beauté",
        "Téléphonie & Accessoires",
        "Quincaillerie & Électricité",
        "Autre"
    )

    val purchasePrice = purchasePriceText.toDoubleOrNull() ?: 0.0
    val salePrice = salePriceText.toDoubleOrNull() ?: 0.0
    val margin = salePrice - purchasePrice
    val marginPercent = if (purchasePrice > 0) ((margin / purchasePrice) * 100).toInt() else 0
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("add_edit_product_dialog"),
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
                    Text(
                        text = if (initialProduct == null) "Nouveau Produit" else "Modifier Produit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Désignation du produit *") },
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) }
                    )

                    // Category dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Catégorie") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
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
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("Stock initial *") },
                            modifier = Modifier.weight(1f).testTag("product_quantity_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = minAlertStockText,
                            onValueChange = { minAlertStockText = it },
                            label = { Text("Alerte mini") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = purchasePriceText,
                            onValueChange = { purchasePriceText = it },
                            label = { Text("Prix d'achat (FCFA) *") },
                            modifier = Modifier.weight(1f).testTag("product_purchase_price_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = salePriceText,
                            onValueChange = { salePriceText = it },
                            label = { Text("Prix de vente (FCFA) *") },
                            modifier = Modifier.weight(1f).testTag("product_sale_price_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    // Margin live calculator card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (margin >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bénéfice par unité :",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${numberFormat.format(margin.toLong())} FCFA ($marginPercent%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (margin >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Code-barres / Référence (Facultatif)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            val q = quantityText.toIntOrNull() ?: 0
                            val min = minAlertStockText.toIntOrNull() ?: 5
                            val product = Product(
                                id = initialProduct?.id ?: 0L,
                                name = name.trim(),
                                category = category,
                                quantity = q,
                                purchasePrice = purchasePrice,
                                salePrice = salePrice,
                                minAlertStock = min,
                                barcode = barcode.ifBlank { null }
                            )
                            onSave(product)
                        },
                        enabled = name.isNotBlank() && purchasePrice > 0 && salePrice > 0,
                        modifier = Modifier.weight(1f).testTag("save_product_button")
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}
