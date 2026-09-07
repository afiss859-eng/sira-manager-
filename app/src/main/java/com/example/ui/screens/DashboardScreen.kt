package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.AdminManager
import com.example.data.entity.Sale
import com.example.license.KeypadLayoutType
import com.example.ui.theme.*
import com.example.viewmodel.SiraNavTab
import com.example.viewmodel.SiraViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: SiraViewModel,
    onOpenNewSale: () -> Unit,
    onOpenAddProduct: () -> Unit,
    onOpenCashOp: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onNavigateToTab: (SiraNavTab) -> Unit,
    onViewSaleInvoice: (Sale) -> Unit
) {
    val products by viewModel.products.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val cashTx by viewModel.cashTransactions.collectAsState()
    val merchantProfile by viewModel.merchantProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val licenseConfig by viewModel.licenseConfig.collectAsState()
    val context = LocalContext.current
    val adminManager = remember { AdminManager.getInstance(context) }
    val directives by adminManager.directives.collectAsState()
    val isSuspended = adminManager.isUserSuspended(currentUser.id)
    var showCustomizationDialog by remember { mutableStateOf(false) }

    if (showCustomizationDialog) com.example.ui.components.AppCustomizationDialog(viewModel = viewModel, licenseConfig = licenseConfig, onDismiss = { showCustomizationDialog = false })

    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.FRENCH) }
    val totalTurnover = sales.sumOf { it.totalAmount }
    val totalProfit = sales.sumOf { it.profitAmount }
    val totalExpenses = cashTx.filter { !it.type.isCredit }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SleekBackground).testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Bonjour, ${currentUser.displayName.ifBlank { "Commerçant" }}", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary, letterSpacing = (-0.7).sp)
                    Spacer(Modifier.height(3.dp)); Text("${merchantProfile.shopName} • Aujourd’hui", fontSize = 13.sp, color = SleekTextSecondary)
                }
                Surface(shape = CircleShape, color = SleekBlueContainer, modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Insights, contentDescription = null, tint = SleekBlueOnContainer, modifier = Modifier.size(20.dp)) } }
            }
        }
        if (isSuspended) item { SiraNoticeCard(Icons.Default.Warning, "Compte temporairement suspendu", "Certaines opérations peuvent être restreintes. Contactez la Direction SIRA.", Color(0xFFFFF4F2), Color(0xFFFFE2DE), Color(0xFFB42318)) }
        if (directives.isNotEmpty()) item { val latest = directives.first(); SiraNoticeCard(Icons.Default.Campaign, latest.title, latest.message, SleekBlueContainer.copy(alpha = 0.45f), SleekBlueContainer, SleekBlueOnContainer) }

        item {
            Surface(shape = SiraScreenShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.72f)), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth().testTag("hero_turnover_card")) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column {
                            Text("CHIFFRE D’AFFAIRES", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp, color = SleekTextTertiary)
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.Bottom) { Text(numberFormat.format(totalTurnover.toLong()), fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.1).sp, color = SleekTextPrimary); Spacer(Modifier.width(7.dp)); Text("FCFA", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary, modifier = Modifier.padding(bottom = 5.dp)) }
                        }
                        Surface(shape = SiraPillShape, color = SleekSuccess.copy(alpha = 0.12f)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(7.dp).clip(CircleShape).background(SleekSuccess)); Spacer(Modifier.width(6.dp)); Text("Aujourd’hui", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SleekSuccess) } }
                    }
                    Spacer(Modifier.height(18.dp)); HorizontalDivider(color = SleekOutline.copy(alpha = 0.6f)); Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCell("Bénéfice", "+${numberFormat.format(totalProfit.toLong())} F", SleekSuccess, Modifier.weight(1f)); MetricCell("Dépenses", "${numberFormat.format(totalExpenses.toLong())} F", SleekError, Modifier.weight(1f)); MetricCell("Ventes", sales.size.toString(), SleekBluePrimary, Modifier.weight(1f)) }
                }
            }
        }

        item {
            SectionHeader("ACTIONS RAPIDES", null); Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Default.PointOfSale, "Vendre", SleekBluePrimary, onOpenNewSale, "action_new_sale", Modifier.weight(1f))
                QuickAction(Icons.Default.QrCodeScanner, "Scanner", SleekTertiary, onOpenNewSale, "action_scanner", Modifier.weight(1f))
                QuickAction(Icons.Default.Inventory2, "Stock", SleekSecondary, { onNavigateToTab(SiraNavTab.STOCK) }, "action_stock", Modifier.weight(1f))
                QuickAction(Icons.Default.Payments, "Caisse", SleekError, onOpenCashOp, "action_cash_op", Modifier.weight(1f))
            }
        }

        if (licenseConfig.keypadLayout != KeypadLayoutType.GRID_4) item {
            Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.7f)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text("Disposition rapide", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary); Text(licenseConfig.keypadLayout.label, fontSize = 11.sp, color = SleekTextSecondary) }
                    Text("Personnaliser", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SleekBluePrimary, modifier = Modifier.clickable { showCustomizationDialog = true })
                }
            }
        }

        if (lowStockProducts.isNotEmpty()) item {
            SiraNoticeCard(Icons.Default.Inventory, "${lowStockProducts.size} article(s) à surveiller", lowStockProducts.take(2).joinToString(" • ") { "${it.name} (${it.quantity})" }, Color(0xFFFFF8E7), Color(0xFFFFEDB3), Color(0xFF9A6700)) { onNavigateToTab(SiraNavTab.STOCK) }
        }

        item {
            Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.7f)), modifier = Modifier.fillMaxWidth().clickable { onNavigateToTab(SiraNavTab.INTELLIGENCE) }) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF2EDFF), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SleekTertiary, modifier = Modifier.size(22.dp)) } }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("SIRA Intelligence", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary); Text(if (lowStockProducts.isNotEmpty()) "Priorité : surveiller ${lowStockProducts.first().name}." else "Votre activité semble stable aujourd’hui.", fontSize = 11.sp, color = SleekTextSecondary, maxLines = 2) }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SleekTextTertiary)
                }
            }
        }

        item { SectionHeader("DERNIÈRES OPÉRATIONS", "Voir tout") { onNavigateToTab(SiraNavTab.VENTES) } }
        if (sales.isEmpty()) item {
            Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.7f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SleekTextTertiary, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(8.dp)); Text("Aucune vente enregistrée", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary); Text("Votre prochaine opération apparaîtra ici.", fontSize = 11.sp, color = SleekTextSecondary) }
            }
        } else items(sales.take(5), key = { it.id }) { sale ->
            Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(13.dp), color = SleekBlueContainer.copy(alpha = 0.65f), modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(19.dp)) } }
                    Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text("Vente #${sale.reference}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary); Text("${sale.customerName} • ${dateFormat.format(Date(sale.timestamp))}", fontSize = 11.sp, color = SleekTextTertiary) }
                    Column(horizontalAlignment = Alignment.End) { Text("+${numberFormat.format(sale.totalAmount.toLong())} F", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekSuccess); IconButton(onClick = { onViewSaleInvoice(sale) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.ReceiptLong, contentDescription = "Reçu", tint = SleekBluePrimary, modifier = Modifier.size(17.dp)) } }
                }
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun MetricCell(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) { Column(modifier = modifier) { Text(label, fontSize = 10.sp, color = SleekTextTertiary); Spacer(Modifier.height(3.dp)); Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1) } }

@Composable
private fun SectionHeader(title: String, action: String?, onAction: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = SleekTextTertiary); if (action != null) Text(action, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SleekBluePrimary, modifier = Modifier.clickable { onAction?.invoke() }) } }

@Composable
private fun QuickAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) { Column(modifier = modifier.testTag(tag).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) { Surface(shape = RoundedCornerShape(17.dp), color = tint.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth().height(58.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(23.dp)) } }; Spacer(Modifier.height(6.dp)); Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SleekTextSecondary) } }

@Composable
private fun SiraNoticeCard(icon: ImageVector, title: String, message: String, container: Color, iconContainer: Color, accent: Color, onClick: (() -> Unit)? = null) { Surface(shape = SiraCardShape, color = container, border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)), modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable { onClick() } else it }) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(13.dp), color = iconContainer, modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)) } }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent); Spacer(Modifier.height(2.dp)); Text(message, fontSize = 11.sp, color = accent.copy(alpha = 0.86f), maxLines = 2) }; if (onClick != null) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = accent.copy(alpha = 0.65f)) } } }
