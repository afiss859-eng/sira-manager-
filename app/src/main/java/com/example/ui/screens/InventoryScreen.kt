package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Product
import com.example.ui.theme.*
import com.example.viewmodel.SiraViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: SiraViewModel,
    onAddProductClick: () -> Unit,
    onEditProductClick: (Product) -> Unit
) {
    val products by viewModel.products.collectAsState()
    var selectedCategory by remember { mutableStateOf("Tous") }
    var filterLowStockOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }
    val categories = remember(products) { listOf("Tous") + products.map { it.category }.distinct() }
    val filteredProducts = remember(products, selectedCategory, filterLowStockOnly, searchQuery) {
        products.filter { product ->
            val matchCat = selectedCategory == "Tous" || product.category == selectedCategory
            val matchLowStock = !filterLowStockOnly || product.isLowStock
            val matchQuery = searchQuery.isBlank() || product.name.contains(searchQuery, true) || product.barcode?.contains(searchQuery, true) == true
            matchCat && matchLowStock && matchQuery
        }
    }

    Scaffold(
        containerColor = SleekBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProductClick,
                containerColor = SleekBluePrimary,
                contentColor = Color.White,
                shape = SiraPillShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 5.dp),
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ajouter un produit", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).testTag("inventory_screen")) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("Stock", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, color = SleekTextPrimary)
                        Spacer(Modifier.height(3.dp))
                        Text("${products.size} références • ${products.sumOf { it.quantity }} unités", fontSize = 13.sp, color = SleekTextSecondary)
                    }
                    Surface(shape = CircleShape, color = SleekBlueContainer, modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Inventory2, contentDescription = null, tint = SleekBlueOnContainer, modifier = Modifier.size(19.dp)) }
                    }
                }
                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = SiraPillShape,
                    color = SleekSurface,
                    border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("stock_search_input")
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SleekTextSecondary, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = SleekTextPrimary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isBlank()) Text("Rechercher par nom ou code-barres", fontSize = 12.sp, color = SleekTextTertiary)
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer", tint = SleekTextTertiary, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    items(categories) { cat ->
                        StockChip(cat, selectedCategory == cat && !filterLowStockOnly) {
                            selectedCategory = cat
                            filterLowStockOnly = false
                        }
                    }
                    item {
                        StockChip("Stock faible", filterLowStockOnly, danger = true) { filterLowStockOnly = !filterLowStockOnly }
                    }
                }
                Spacer(Modifier.height(11.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("INVENTAIRE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = SleekTextTertiary)
                    Text("${filteredProducts.size} affichés", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SleekBluePrimary)
                }
            }

            if (filteredProducts.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(shape = CircleShape, color = SleekSurfaceVariant, modifier = Modifier.size(54.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Inventory2, contentDescription = null, tint = SleekTextTertiary, modifier = Modifier.size(25.dp)) }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Aucun produit trouvé", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                            Text("Essayez un autre filtre ou ajoutez une référence.", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp + 82.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductStockCard(
                            product = product,
                            numberFormat = numberFormat,
                            onIncrement = { viewModel.quickStockAdjust(product.id, +1) },
                            onDecrement = { viewModel.quickStockAdjust(product.id, -1) },
                            onEdit = { onEditProductClick(product) },
                            onDelete = { viewModel.deleteProduct(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockChip(label: String, selected: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val accent = if (danger) SleekError else SleekBluePrimary
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium) },
        shape = SiraPillShape,
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.11f), selectedLabelColor = accent, containerColor = SleekSurface),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = SleekOutline.copy(alpha = 0.62f), selectedBorderColor = accent.copy(alpha = 0.24f))
    )
}

@Composable
private fun ProductStockCard(
    product: Product,
    numberFormat: NumberFormat,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val accent = if (product.isLowStock) SleekError else SleekBluePrimary

    Surface(
        shape = SiraCardShape,
        color = SleekSurface,
        border = BorderStroke(1.dp, if (product.isLowStock) SleekError.copy(alpha = 0.28f) else SleekOutline.copy(alpha = 0.62f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(13.dp), color = accent.copy(alpha = 0.09f), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Inventory2, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary, maxLines = 1)
                    Text(product.category, fontSize = 10.sp, color = SleekTextSecondary)
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Options", tint = SleekTextTertiary, modifier = Modifier.size(19.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Modifier") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { showMenu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Supprimer", color = SleekError) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = SleekError) }, onClick = { showMenu = false; onDelete() })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StockValue("Achat", "${numberFormat.format(product.purchasePrice.toLong())} F", Modifier.weight(1f))
                StockValue("Vente", "${numberFormat.format(product.salePrice.toLong())} F", Modifier.weight(1f), SleekBluePrimary)
                StockValue("Marge", "+${numberFormat.format((product.salePrice - product.purchasePrice).toLong())} F", Modifier.weight(1f), SleekSuccess)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = SleekOutline.copy(alpha = 0.55f))
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Quantité disponible", fontSize = 10.sp, color = SleekTextTertiary)
                    Text(if (product.isLowStock) "Niveau à surveiller" else "Stock normal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (product.isLowStock) SleekError else SleekSuccess)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilledIconButton(
                        onClick = onDecrement,
                        enabled = product.quantity > 0,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SleekErrorContainer, contentColor = SleekError),
                        modifier = Modifier.size(34.dp).testTag("btn_stock_minus_${product.id}")
                    ) { Icon(Icons.Default.Remove, contentDescription = "Vente rapide (-1)", modifier = Modifier.size(18.dp)) }
                    Surface(shape = SiraPillShape, color = if (product.isLowStock) SleekError.copy(alpha = 0.08f) else SleekSurfaceVariant, modifier = Modifier.widthIn(min = 48.dp)) {
                        Text(product.quantity.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = if (product.isLowStock) SleekError else SleekTextPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    FilledIconButton(
                        onClick = onIncrement,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SleekBlueContainer, contentColor = SleekBlueOnContainer),
                        modifier = Modifier.size(34.dp).testTag("btn_stock_plus_${product.id}")
                    ) { Icon(Icons.Default.Add, contentDescription = "Ravitaillement rapide (+1)", modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StockValue(label: String, value: String, modifier: Modifier, valueColor: Color = SleekTextPrimary) {
    Column(modifier) {
        Text(label, fontSize = 9.sp, color = SleekTextTertiary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1)
    }
}
