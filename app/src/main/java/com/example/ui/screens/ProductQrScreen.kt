package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ProductQrCode
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.theme.*
import com.example.viewmodel.SiraViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductQrScreen(viewModel: SiraViewModel) {
    val savedCodes by viewModel.productQrCodes.collectAsState()
    var scannerOpen by remember { mutableStateOf(false) }
    var scannedCode by remember { mutableStateOf("") }
    var scannerStatus by remember { mutableStateOf<String?>(null) }
    var productName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("0") }
    var search by remember { mutableStateOf("") }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

    Column(Modifier.fillMaxSize().background(SleekBackground).testTag("product_qr_screen")) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("QR produits", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, color = SleekTextPrimary)
                    Spacer(Modifier.height(3.dp))
                    Text("Scannez une référence une fois, associez son nom et son prix, puis utilisez ce QR à chaque vente.", fontSize = 12.sp, color = SleekTextSecondary)
                }
                Surface(shape = CircleShape, color = SleekBlueContainer, modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.QrCode2, null, tint = SleekBlueOnContainer) } }
            }
            Spacer(Modifier.height(14.dp))
            Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Text("1 · Scanner le produit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Text("Le code-barres ou QR existant devient la référence SIRA utilisée à la caisse.", fontSize = 10.sp, color = SleekTextSecondary, modifier = Modifier.padding(top = 3.dp))
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = scannedCode, onValueChange = { scannedCode = it }, modifier = Modifier.weight(1f).testTag("qr_scanned_code"), label = { Text("Code scanné") }, placeholder = { Text("Ex. 615xxxxxxxxxx") }, singleLine = true)
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(onClick = { scannerStatus = null; scannerOpen = true }, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = SleekBluePrimary, contentColor = Color.White)) { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner le produit") }
                    }
                    scannerStatus?.let { Text(it, fontSize = 10.sp, color = if (it.startsWith("!")) SleekError else SleekBluePrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
                    Spacer(Modifier.height(10.dp))
                    Text("2 · Nom et prix", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = productName, onValueChange = { productName = it }, modifier = Modifier.fillMaxWidth().testTag("qr_product_name"), label = { Text("Nom du produit") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = category, onValueChange = { category = it }, modifier = Modifier.weight(1f), label = { Text("Catégorie") }, singleLine = true)
                        OutlinedTextField(value = stockText, onValueChange = { stockText = it.filter(Char::isDigit) }, modifier = Modifier.weight(0.62f), label = { Text("Stock") }, singleLine = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth().testTag("qr_price_input"), label = { Text("Prix de vente (FCFA)") }, singleLine = true, trailingIcon = { Text("XOF", fontWeight = FontWeight.SemiBold, color = SleekTextSecondary, modifier = Modifier.padding(end = 10.dp)) })
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { val price = priceText.toDoubleOrNull() ?: 0.0; viewModel.saveScannedProductQr(scannedCode, productName, price, category, stockText.toIntOrNull() ?: 0); scannerStatus = "✓ Produit enregistré et QR prêt à utiliser"; scannedCode = ""; productName = ""; category = ""; priceText = ""; stockText = "0" }, enabled = scannedCode.isNotBlank() && productName.isNotBlank() && (priceText.toDoubleOrNull() ?: 0.0) > 0, modifier = Modifier.fillMaxWidth().testTag("qr_save_button"), shape = SiraPillShape) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Enregistrer + générer le QR", fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().testTag("qr_saved_search"), label = { Text("Rechercher un QR enregistré") }, singleLine = true, leadingIcon = { Icon(Icons.Default.QrCode2, null) })
            Spacer(Modifier.height(10.dp))
            val visibleCodes = savedCodes.filter { it.productName.contains(search, true) || it.payload.contains(search, true) }
            Text("QR ENREGISTRÉS · ${visibleCodes.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = SleekTextTertiary)
        }
        val visibleCodes = savedCodes.filter { it.productName.contains(search, true) || it.payload.contains(search, true) }
        if (visibleCodes.isEmpty()) Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) { Text("Aucun QR enregistré", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary) }
        else LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxSize()) { items(visibleCodes, key = { it.id }) { code -> ProductQrCard(code, numberFormat, { viewModel.deleteProductQrCode(code) }, { viewModel.saveProductQrCode(viewModel.products.value.firstOrNull { it.id == code.productId }, code.price) }) } }
    }
    if (scannerOpen) BarcodeScannerDialog(statusMessage = null, onBarcodeScanned = { code -> scannedCode = code.trim(); scannerStatus = "✓ Référence capturée"; scannerOpen = false }, onDismiss = { scannerOpen = false })
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
                Text("Code : ${code.payload}", fontSize = 9.sp, color = SleekTextSecondary, maxLines = 2)
                Text("Au scan : nom + prix + article repris dans la facture.", fontSize = 10.sp, color = SleekTextSecondary, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { AssistChip(onClick = onRegenerate, label = { Text("Régénérer", fontSize = 10.sp) }, leadingIcon = { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(15.dp)) }); AssistChip(onClick = onDelete, label = { Text("Supprimer", fontSize = 10.sp, color = SleekError) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = SleekError, modifier = Modifier.size(15.dp)) }) }
            }
        }
    }
}

private fun createQrBitmap(content: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap -> for (x in 0 until size) for (y in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE) }
}
