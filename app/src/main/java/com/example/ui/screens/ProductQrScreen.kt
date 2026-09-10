package com.example.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductQrBackup
import com.example.data.ProductQrStats
import com.example.data.entity.ProductQrCode
import com.example.printer.SiraBluetoothPrinter
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.theme.*
import com.example.viewmodel.SiraViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductQrScreen(viewModel: SiraViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out -> out.writer().use { writer -> writer.write(ProductQrBackup.toCsv(savedCodes)) } } ?: error("Fichier inaccessible")
        }.onSuccess { Toast.makeText(context, "Sauvegarde QR exportée", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(context, it.message ?: "Export impossible", Toast.LENGTH_SHORT).show() }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Fichier inaccessible") }
                .onSuccess { csv -> val count = viewModel.importProductQrCsv(csv); Toast.makeText(context, "$count QR restauré(s) sans doublon", Toast.LENGTH_LONG).show() }
                .onFailure { Toast.makeText(context, it.message ?: "Restauration impossible", Toast.LENGTH_SHORT).show() }
        }
    }

    LaunchedEffect(editing) {
        editing?.let {
            scannedCode = it.payload
            productName = it.productName
            variant = it.variant
            priceText = it.price.toLong().toString()
            category = viewModel.products.value.firstOrNull { p -> p.id == it.productId }?.category.orEmpty()
        }
    }

    fun clearEditor() { editing = null; scannedCode = ""; productName = ""; variant = ""; category = ""; priceText = ""; stockText = "0" }
    fun persistEditor() {
        val price = priceText.toDoubleOrNull() ?: 0.0
        if (scannedCode.isBlank() || productName.isBlank() || price <= 0) { scannerStatus = "! Renseignez le code, le nom et un prix valide"; return }
        editing?.let { viewModel.deleteProductQrCode(it) }
        val finalName = if (variant.isBlank()) productName else "$productName — $variant"
        viewModel.saveScannedProductQr(scannedCode, finalName, price, category, stockText.toIntOrNull() ?: 0)
        scannerStatus = if (editing == null) "✓ Produit + QR enregistrés" else "✓ QR modifié"
        clearEditor()
    }

    val visibleCodes = savedCodes.filter { it.productName.contains(search, true) || it.variant.contains(search, true) || it.payload.contains(search, true) }
    val topScanned = ProductQrStats.top(context, savedCodes.map { it.payload }, 3)

    Column(Modifier.fillMaxSize().background(SleekBackground).testTag("product_qr_screen")) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("QR produits", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp, color = SleekTextPrimary)
                    Text("Scan → produit → facture → stock → reçu Bluetooth, avec fonctionnement local.", fontSize = 12.sp, color = SleekTextSecondary, modifier = Modifier.padding(top = 3.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    IconButton(onClick = { exportLauncher.launch("sira-qr-backup.csv") }, modifier = Modifier.testTag("qr_export_button")) { Icon(Icons.Default.FileDownload, "Exporter") }
                    IconButton(onClick = { importLauncher.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) }, modifier = Modifier.testTag("qr_import_button")) { Icon(Icons.Default.FileUpload, "Restaurer") }
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(modifier = Modifier.fillMaxWidth(), shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = .65f))) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip("QR", savedCodes.size.toString(), Modifier.weight(1f))
                        StatChip("Variantes", savedCodes.count { it.variant.isNotBlank() }.toString(), Modifier.weight(1f))
                        StatChip("Scans", topScanned.sumOf { it.second }.toString(), Modifier.weight(1f))
                    }
                    if (topScanned.isNotEmpty()) Text("Plus scannés : " + topScanned.joinToString(" • ") { payload -> "${savedCodes.firstOrNull { it.payload == payload.first }?.productName ?: payload.first} (${payload.second})" }, fontSize = 9.sp, color = SleekTextSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp).padding(bottom = 10.dp), maxLines = 2)
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(modifier = Modifier.fillMaxWidth(), shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = .65f))) {
                Column(Modifier.padding(15.dp)) {
                    Text(if (editing == null) "Préparer un QR produit" else "Modifier le QR produit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Text("La référence reste le payload du QR : aucune connexion réseau n'est nécessaire à la caisse.", fontSize = 10.sp, color = SleekTextSecondary, modifier = Modifier.padding(top = 3.dp))
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = scannedCode, onValueChange = { scannedCode = it }, modifier = Modifier.weight(1f).testTag("qr_scanned_code"), label = { Text("Code / référence") }, singleLine = true)
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(onClick = { scannerStatus = null; scannerOpen = true }, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.QrCodeScanner, "Scanner") }
                    }
                    scannerStatus?.let { Text(it, fontSize = 10.sp, color = if (it.startsWith("!")) SleekError else SleekBluePrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = productName, onValueChange = { productName = it }, modifier = Modifier.weight(1f).testTag("qr_product_name"), label = { Text("Nom") }, singleLine = true)
                        OutlinedTextField(value = variant, onValueChange = { variant = it }, modifier = Modifier.weight(.78f), label = { Text("Variante") }, placeholder = { Text("1L, Rouge…") }, singleLine = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = category, onValueChange = { category = it }, modifier = Modifier.weight(1f), label = { Text("Catégorie") }, singleLine = true)
                        OutlinedTextField(value = stockText, onValueChange = { stockText = it.filter(Char::isDigit) }, modifier = Modifier.weight(.62f), label = { Text("Stock initial") }, singleLine = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth().testTag("qr_price_input"), label = { Text("Prix de vente") }, trailingIcon = { Text("XOF", fontWeight = FontWeight.SemiBold, color = SleekTextSecondary, modifier = Modifier.padding(end = 10.dp)) }, singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = ::persistEditor, modifier = Modifier.weight(1f).testTag("qr_save_button"), shape = SiraPillShape) { Icon(if (editing == null) Icons.Default.Add else Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(if (editing == null) "Enregistrer + QR" else "Enregistrer les modifications") }
                        if (editing != null) OutlinedButton(onClick = ::clearEditor, shape = SiraPillShape) { Text("Annuler") }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { val results = savedCodes.map { printer.printProductQrLabel(it.productName, it.price, it.payload, "FCFA") }; Toast.makeText(context, "${results.count { it.isSuccess }} QR envoyé(s)", Toast.LENGTH_LONG).show() }, modifier = Modifier.weight(1f), shape = SiraPillShape, enabled = savedCodes.isNotEmpty()) { Icon(Icons.Default.Print, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Imprimer tous") }
                OutlinedButton(onClick = { exportLauncher.launch("sira-qr-backup.csv") }, modifier = Modifier.weight(1f), shape = SiraPillShape, enabled = savedCodes.isNotEmpty()) { Icon(Icons.Default.SaveAlt, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Sauvegarder") }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().testTag("qr_saved_search"), label = { Text("Rechercher un QR") }, singleLine = true, leadingIcon = { Icon(Icons.Default.QrCode2, null) })
            Spacer(Modifier.height(8.dp))
            Text("QR ENREGISTRÉS · ${visibleCodes.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, color = SleekTextTertiary)
        }
        if (visibleCodes.isEmpty()) Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) { Text("Aucun QR enregistré", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary) }
        else LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxSize()) { items(visibleCodes, key = { it.id }) { code -> ProductQrCard(code, numberFormat, { editing = code }, { viewModel.deleteProductQrCode(code) }, { viewModel.saveProductQrCode(viewModel.products.value.firstOrNull { it.id == code.productId }, code.price) }, { printer.printProductQrLabel(code.productName, code.price, code.payload, "FCFA").onFailure { Toast.makeText(context, it.message ?: "Impression impossible", Toast.LENGTH_SHORT).show() } }, { shareQrImage(context, code) }) } }
    }
    if (scannerOpen) BarcodeScannerDialog(statusMessage = null, onBarcodeScanned = { code -> scannedCode = code.trim(); scannerStatus = "✓ Référence capturée"; scannerOpen = false }, onDismiss = { scannerOpen = false })
}

@Composable private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = SleekSurfaceVariant, border = BorderStroke(1.dp, SleekOutline.copy(alpha = .45f))) { Column(Modifier.padding(8.dp)) { Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = SleekTextPrimary); Text(label, fontSize = 9.sp, color = SleekTextSecondary) } }
}

@Composable private fun ProductQrCard(code: ProductQrCode, numberFormat: NumberFormat, onEdit: () -> Unit, onDelete: () -> Unit, onRegenerate: () -> Unit, onPrint: () -> Unit, onShare: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = .62f)), shadowElevation = 1.dp) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            val bitmap = remember(code.payload) { createQrBitmap(code.payload, 180) }
            Image(bitmap.asImageBitmap(), "QR ${code.productName}", Modifier.size(104.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(code.productName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary, maxLines = 2)
                if (code.variant.isNotBlank()) Text("Variante : ${code.variant}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary)
                Text("${numberFormat.format(code.price.toLong())} F", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SleekBluePrimary, modifier = Modifier.padding(top = 3.dp))
                Text("Code : ${code.payload}", fontSize = 9.sp, color = SleekTextSecondary, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 5.dp)) {
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
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap -> for (x in 0 until size) for (y in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE) }
}

private fun shareQrImage(context: android.content.Context, code: ProductQrCode) {
    runCatching {
        val bitmap = createQrBitmap(code.payload, 700)
        val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "SIRA-QR-${code.productName.take(24)}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png"); if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SIRA") }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Impossible de créer l'image")
        context.contentResolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) } ?: error("Impossible d'écrire l'image")
        context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "image/png"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Partager le QR"))
    }.onFailure { Toast.makeText(context, it.message ?: "Partage impossible", Toast.LENGTH_SHORT).show() }
}