package com.example.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ProductQrCode
import com.example.printer.SiraBluetoothPrinter
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
    val context = LocalContext.current
    val printer = remember { SiraBluetoothPrinter(context) }
    val savedCodes by viewModel.productQrCodes.collectAsState()
    var scannerOpen by remember { mutableStateOf(false) }
    var scannedCode by remember { mutableStateOf("") }
    var scannerStatus by remember { mutableStateOf<String?>(null) }
    var productName by remember { mutableStateOf("") }
    var variant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("0") }
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ProductQrCode?>(null) }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

    LaunchedEffect(editing) {
        editing?.let {
            scannedCode = it.payload
            productName = it.productName
            variant = it.variant
            priceText = it.price.toLong().toString()
            category = viewModel.products.value.firstOrNull { p -> p.id == editing?.productId }?.category.orEmpty()
        }
    }

    fun persistEditor() {
        val price = priceText.toDoubleOrNull() ?: 0.0
        if (scannedCode.isBlank() || productName.isBlank() || price <= 0) {
            scannerStatus = "! Renseignez le code, le nom et un prix valide"
            return
        }
        editing?.let { viewModel.deleteProductQrCode(it) }
        val finalName = if (variant.isBlank()) productName else "$productName — $variant"
        viewModel.saveScannedProductQr(scannedCode, finalName, price, category, stockText.toIntOrNull() ?: 0)
        scannerStatus = if (editing == null) "✓ Produit + QR enregistrés" else "✓ QR modifié"
        editing = null
        scannedCode = ""; productName = ""; variant = ""; category = ""; priceText = ""; stockText = "0"
    }

    Column(Modifier.fillMaxSize().background(SleekBackground).testTag("product_qr_screen")) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("QR produits", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, color = SleekTextPrimary)
                    Spacer(Modifier.height(3.dp))
                    Text("Un scan ajoute automatiquement l'article, son prix et +1 quantité au panier.", fontSize = 12.sp, color = SleekTextSecondary)
                }
                Surface(shape = CircleShape, color = SleekBlueContainer, modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.QrCode2, null, tint = SleekBlueOnContainer) } }
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Text(if (editing == null) "Préparer un QR produit" else "Modifier le QR produit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Text("Le QR mémorise la référence locale : les ventes restent utilisables hors connexion.", fontSize = 10.sp, color = SleekTextSecondary, modifier = Modifier.padding(top = 3.dp))
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = scannedCode, onValueChange = { scannedCode = it }, modifier = Modifier.weight(1f).testTag("qr_scanned_code"), label = { Text("Code / référence") }, placeholder = { Text("Ex. 615xxxxxxxxxx") }, singleLine = true)
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(onClick = { scannerStatus = null; scannerOpen = true }, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = SleekBluePrimary, contentColor = Color.White)) { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner le produit") }
                    }
                    scannerStatus?.let { Text(it, fontSize = 10.sp, color = if (it.startsWith("!")) SleekError else SleekBluePrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = productName, onValueChange = { productName = it }, modifier = Modifier.weight(1f).testTag("qr_product_name"), label = { Text("Nom") }, singleLine = true)
                        OutlinedTextField(value = variant, onValueChange = { variant = it }, modifier = Modifier.weight(0.78f), label = { Text("Variante") }, placeholder = { Text("1L, Rouge…") }, singleLine = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = category, onValueChange = { category = it }, modifier = Modifier.weight(1f), label = { Text("Catégorie") }, singleLine = true)
                        OutlinedTextField(value = stockText, onValueChange = { stockText = it.filter(Char::isDigit) }, modifier = Modifier.weight(0.62f), label = { Text("Stock initial") }, singleLine = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth().testTag("qr_price_input"), label = { Text("Prix de vente (FCFA)") }, singleLine = true, trailingIcon = { Text("XOF", fontWeight = FontWeight.SemiBold, color = SleekTextSecondary, modifier = Modifier.padding(end = 10.dp)) })
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = ::persistEditor, modifier = Modifier.weight(1f).testTag("qr_save_button"), shape = SiraPillShape) { Icon(if (editing == null) Icons.Default.Add else Icons.Default.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(if (editing == null) "Enregistrer + QR" else "Enregistrer les modifications", fontWeight = FontWeight.SemiBold) }
                        if (editing != null) OutlinedButton(onClick = { editing = null; scannedCode = ""; productName = ""; variant = ""; category = ""; priceText = ""; stockText = "0" }, shape = SiraPillShape) { Text("Annuler") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().testTag("qr_saved_search"), label = { Text("Rechercher un QR") }, singleLine = true, leadingIcon = { Icon(Icons.Default.QrCode2, null) })
            Spacer(Modifier.height(8.dp))
            val visibleCodes = savedCodes.filter { it.productName.contains(search, true) || it.variant.contains(search, true) || it.payload.contains(search, true) }
            Text("QR ENREGISTRÉS · ${visibleCodes.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = SleekTextTertiary)
        }
        val visibleCodes = savedCodes.filter { it.productName.contains(search, true) || it.variant.contains(search, true) || it.payload.contains(search, true) }
        if (visibleCodes.isEmpty()) Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) { Text("Aucun QR enregistré", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary) }
        else LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxSize()) {
            items(visibleCodes, key = { it.id }) { code ->
                ProductQrCard(
                    code = code,
                    numberFormat = numberFormat,
                    onEdit = { editing = code },
                    onDelete = { viewModel.deleteProductQrCode(code) },
                    onRegenerate = { viewModel.saveProductQrCode(viewModel.products.value.firstOrNull { it.id == code.productId }, code.price) },
                    onPrint = { printer.printProductQrLabel(code.productName, code.price, code.payload, "FCFA").onFailure { Toast.makeText(context, it.message ?: "Impression impossible", Toast.LENGTH_SHORT).show() }.onSuccess { Toast.makeText(context, "QR envoyé à l'imprimante", Toast.LENGTH_SHORT).show() } },
                    onShare = { shareQrImage(context, code) }
                )
            }
        }
    }
    if (scannerOpen) BarcodeScannerDialog(statusMessage = null, onBarcodeScanned = { code -> scannedCode = code.trim(); scannerStatus = "✓ Référence capturée"; scannerOpen = false }, onDismiss = { scannerOpen = false })
}

@Composable
private fun ProductQrCard(code: ProductQrCode, numberFormat: NumberFormat, onEdit: () -> Unit, onDelete: () -> Unit, onRegenerate: () -> Unit, onPrint: () -> Unit, onShare: () -> Unit) {
    Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.62f)), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            val bitmap = remember(code.payload) { createQrBitmap(code.payload, 180) }
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR ${code.productName}", modifier = Modifier.size(104.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(code.productName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary, maxLines = 2)
                if (code.variant.isNotBlank()) Text("Variante : ${code.variant}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary)
                Spacer(Modifier.height(3.dp))
                Text("${numberFormat.format(code.price.toLong())} F", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SleekBluePrimary)
                Text("Code : ${code.payload}", fontSize = 9.sp, color = SleekTextSecondary, maxLines = 2)
                Text("1 scan = +1 article • prix repris dans la facture.", fontSize = 10.sp, color = SleekTextSecondary, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Edit, "Modifier", tint = SleekBluePrimary, modifier = Modifier.size(17.dp)) }
                    IconButton(onClick = onPrint, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Print, "Imprimer", tint = SleekTextSecondary, modifier = Modifier.size(17.dp)) }
                    IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Share, "Partager", tint = SleekTextSecondary, modifier = Modifier.size(17.dp)) }
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Refresh, "Régénérer", tint = SleekTextSecondary, modifier = Modifier.size(17.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Delete, "Supprimer", tint = SleekError, modifier = Modifier.size(17.dp)) }
                }
            }
        }
    }
}

private fun createQrBitmap(content: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap -> for (x in 0 until size) for (y in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE) }
}

private fun shareQrImage(context: android.content.Context, code: ProductQrCode) {
    runCatching {
        val bitmap = createQrBitmap(code.payload, 700)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "SIRA-QR-${code.productName.take(24)}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SIRA")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Impossible de créer l'image")
        context.contentResolver.openOutputStream(uri).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Partager le QR"))
    }.onFailure { Toast.makeText(context, it.message ?: "Partage impossible", Toast.LENGTH_SHORT).show() }
}
