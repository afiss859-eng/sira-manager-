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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.AdminManager
import com.example.auth.SiraRole
import com.example.data.entity.Sale
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
    val context = LocalContext.current
    val adminManager = remember { AdminManager.getInstance(context) }
    val directives by adminManager.directives.collectAsState()
    val isSuspended = adminManager.isUserSuspended(currentUser.id)
    val isApproved = adminManager.isUserApproved(currentUser.id)
    val licenseConfig by viewModel.licenseConfig.collectAsState()
    var showCustomizationDialog by remember { mutableStateOf(false) }

    if (showCustomizationDialog) {
        com.example.ui.components.AppCustomizationDialog(
            viewModel = viewModel,
            licenseConfig = licenseConfig,
            onDismiss = { showCustomizationDialog = false }
        )
    }

    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.FRENCH) }

    val totalTurnover = sales.sumOf { it.totalAmount }
    val totalProfit = sales.sumOf { it.profitAmount }
    val totalExpenses = cashTx.filter { !it.type.isCredit }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBackground)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Administrative Governance Notice (Directives or Suspension)
        if (isSuspended) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFEE2E2),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "COMPTE COMMERÇANT SUSPENDU",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "L'administrateur a restreint ce compte. Veuillez contacter la Direction SIRA.",
                                fontSize = 11.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            }
        }

        if (directives.isNotEmpty()) {
            item {
                val latest = directives.first()
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DIRECTION SIRA : ${latest.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                            Text(
                                text = latest.message,
                                fontSize = 10.sp,
                                color = Color(0xFF1E40AF),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Hero Turnout Card (Modern Deep Gradient with Glassmorphism & High-Contrast Craft)
        item {
            val customThemeColor = remember(licenseConfig.themeColorHex) {
                try {
                    Color(android.graphics.Color.parseColor(licenseConfig.themeColorHex))
                } catch (e: Exception) {
                    SleekBluePrimary
                }
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                customThemeColor,
                                customThemeColor.copy(alpha = 0.88f),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .testTag("hero_turnover_card")
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CHIFFRE D'AFFAIRES DU JOUR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = numberFormat.format(totalTurnover.toLong()),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 32.sp,
                                    letterSpacing = (-0.5).sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FCFA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4ADE80))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "En direct",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "Bénéfice estimé",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+${numberFormat.format(totalProfit.toLong())} F",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF4ADE80)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "Dépenses caisse",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${numberFormat.format(totalExpenses.toLong())} F",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dynamic Quick Actions (Disposition des touches selon licence: GRID_4, GRID_2, ou LIST_COMPACT)
        item {
            when (licenseConfig.keypadLayout) {
                com.example.license.KeypadLayoutType.GRID_2 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = SleekTertiaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable { onOpenNewSale() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = SleekOnTertiaryContainer, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("NOUVELLE VENTE", fontWeight = FontWeight.Black, fontSize = 13.sp, color = SleekOnTertiaryContainer)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = SleekSecondaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable { onNavigateToTab(SiraNavTab.STOCK) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = SleekOnSecondaryContainer, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("GESTION STOCK", fontWeight = FontWeight.Black, fontSize = 13.sp, color = SleekOnSecondaryContainer)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = SleekErrorContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable { onOpenCashOp() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = SleekError, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("DÉPENSE CAISSE", fontWeight = FontWeight.Black, fontSize = 13.sp, color = SleekError)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = SleekSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable { onNavigateToTab(SiraNavTab.PARTENAIRES) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = SleekTextPrimary, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("CLIENTS & KYC", fontWeight = FontWeight.Black, fontSize = 13.sp, color = SleekTextPrimary)
                                }
                            }
                        }
                    }
                }

                com.example.license.KeypadLayoutType.LIST_COMPACT -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SleekTertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenNewSale() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = SleekOnTertiaryContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Enregistrer une Vente Rapide", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SleekOnTertiaryContainer)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SleekOnTertiaryContainer)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SleekSecondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToTab(SiraNavTab.STOCK) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = SleekOnSecondaryContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Consulter l'Inventaire & Stock", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SleekOnSecondaryContainer)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SleekOnSecondaryContainer)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SleekErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCashOp() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = SleekError)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Sortie de Caisse / Dépense", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SleekError)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SleekError)
                            }
                        }
                    }
                }

                com.example.license.KeypadLayoutType.GRID_4 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SleekActionItem(
                            icon = Icons.Default.ShoppingCart,
                            label = "Vente",
                            containerColor = SleekTertiaryContainer,
                            contentColor = SleekOnTertiaryContainer,
                            onClick = onOpenNewSale,
                            tag = "action_new_sale"
                        )
                        SleekActionItem(
                            icon = Icons.Default.Inventory2,
                            label = "Stock",
                            containerColor = SleekSecondaryContainer,
                            contentColor = SleekOnSecondaryContainer,
                            onClick = { onNavigateToTab(SiraNavTab.STOCK) },
                            tag = "action_stock"
                        )
                        SleekActionItem(
                            icon = Icons.Default.Payments,
                            label = "Dépense",
                            containerColor = SleekErrorContainer,
                            contentColor = SleekError,
                            onClick = onOpenCashOp,
                            tag = "action_cash_op"
                        )
                        SleekActionItem(
                            icon = Icons.Default.Badge,
                            label = "KYC/ID",
                            containerColor = SleekSurfaceVariant,
                            contentColor = SleekTextPrimary,
                            onClick = { onNavigateToTab(SiraNavTab.PARTENAIRES) },
                            tag = "action_kyc"
                        )
                    }
                }
            }
        }

        // Customization Banner (Bouton pour personnaliser fond, photo, nom, disposition des touches)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SleekOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomizationDialog = true }
                    .testTag("open_customization_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SleekBluePrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Personnaliser l'Application",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SleekTextPrimary
                            )
                            Text(
                                text = "Fond, logo, nom boutique et touches (${licenseConfig.keypadLayout.label})",
                                fontSize = 10.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SleekTextSecondary)
                }
            }
        }

        // Low stock warning banner if any
        if (lowStockProducts.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTab(SiraNavTab.STOCK) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alerte : ${lowStockProducts.size} article(s) presque épuisés",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = lowStockProducts.take(2).joinToString { "${it.name} (${it.quantity})" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        // SIRA Intelligence Card (Sleek Interface Section)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SIRA INTELLIGENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "VOIR TOUT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekBluePrimary,
                        modifier = Modifier.clickable { onNavigateToTab(SiraNavTab.INTELLIGENCE) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SleekOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTab(SiraNavTab.INTELLIGENCE) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lowStockProducts.isNotEmpty()) "Stock Critique : ${lowStockProducts.first().name}"
                                       else "Diagnostic de gestion en temps réel",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekTextPrimary
                            )
                            Text(
                                text = if (lowStockProducts.isNotEmpty()) "Plus que ${lowStockProducts.first().quantity} unités. SIRA prévoit une rupture imminente."
                                       else "Trésorerie et flux de ventes équilibrés aujourd'hui.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = SleekTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Recent Operations Header & List (Sleek Interface Cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DERNIÈRES OPÉRATIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = SleekTextPrimary
                )
                Text(
                    text = "VOIR TOUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekBluePrimary,
                    modifier = Modifier.clickable { onNavigateToTab(SiraNavTab.VENTES) }
                )
            }
        }

        if (sales.isEmpty()) {
            item {
                Text(
                    text = "Aucune opération enregistrée pour le moment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            }
        } else {
            items(sales.take(4)) { sale ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SleekSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFC2E7FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CallReceived,
                                    contentDescription = null,
                                    tint = SleekBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Vente #${sale.reference}",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekTextPrimary
                                )
                                Text(
                                    text = "Client : ${sale.customerName} • ${dateFormat.format(Date(sale.timestamp))}",
                                    fontSize = 11.sp,
                                    color = SleekTextTertiary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "+${numberFormat.format(sale.totalAmount.toLong())} F",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekSuccess
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onViewSaleInvoice(sale) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = "Reçu",
                                    tint = SleekBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleekActionItem(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable { onClick() }.testTag(tag)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = SleekTextSecondary
        )
    }
}
