package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Product
import com.example.data.entity.ProductQrCode
import com.example.ui.theme.*
import com.example.viewmodel.SiraViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductQrScreen(viewModel: SiraViewModel) {
    val products by viewModel.products.collectAsState()
    val savedCodes by viewModel.productQrCodes.collectAsState()
    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    var priceText by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }

    val selectedProduct = products.firstOrNull { it.id == selectedProductId }
    val filteredProducts = remember(products, search) {
        products.filter { it.name.contains(search, true) || it.category.contains(search, true) || it.barcode?.contains(search, true) == true }
    }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

    Column(Modifier.fillMaxSize().background(SleekBackground).testTag("product_qr_screen")) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("QR produits", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, color = SleekTextPrimary)
                    Spacer(Modifier.height(3.dp))
                    Text("Enregistrez un QR avec le produit et son prix de vente.", fontSize = 12.sp, color = SleekTextSecondary)
                }
                Surface(shape = CircleShape, color = SleekBlueContainer, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.QrCode2, contentDescription = null, tint = SleekBlueOnContainer, modifier = Modifier.size(22.dp)) }
                }
            }
            Spacer(Modifier.height(14.dp))

            Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Text("Nouveau QR", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Text("Le prix peut être différent du prix actuel du catalogue.", fontSize = 10.sp, color = SleekTextSecondary, modifier = Modifier.padding(top = 3.dp))
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth().testTag("qr_product_search"),
                        label = { Text("Rechercher un produit") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.QrCode2, null) }
                    )
                    Spacer(Modifier.height(7.dp))
                    ExposedDropdownMenuBox(expanded = selectedProductId == -1L, onExpandedChange = { selectedProductId = if (selectedProductId == -1L) null else -1L }) {
                        OutlinedTextField(
                            value = selectedProduct?.let { "${it.name} · ${numberFormat.format(it.salePrice.toLong())} F" } ?: "Sélectionner un produit",
                            onValueChange = {}, readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Produit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = selectedProductId == -1L) }
                        )
                        ExposedDropdownMenu(expanded = selectedProductId == -1L, onDismissRequest = { selectedProductId = null }) {
                            filteredProducts.take(60).forEach { product ->
                                DropdownMenuItem(
                                    text = { Text("${product.name} · ${numberFormat.format(product.salePrice.toLong())} F") },
                                    onClick = { selectedProductId = product.id; priceText = product.salePrice.toLong().toString() }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth().testTag("qr_price_input"),
                        label = { Text("Prix de vente (FCFA)") },
                        singleLine = true,
                        trailingIcon = { Text("XOF", fontWeight = FontWeight.SemiBold, color = SleekTextSecondary, modifier = Modifier.padding(end = 10.dp)) }
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            selectedProduct?.let { product ->
                                val price = priceText.toDoubleOrNull() ?: product.salePrice
                                if (price > 0) viewModel.saveProductQrCode(product, price)
                            }
                        },
                        enabled = selectedProduct != null && (priceText.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.fillMaxWidth().testTag("qr_save_button"),
                        shape = SiraPillShape
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enregistrer le QR", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("QR ENREGISTRÉS · ${savedCodes.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = SleekTextTertiary)
        }

        if (savedCodes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
                Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QrCode2, null, tint = SleekTextTertiary, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Aucun QR enregistré", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                        Text("Choisissez un produit puis enregistrez son prix.", fontSize = 11.sp, color = SleekTextSecondary)
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxSize()) {
                items(savedCodes, key = { it.id }) { code ->
                    ProductQrCard(code, numberFormat, onDelete = { viewModel.deleteProductQrCode(code) }, onRegenerate = { viewModel.saveProductQrCode(products.firstOrNull { it.id == code.productId }, code.price) })
                }
            }
        }
    }
}

@Composable
private fun ProductQrCard(code: ProductQrCode, numberFormat: NumberFormat, onDelete: () -> Unit, onRegenerate: () -> Unit) {
    Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.62f)), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            val bitmap = remember(code.payload) { createQrBitmap(code.payload, 180) }
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR ${code.productName}", modifier = Modifier.size(104.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(code.productName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text("${numberFormat.format(code.price.toLong())} F", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SleekBluePrimary)
                Text("Scannable par SIRA · ${code.currency}", fontSize = 10.sp, color = SleekTextSecondary, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = onRegenerate, label = { Text("Régénérer", fontSize = 10.sp) }, leadingIcon = { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(15.dp)) })
                    AssistChip(onClick = onDelete, label = { Text("Supprimer", fontSize = 10.sp, color = SleekError) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = SleekError, modifier = Modifier.size(15.dp)) })
                }
            }
        }
    }
}

private fun createQrBitmap(content: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        for (x in 0 until size) for (y in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
    }
}
