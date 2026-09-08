package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Purchase
import com.example.data.entity.Sale
import com.example.ui.theme.*
import com.example.viewmodel.SiraViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SalesPurchasesScreen(
    viewModel: SiraViewModel,
    onNewSaleClick: () -> Unit,
    onViewSaleInvoice: (Sale) -> Unit,
    onNewProformaClick: () -> Unit = {}
) {
    var selectedSubTab by remember { mutableStateOf(0) }
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH) }

    val totalSalesAmount = sales.sumOf { it.totalAmount }
    val totalPurchasesAmount = purchases.sumOf { it.totalAmount }

    Scaffold(
        containerColor = SleekBackground,
        floatingActionButton = {
            if (selectedSubTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onNewSaleClick,
                    containerColor = SleekBluePrimary,
                    contentColor = Color.White,
                    shape = SiraPillShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 5.dp),
                    modifier = Modifier.testTag("fab_new_sale_from_list")
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nouvelle vente", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("sales_purchases_screen")) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Activité commerciale", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, color = SleekTextPrimary)
                        Spacer(Modifier.height(3.dp))
                        Text("Suivez vos ventes et préparez vos offres au même endroit.", fontSize = 13.sp, color = SleekTextSecondary)
                    }
                    if (selectedSubTab == 0) {
                        OutlinedButton(
                            onClick = onNewProformaClick,
                            shape = SiraPillShape,
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("btn_new_proforma")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Proforma", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Surface(shape = SiraPillShape, color = SleekSurfaceVariant, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth().height(46.dp)) {
                    Row(Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ActivitySegment("Ventes", sales.size, selectedSubTab == 0, SleekBluePrimary, { selectedSubTab = 0 }, Modifier.weight(1f))
                        ActivitySegment("Achats", purchases.size, selectedSubTab == 1, SleekSecondary, { selectedSubTab = 1 }, Modifier.weight(1f))
                    }
                }
            }

            Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.68f)), modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Row(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (selectedSubTab == 0) SleekBlueContainer else SleekSecondaryContainer, modifier = Modifier.size(34.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(if (selectedSubTab == 0) Icons.Default.TrendingUp else Icons.Default.ShoppingBag, contentDescription = null, tint = if (selectedSubTab == 0) SleekBlueOnContainer else SleekOnSecondaryContainer, modifier = Modifier.size(17.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (selectedSubTab == 0) "Recettes" else "Achats", fontSize = 11.sp, color = SleekTextTertiary)
                            Text(if (selectedSubTab == 0) "${numberFormat.format(totalSalesAmount.toLong())} FCFA" else "${numberFormat.format(totalPurchasesAmount.toLong())} FCFA", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (selectedSubTab == 0) SleekBluePrimary else SleekSecondary)
                        }
                    }
                    Text(if (selectedSubTab == 0) "${sales.size} opérations" else "${purchases.size} opérations", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SleekTextSecondary)
                }
            }

            Spacer(Modifier.height(10.dp))
            when {
                selectedSubTab == 0 && sales.isEmpty() -> EmptyActivityState("Aucune vente enregistrée", "Votre prochaine vente apparaîtra ici.")
                selectedSubTab == 1 && purchases.isEmpty() -> EmptyActivityState("Aucun achat fournisseur", "Les achats enregistrés apparaîtront ici.")
                selectedSubTab == 0 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp + 78.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { items(sales, key = { it.id }) { sale -> SaleItemCard(sale, numberFormat, dateFormat) { onViewSaleInvoice(sale) } } }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp + 24.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { items(purchases, key = { it.id }) { purchase -> PurchaseItemCard(purchase, numberFormat, dateFormat) } }
            }
        }
    }
}

@Composable
private fun ActivitySegment(label: String, count: Int, selected: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = SiraPillShape, color = if (selected) SleekSurface else Color.Transparent, shadowElevation = if (selected) 2.dp else 0.dp, modifier = modifier.fillMaxHeight().clickable { onClick() }) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) color else SleekTextSecondary)
            Spacer(Modifier.width(5.dp))
            Surface(shape = SiraPillShape, color = if (selected) color.copy(alpha = 0.11f) else SleekOutline.copy(alpha = 0.35f)) { Text(count.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (selected) color else SleekTextTertiary, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)) }
        }
    }
}

@Composable
private fun EmptyActivityState(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = SleekSurfaceVariant, modifier = Modifier.size(56.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SleekTextTertiary, modifier = Modifier.size(25.dp)) } }
                Spacer(Modifier.height(11.dp)); Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary); Spacer(Modifier.height(4.dp)); Text(subtitle, fontSize = 11.sp, color = SleekTextSecondary)
            }
        }
    }
}

@Composable
private fun SaleItemCard(sale: Sale, numberFormat: NumberFormat, dateFormat: SimpleDateFormat, onShowInvoice: () -> Unit) {
    Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.64f)), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(13.dp), color = SleekBlueContainer.copy(alpha = 0.75f), modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowUpward, null, tint = SleekBluePrimary, modifier = Modifier.size(19.dp)) } }
                    Spacer(Modifier.width(10.dp)); Column { Text("Facture ${sale.reference}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SleekTextPrimary); Text("Client : ${sale.customerName}", fontSize = 11.sp, color = SleekTextSecondary) }
                }
                Column(horizontalAlignment = Alignment.End) { Text("${numberFormat.format(sale.totalAmount.toLong())} F", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekSuccess); Text(sale.status.label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = SleekTextTertiary) }
            }
            Spacer(Modifier.height(11.dp)); HorizontalDivider(color = SleekOutline.copy(alpha = 0.55f)); Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${dateFormat.format(Date(sale.timestamp))} • ${sale.paymentMethod.label}", fontSize = 10.sp, color = SleekTextTertiary)
                Text("Voir le reçu", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SleekBluePrimary, modifier = Modifier.testTag("btn_view_invoice_${sale.id}").clickable { onShowInvoice() })
            }
        }
    }
}

@Composable
private fun PurchaseItemCard(purchase: Purchase, numberFormat: NumberFormat, dateFormat: SimpleDateFormat) {
    Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.64f)), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(13.dp), color = SleekSecondaryContainer.copy(alpha = 0.72f), modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingBag, null, tint = SleekSecondary, modifier = Modifier.size(19.dp)) } }
                    Spacer(Modifier.width(10.dp)); Column { Text("Achat ${purchase.reference}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SleekTextPrimary); Text("Fournisseur : ${purchase.supplierName}", fontSize = 11.sp, color = SleekTextSecondary) }
                }
                Text("${numberFormat.format(purchase.totalAmount.toLong())} F", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekSecondary)
            }
            Spacer(Modifier.height(10.dp)); Text("${dateFormat.format(Date(purchase.timestamp))} • Règlement : ${purchase.paymentMethod.label}", fontSize = 10.sp, color = SleekTextTertiary)
        }
    }
}
