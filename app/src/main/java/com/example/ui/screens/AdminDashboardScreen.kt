package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.AdminManager
import com.example.admin.MerchantAccountInfo
import com.example.auth.SiraRole
import com.example.ui.theme.*
import com.example.viewmodel.SiraViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Cockpit d'administration SIRA.
 * Le centre de gestion détaillé historique est conservé et accessible depuis ce cockpit.
 */
@Composable
fun AdminDashboardScreen(
    viewModel: SiraViewModel,
    onOpenTerms: () -> Unit,
    onOpenAuth: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminManager = remember { AdminManager.getInstance(context) }
    val currentUser by viewModel.currentUser.collectAsState()
    val metrics by adminManager.serverMetrics.collectAsState()
    val auditLogs by adminManager.auditLogs.collectAsState()
    val directives by adminManager.directives.collectAsState()
    val licenses by adminManager.licenseKeys.collectAsState()
    val apiKeys by adminManager.apiKeys.collectAsState()
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

    var merchants by remember { mutableStateOf<List<MerchantAccountInfo>>(emptyList()) }
    var refreshing by remember { mutableStateOf(false) }
    var showDetailed by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            refreshing = true
            merchants = adminManager.getLocalMerchantAccounts()
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    if (currentUser.role != SiraRole.SUPER_ADMIN) {
        Surface(modifier = Modifier.fillMaxSize(), color = SleekBackground) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.GppBad, contentDescription = null, tint = SleekError, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(14.dp))
                Text("Accès administrateur refusé", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Cette zone est réservée au rôle Super Administrateur.", color = SleekTextSecondary)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onOpenAuth) { Text("Changer de compte") }
            }
        }
        return
    }

    if (showDetailed) {
        Box(modifier = Modifier.fillMaxSize()) {
            AdminControlCenterScreen(viewModel = viewModel, onOpenTerms = onOpenTerms, onOpenAuth = onOpenAuth)
            FilledTonalButton(
                onClick = { showDetailed = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cockpit")
            }
        }
        return
    }

    val activeMerchants = merchants.count { it.isActive }
    val pendingApprovals = merchants.count { !it.isApproved }
    val totalRevenue = merchants.sumOf { it.totalRevenueFcfa }
    val activeLicenses = licenses.count { it.status == "ACTIVE" }
    val configuredEngines = listOf(
        apiKeys.geminiApiKey,
        apiKeys.cinetPayApiKey,
        apiKeys.payDunyaMasterKey,
        apiKeys.orangeMoneyApiKey
    ).count { it.isNotBlank() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SleekBackground).testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(16.dp), color = SleekBluePrimary.copy(alpha = 0.18f)) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.padding(12.dp).size(25.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SIRA • CENTRE DE CONTRÔLE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("Pilotage réseau, sécurité et opérations", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(currentUser.email, color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = ::refresh) {
                            if (refreshing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF38BDF8))
                            else Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showDetailed = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Gestion détaillée")
                        }
                        FilledTonalButton(
                            onClick = {
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adminManager.cloudAdminUrl))) } catch (_: Exception) { }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Portail Web")
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AdminKpi("Marchands", merchants.size.toString(), "$activeMerchants actifs", Icons.Default.People, SleekBluePrimary, Modifier.weight(1f))
                AdminKpi("Approbations", pendingApprovals.toString(), "à traiter", Icons.Default.VerifiedUser, if (pendingApprovals > 0) Color(0xFFD97706) else SleekSuccess, Modifier.weight(1f))
                AdminKpi("CA réseau", "${numberFormat.format(totalRevenue)}", "FCFA cumulé", Icons.Default.Payments, Color(0xFF0D9488), Modifier.weight(1f))
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SleekOutline), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text("État de la plateforme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Mesures du runtime et moteurs connectés", fontSize = 10.sp, color = SleekTextSecondary)
                            }
                        }
                        Surface(shape = RoundedCornerShape(99.dp), color = SleekSuccessContainer) {
                            Text("ACTIF", color = SleekOnSuccessContainer, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    AdminInfoRow("Port", metrics.port.toString())
                    AdminInfoRow("Base(s) active(s)", metrics.activeDatabasesCount.toString())
                    AdminInfoRow("Uptime", formatUptime(metrics.uptimeSeconds))
                    AdminInfoRow("Mémoire runtime", "${metrics.jvmFreeMemoryMb} / ${metrics.jvmTotalMemoryMb} Mo libres / total")
                    AdminInfoRow("Moteurs configurés", "$configuredEngines / 4")
                    Text("État réseau : ${metrics.serverStatus}", fontSize = 10.sp, color = SleekTextSecondary, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SleekOutline), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("Sécurité & licences", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Éléments nécessitant une surveillance administrative", fontSize = 10.sp, color = SleekTextSecondary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SecurityLine("Licences actives", activeLicenses.toString(), activeLicenses > 0)
                    SecurityLine("Clés API configurées", configuredEngines.toString(), configuredEngines > 0)
                    SecurityLine("Approbations en attente", pendingApprovals.toString(), pendingApprovals == 0)
                    SecurityLine("Journal d'audit local", auditLogs.size.toString(), auditLogs.isNotEmpty())
                }
            }
        }

        if (merchants.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Surveillance des commerçants", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            items(merchants.take(8), key = { it.id }) { merchant ->
                MerchantMonitorCard(merchant, numberFormat)
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, SleekOutline), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = SleekSecondary, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("Communication réseau", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Dernière directive administrative", fontSize = 10.sp, color = SleekTextSecondary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val directive = directives.firstOrNull()
                    if (directive == null) {
                        Text("Aucune directive publiée.", fontSize = 11.sp, color = SleekTextSecondary)
                    } else {
                        Text(directive.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(directive.message, fontSize = 11.sp, color = SleekTextSecondary, maxLines = 3)
                        Spacer(Modifier.height(5.dp))
                        Text("Priorité : ${directive.priority} • ${directive.author}", fontSize = 9.sp, color = SleekTextTertiary)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { showDetailed = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ouvrir toutes les fonctions administratives")
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = SleekSurfaceVariant, modifier = Modifier.fillMaxWidth().clickable { onOpenTerms() }) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = SleekBluePrimary)
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CGU • Confidentialité • Gouvernance", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("Cadre officiel SIRA", fontSize = 10.sp, color = SleekTextSecondary)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SleekTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AdminKpi(title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SleekOutline), modifier = modifier) {
        Column(modifier = Modifier.padding(11.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(5.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 17.sp, color = SleekTextPrimary)
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
            Text(subtitle, fontSize = 9.sp, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun AdminInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 10.sp, color = SleekTextSecondary)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
    }
}

@Composable
private fun SecurityLine(label: String, value: String, healthy: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(99.dp), color = if (healthy) SleekSuccessContainer else Color(0xFFFFF4E5)) {
            Icon(if (healthy) Icons.Default.CheckCircle else Icons.Default.WarningAmber, contentDescription = null, tint = if (healthy) SleekSuccess else Color(0xFFD97706), modifier = Modifier.padding(5.dp).size(15.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun MerchantMonitorCard(merchant: MerchantAccountInfo, numberFormat: NumberFormat) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, SleekOutline), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = if (merchant.isActive) SleekBlueContainer else Color(0xFFFFE9E7)) {
                    Icon(if (merchant.isActive) Icons.Default.Storefront else Icons.Default.Block, contentDescription = null, tint = if (merchant.isActive) SleekBluePrimary else SleekError, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(merchant.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${merchant.shopName} • ${merchant.city}", fontSize = 10.sp, color = SleekTextSecondary)
                }
                Surface(shape = RoundedCornerShape(99.dp), color = if (merchant.isApproved) SleekSuccessContainer else Color(0xFFFFF4E5)) {
                    Text(if (merchant.isApproved) "AGRÉÉ" else "ATTENTE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (merchant.isApproved) SleekOnSuccessContainer else Color(0xFF9A6700), modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${merchant.productCount} produits", fontSize = 10.sp, color = SleekTextSecondary)
                Text("${merchant.saleCount} ventes", fontSize = 10.sp, color = SleekTextSecondary)
                Text("${numberFormat.format(merchant.totalRevenueFcfa)} FCFA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
            }
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 0 -> "${days}j ${hours}h ${minutes}min"
        hours > 0 -> "${hours}h ${minutes}min"
        else -> "${minutes}min"
    }
}
