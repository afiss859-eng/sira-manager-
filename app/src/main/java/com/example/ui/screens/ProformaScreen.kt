package com.example.ui.screens

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.DetailLevelSlider
import com.example.ui.theme.*
import com.example.viewmodel.MerchantProfile
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private data class ProformaLine(var name: String = "", var qty: Double = 1.0, var price: Double = 0.0)

@Composable
fun ProformaScreen(profile: MerchantProfile) {
    var client by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var validity by remember { mutableStateOf("7 jours") }
    var reference by remember { mutableStateOf("SIRA-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}") }
    var notes by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf(ProformaLine()) }
    val context = LocalContext.current

    val subtotal = lines.sumOf { it.qty * it.price }
    var discount by remember { mutableStateOf(0.0) }
    var tax by remember { mutableStateOf(0.0) }
    val total = (subtotal - discount).coerceAtLeast(0.0) + tax

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Proforma", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Préparez un devis PDF clair pour votre client.", color = SleekTextSecondary)
        }
        item {
            Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                Column(Modifier.padding(16.dp)) {
                    Text("Client", fontWeight = FontWeight.Bold)
                    SiraField("Nom du client", client) { client = it }
                    SiraField("Téléphone", clientPhone) { clientPhone = it }
                    SiraField("Validité", validity) { validity = it }
                    SiraField("Référence", reference) { reference = it }
                }
            }
        }
        items(lines.size) { index ->
            val line = lines[index]
            Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Article ${index + 1}", fontWeight = FontWeight.Bold)
                        if (lines.size > 1) IconButton(onClick = { lines.removeAt(index) }) { Icon(Icons.Default.Delete, contentDescription = "Supprimer") }
                    }
                    SiraField("Désignation", line.name) { line.name = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SiraField("Quantité", line.qty.toString(), Modifier.weight(1f)) { line.qty = it.replace(',', '.').toDoubleOrNull() ?: line.qty }
                        SiraField("Prix", line.price.toString(), Modifier.weight(1f)) { line.price = it.replace(',', '.').toDoubleOrNull() ?: line.price }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { lines.add(ProformaLine()) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Ajouter un article") }
        }
        item {
            Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                Column(Modifier.padding(16.dp)) {
                    SummaryLine("Sous-total", "${NumberFormat.getInstance().format(subtotal)}")
                    SummaryLine("Remise", "${NumberFormat.getInstance().format(discount)}")
                    SummaryLine("Taxe", "${NumberFormat.getInstance().format(tax)}")
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    SummaryLine("Total", "${NumberFormat.getInstance().format(total)}", strong = true)
                    Button(onClick = { exportProformaPdf(context, profile, client, clientPhone, validity, reference, lines, subtotal, discount, tax, total, notes, showAdvanced) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PictureAsPdf, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Exporter en PDF") }
                }
            }
        }
        item {
            TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(if (showAdvanced) "Masquer les options avancées" else "Afficher les options avancées") }
        }
        if (showAdvanced) {
            item {
                Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Conditions", fontWeight = FontWeight.Bold)
                        SiraField("Notes et conditions", notes, singleLine = false) { notes = it }
                        Spacer(Modifier.height(5.dp))
                        Text("Référence : $reference", fontSize = 10.sp, color = SleekTextTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SiraField(label: String, value: String, onValueChange: (String) -> Unit) = SiraField(label, value, true, onValueChange)

@Composable
private fun SiraField(label: String, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
}

@Composable
private fun SiraField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, modifier = modifier.padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
}

@Composable
private fun SummaryLine(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = if (strong) 15.sp else 12.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium, color = SleekTextPrimary)
        Text(value, fontSize = if (strong) 17.sp else 12.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold, color = if (strong) SleekBluePrimary else SleekTextPrimary)
    }
}

private fun exportProformaPdf(context: Context, profile: MerchantProfile, client: String, clientPhone: String, validity: String, reference: String, lines: List<ProformaLine>, subtotal: Double, discount: Double, tax: Double, total: Double, notes: String, showAdvanced: Boolean) {
    try {
        val pdf = PdfDocument()
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f }
        var y = 50f
        canvas.drawText(profile.shopName, 40f, y, paint); y += 25f
        paint.textSize = 11f
        canvas.drawText("Proforma $reference", 40f, y, paint); y += 20f
        canvas.drawText("Client : ${client.ifBlank { "Non renseigné" }}", 40f, y, paint); y += 18f
        canvas.drawText("Téléphone : ${clientPhone.ifBlank { "Non renseigné" }}", 40f, y, paint); y += 18f
        canvas.drawText("Validité : $validity", 40f, y, paint); y += 28f
        lines.filter { it.name.isNotBlank() }.forEach { line ->
            canvas.drawText("${line.name} | Qté ${line.qty} | Prix ${line.price}", 40f, y, paint); y += 17f
        }
        y += 12f
        canvas.drawText("Sous-total : $subtotal", 40f, y, paint); y += 18f
        canvas.drawText("Remise : $discount", 40f, y, paint); y += 18f
        canvas.drawText("Taxe : $tax", 40f, y, paint); y += 18f
        paint.textSize = 14f
        canvas.drawText("TOTAL : $total", 40f, y, paint); y += 25f
        if (showAdvanced && notes.isNotBlank()) {
            paint.textSize = 10f
            canvas.drawText("Notes : $notes", 40f, y, paint)
        }
        pdf.finishPage(page)
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(folder, "$reference.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        Toast.makeText(context, "PDF enregistré : ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export PDF impossible : ${e.message}", Toast.LENGTH_LONG).show()
    }
}
