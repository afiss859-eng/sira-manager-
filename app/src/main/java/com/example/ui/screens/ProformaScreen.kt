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
    val context = LocalContext.current
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }
    val prefs = remember { context.getSharedPreferences("sira_display_preferences", Context.MODE_PRIVATE) }
    var detailLevel by remember { mutableFloatStateOf(prefs.getFloat("proforma_detail_level", 0.34f)) }
    val showAdvanced = detailLevel >= 0.67f
    val showStandard = detailLevel >= 0.34f
    var client by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var validity by remember { mutableStateOf("7") }
    var discount by remember { mutableStateOf("0") }
    var tax by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("Cette facture proforma est établie à titre indicatif et ne constitue pas une facture définitive.") }
    val lines = remember { mutableStateListOf(ProformaLine()) }
    val reference = remember { "SIRA-PF-${SimpleDateFormat("yyyy", Locale.FRENCH).format(Date())}-${UUID.randomUUID().toString().take(4).uppercase()}" }

    val subtotal = lines.sumOf { it.qty.coerceAtLeast(0.0) * it.price.coerceAtLeast(0.0) }
    val discountValue = (discount.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)
    val taxable = subtotal - discountValue
    val taxValue = taxable * (tax.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0) / 100.0
    val total = taxable + taxValue

    Scaffold(
        containerColor = SleekBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { exportProformaPdf(context, profile, client, clientPhone, validity, reference, lines, subtotal, discountValue, taxValue, total, notes, showAdvanced) },
                containerColor = SleekBluePrimary,
                contentColor = Color.White,
                shape = SiraPillShape
            ) {
                Icon(Icons.Default.PictureAsPdf, null)
                Spacer(Modifier.width(8.dp))
                Text("PDF / Imprimer", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Facture proforma", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                Text("Préparez une offre professionnelle directement depuis SIRA Manager.", fontSize = 13.sp, color = SleekTextSecondary)
            }
            item {
                Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                    Column(Modifier.padding(16.dp)) {
                        DetailLevelSlider(
                            value = detailLevel,
                            onValueChange = {
                                detailLevel = it
                                prefs.edit().putFloat("proforma_detail_level", it).apply()
                            }
                        )
                    }
                }
            }
            item {
                Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Votre commerce", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(profile.shopName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("${profile.merchantName} • ${profile.city}", fontSize = 11.sp, color = SleekTextSecondary)
                        if (showStandard) Text(profile.phone, fontSize = 11.sp, color = SleekTextSecondary)
                        if (showAdvanced && profile.ifuNumber.isNotBlank()) Text("IFU : ${profile.ifuNumber}", fontSize = 10.sp, color = SleekTextTertiary)
                        if (showAdvanced && profile.rccmNumber.isNotBlank()) Text("RCCM : ${profile.rccmNumber}", fontSize = 10.sp, color = SleekTextTertiary)
                    }
                }
            }
            item {
                Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Client", fontWeight = FontWeight.Bold)
                        SiraField("Nom / entreprise", client) { client = it }
                        if (showStandard) SiraField("Téléphone", clientPhone) { clientPhone = it }
                        if (showAdvanced) SiraField("Validité (jours)", validity) { validity = it }
                    }
                }
            }
            item {
                Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Articles", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { lines += ProformaLine() }) { Icon(Icons.Default.Add, "Ajouter") }
                        }
                        lines.forEachIndexed { index, line ->
                            Column(Modifier.padding(vertical = 5.dp)) {
                                SiraField("Désignation", line.name) { lines[index] = line.copy(name = it) }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Box(Modifier.weight(1f)) { SiraField("Qté", line.qty.toString()) { lines[index] = line.copy(qty = it.toDoubleOrNull() ?: 0.0) } }
                                    Box(Modifier.weight(1f)) { SiraField("Prix unitaire", line.price.toString()) { lines[index] = line.copy(price = it.toDoubleOrNull() ?: 0.0) } }
                                    IconButton(onClick = { if (lines.size > 1) lines.removeAt(index) }) { Icon(Icons.Default.Delete, "Supprimer") }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
            if (showStandard) {
                item {
                    Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Tarification", fontWeight = FontWeight.Bold)
                            SiraField("Remise (FCFA)", discount) { discount = it }
                            SiraField("Taxe (%)", tax) { tax = it }
                            Spacer(Modifier.height(8.dp))
                            SummaryLine("Sous-total", numberFormat.format(subtotal) + " FCFA")
                            SummaryLine("Remise", "- ${numberFormat.format(discountValue)} FCFA")
                            SummaryLine("Taxes", numberFormat.format(taxValue) + " FCFA")
                            HorizontalDivider(Modifier.padding(vertical = 7.dp))
                            SummaryLine("TOTAL", numberFormat.format(total) + " FCFA", true)
                        }
                    }
                }
            } else {
                item {
                    Card(border = BorderStroke(1.dp, SleekOutline), colors = CardDefaults.cardColors(containerColor = SleekSurface), shape = SiraCardShape) {
                        Column(Modifier.padding(16.dp)) {
                            SummaryLine("TOTAL", numberFormat.format(total) + " FCFA", true)
                        }
                    }
                }
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
}

@Composable
private fun SiraField(label: String, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
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
        val c = page.canvas
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        p.color = android.graphics.Color.rgb(25, 25, 25)
        p.textSize = 14f
        c.drawText("SIRA — ${profile.shopName}", 42f, 48f, p)
        p.textSize = 24f; p.isFakeBoldText = true; c.drawText("FACTURE PROFORMA", 285f, 55f, p)
        p.isFakeBoldText = false; p.textSize = 10f
        c.drawText("N° $reference", 410f, 72f, p)
        c.drawText("Émise le ${SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(Date())}", 410f, 87f, p)
        c.drawText("Valable $validity jour(s)", 410f, 102f, p)
        c.drawText("${profile.merchantName} • ${profile.city} • ${profile.phone}", 42f, 78f, p)
        if (showAdvanced) c.drawText("IFU: ${profile.ifuNumber}   RCCM: ${profile.rccmNumber}", 42f, 94f, p)
        c.drawText("CLIENT", 42f, 140f, p); c.drawText(client.ifBlank { "Client" }, 42f, 158f, p)
        if (clientPhone.isNotBlank()) c.drawText(clientPhone, 42f, 174f, p)
        var y = 215f
        p.isFakeBoldText = true
        c.drawText("DÉSIGNATION", 42f, y, p); c.drawText("QTÉ", 350f, y, p); c.drawText("PRIX", 410f, y, p); c.drawText("MONTANT", 485f, y, p)
        p.isFakeBoldText = false; y += 18f
        val fmt = NumberFormat.getIntegerInstance(Locale.FRENCH)
        lines.filter { it.name.isNotBlank() || it.price > 0 }.take(25).forEach { l ->
            val amount = l.qty * l.price
            c.drawText(l.name.take(38), 42f, y, p); c.drawText(fmt.format(l.qty), 350f, y, p); c.drawText(fmt.format(l.price), 410f, y, p); c.drawText(fmt.format(amount), 485f, y, p); y += 20f
        }
        y += 12f; c.drawText("Sous-total : ${fmt.format(subtotal)} FCFA", 380f, y, p); y += 18f; c.drawText("Remise : - ${fmt.format(discount)} FCFA", 380f, y, p); y += 18f; c.drawText("Taxes : ${fmt.format(tax)} FCFA", 380f, y, p); y += 24f
        p.isFakeBoldText = true; p.textSize = 15f; c.drawText("TOTAL : ${fmt.format(total)} FCFA", 360f, y, p)
        p.isFakeBoldText = false; p.textSize = 10f
        if (showAdvanced) {
            y += 44f; c.drawText("Conditions", 42f, y, p); y += 16f
            notes.lines().take(6).forEach { line -> c.drawText(line.take(95), 42f, y, p); y += 14f }
        }
        y = 780f; c.drawText("Le commerçant", 70f, y, p); c.drawText("Le client", 420f, y, p)
        c.drawText("SIRA — Chemin d’aujourd’hui, avenir de demain", 170f, 825f, p)
        pdf.finishPage(page)
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        dir.mkdirs()
        val file = File(dir, "$reference.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        Toast.makeText(context, "Proforma PDF créée : ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Échec PDF : ${e.message}", Toast.LENGTH_LONG).show()
    }
}
