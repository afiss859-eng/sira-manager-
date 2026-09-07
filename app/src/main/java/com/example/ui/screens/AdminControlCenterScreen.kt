package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.admin.AdminApiKeys
import com.example.admin.AdminDirective
import com.example.admin.AdminManager
import com.example.admin.MerchantAccountInfo
import com.example.auth.SiraRole
import com.example.ui.theme.*
import com.example.viewmodel.SiraViewModel
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminControlCenterScreen(
    viewModel: SiraViewModel,
    onOpenTerms: () -> Unit,
    onOpenAuth: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val adminManager = remember { AdminManager.getInstance(context) }
    val currentUser by viewModel.currentUser.collectAsState()
    val serverMetrics by adminManager.serverMetrics.collectAsState()
    val apiKeys by adminManager.apiKeys.collectAsState()
    val directives by adminManager.directives.collectAsState()
    val licenseKeys by adminManager.licenseKeys.collectAsState()
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

    var merchantAccounts by remember { mutableStateOf<List<MerchantAccountInfo>>(emptyList()) }
    var showEmbeddedWebPortal by remember { mutableStateOf(false) }
    var showEditKeysDialog by remember { mutableStateOf(false) }
    var showNewUserDialog by remember { mutableStateOf(false) }
    var showNewDirectiveDialog by remember { mutableStateOf(false) }
    var showCreateLicenseDialog by remember { mutableStateOf(false) }
    var selectedLicenseForEdit by remember { mutableStateOf<com.example.admin.AdminLicenseKey?>(null) }
    var selectedUserForRoleChange by remember { mutableStateOf<MerchantAccountInfo?>(null) }

    // Fetch local user accounts
    fun refreshAccounts() {
        scope.launch {
            merchantAccounts = adminManager.getLocalMerchantAccounts()
        }
    }

    LaunchedEffect(Unit) {
        refreshAccounts()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBackground)
            .testTag("admin_control_center_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Official Brand Hero Header (Cloud Portal, NO Localhost)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.sira_logo),
                                contentDescription = "Logo Officiel SIRA",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "SIRA DIRECTION GÉNÉRALE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Chemin d'aujourd'hui, Avenir de demain",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "Super Administrateur : ${currentUser.email}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        IconButton(onClick = onOpenAuth) {
                            Icon(
                                Icons.Default.SwitchAccount,
                                contentDescription = "Changer d'utilisateur",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Official Cloud Portal & Integrated Web Control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Portail Web Cloud SIRA (HTTPS)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = "Gouvernance commerçants • Direction Centrale",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { showEmbeddedWebPortal = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFF38BDF8)
                                )
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Portail Web", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adminManager.cloudDevUrl))
                                        context.startActivity(intent)
                                    } catch (ignored: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navigateur", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Metrics Overview: Managing the Others
        item {
            val approvedCount = merchantAccounts.count { it.isApproved }
            val activeCount = merchantAccounts.count { it.isActive }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSmallCard(
                    title = "Commerçants",
                    value = "${merchantAccounts.size}",
                    subtitle = "$activeCount comptes actifs",
                    icon = Icons.Default.People,
                    color = SleekBluePrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricSmallCard(
                    title = "Agréments SIRA",
                    value = "$approvedCount",
                    subtitle = "Boutiques certifiées",
                    icon = Icons.Default.Verified,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                MetricSmallCard(
                    title = "Directives",
                    value = "${directives.size}",
                    subtitle = "Consignes diffusées",
                    icon = Icons.Default.Campaign,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section: Directives Officielles Diffusées aux Commerçants
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SleekOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Directives Administratives Réseau",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SleekTextPrimary
                                )
                                Text(
                                    text = "Consignes transmises à tous les commerçants SIRA",
                                    fontSize = 11.sp,
                                    color = SleekTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { showNewDirectiveDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Diffuser", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (directives.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val latest = directives.first()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = latest.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF312E81)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFC7D2FE)
                                    ) {
                                        Text(
                                            text = latest.priority,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3730A3),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = latest.message,
                                    fontSize = 11.sp,
                                    color = Color(0xFF4338CA),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Gestion des Utilisateurs & Commerçants (Cœur du rôle Admin)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GESTION DES UTILISATEURS & COMMERÇANTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "Rôles, agréments officiels et état d'activation des comptes",
                        fontSize = 10.sp,
                        color = SleekTextSecondary
                    )
                }

                Button(
                    onClick = { showNewUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inscrire", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (merchantAccounts.isEmpty()) {
            item {
                Text("Chargement des comptes marchands...", fontSize = 12.sp, color = SleekTextSecondary)
            }
        } else {
            items(merchantAccounts) { account ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SleekOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // User Header (Avatar + Name + Statuses)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (account.role == SiraRole.SUPER_ADMIN) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (account.role == SiraRole.SUPER_ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = if (account.role == SiraRole.SUPER_ADMIN) Color(0xFFD97706) else SleekBluePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = account.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = SleekTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (account.role == SiraRole.SUPER_ADMIN) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                                        ) {
                                            Text(
                                                text = account.role.label,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (account.role == SiraRole.SUPER_ADMIN) Color(0xFFB45309) else Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${account.shopName} • ${account.city}",
                                        fontSize = 11.sp,
                                        color = SleekTextSecondary
                                    )
                                    Text(
                                        text = "${account.email} • ${account.phone}",
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Active / Suspended badge
                            Surface(
                                shape = CircleShape,
                                color = if (account.isActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = if (account.isActive) "ACTIF" else "SUSPENDU",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (account.isActive) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = SleekSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats: Products, Sales, Revenue, IFU
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Activité Commerciale",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextSecondary
                                )
                                Text(
                                    text = "${account.productCount} art. • ${account.saleCount} ventes",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SleekTextPrimary
                                )
                                Text(
                                    text = "CA: ${numberFormat.format(account.totalRevenueFcfa)} FCFA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D9488)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Agrément SIRA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextSecondary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (account.isApproved) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = if (account.isApproved) "✓ AGRÉÉ" else "⏳ EN ATTENTE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (account.isApproved) Color(0xFF16A34A) else Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "IFU: ${account.ifuNumber}",
                                    fontSize = 9.sp,
                                    color = SleekTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Admin Action Buttons: Promotion, Agréer, Suspendre
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Bouton explicite pour Nommer Admin / Rôle
                            Button(
                                onClick = { selectedUserForRoleChange = account },
                                modifier = Modifier.weight(1.3f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (account.role == SiraRole.SUPER_ADMIN) Color(0xFFD97706) else SleekBluePrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (account.role == SiraRole.SUPER_ADMIN) Icons.Default.Shield else Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (account.role == SiraRole.SUPER_ADMIN) "Rôle Admin ✓" else "Nommer Admin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Toggle Approval (Agrément officiel)
                            OutlinedButton(
                                onClick = {
                                    adminManager.toggleMerchantApproval(account.id, currentUser.email)
                                    refreshAccounts()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (account.isApproved) "Désagréer" else "Agréer",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Toggle Suspension
                            Button(
                                onClick = {
                                    adminManager.toggleUserSuspension(account.id, currentUser.email)
                                    refreshAccounts()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (account.isActive) Color(0xFFEF4444) else Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (account.isActive) "Suspendre" else "Activer",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Trousseau de Clés API (Réservé aux Administrateurs)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SleekOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Trousseau de Clés API (Admin)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SleekTextPrimary
                                )
                                Text(
                                    text = "Moteur IA SIRA, CinetPay, PayDunya, Orange Money",
                                    fontSize = 11.sp,
                                    color = SleekTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { showEditKeysDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Modifier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SleekSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    KeyItemRow(label = "Moteur Cloud SIRA IA (Intelligence Décisionnelle)", isConfigured = apiKeys.geminiApiKey.isNotBlank())
                    KeyItemRow(label = "CinetPay (Passerelle BF)", isConfigured = apiKeys.cinetPayApiKey.isNotBlank())
                    KeyItemRow(label = "PayDunya (Paiement Mobile)", isConfigured = apiKeys.payDunyaMasterKey.isNotBlank())
                    KeyItemRow(label = "Orange Money BF Marchand", isConfigured = apiKeys.orangeMoneyApiKey.isNotBlank())
                }
            }
        }

        // Section: Monétisation & Gestion des Clés API d'Activation (SIRA Monetization)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SleekOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Monétisation & Clés API Clients",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SleekTextPrimary
                                )
                                Text(
                                    text = "Quotas d'appareils, personnalisation & révocation immédiate",
                                    fontSize = 11.sp,
                                    color = SleekTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { showCreateLicenseDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Créer Clé", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SleekSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (licenseKeys.isEmpty()) {
                        Text(
                            text = "Aucune clé API active.",
                            fontSize = 11.sp,
                            color = SleekTextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        licenseKeys.forEach { lic ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (lic.status == "ACTIVE") Color(0xFFF8FAFC) else Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, if (lic.status == "ACTIVE") Color(0xFFE2E8F0) else Color(0xFFFCA5A5)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = lic.key,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp,
                                                    color = if (lic.status == "ACTIVE") Color(0xFF1E3A8A) else Color(0xFF991B1B),
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (lic.status == "ACTIVE") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                                ) {
                                                    Text(
                                                        text = lic.status,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (lic.status == "ACTIVE") Color(0xFF16A34A) else Color(0xFFDC2626),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${lic.merchantName} • ${lic.shopName}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SleekTextPrimary
                                            )
                                        }

                                        // Quota Badge
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                text = "${lic.usedCount} / ${lic.maxUsers} max",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1D4ED8),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "App: ${lic.appName} • Disposition: ${lic.keypadLayout} • Thème: ${lic.themeColor}",
                                        fontSize = 10.sp,
                                        color = SleekTextSecondary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                adminManager.toggleLicenseRevocation(lic.key, currentUser.email)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (lic.status == "ACTIVE") Color(0xFFDC2626) else Color(0xFF16A34A)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                if (lic.status == "ACTIVE") Icons.Default.Block else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (lic.status == "ACTIVE") "Révoquer Clé" else "Réactiver",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Legal & Copyright
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SleekSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenTerms() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = SleekBluePrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Conditions Générales & Confidentialité",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = SleekTextPrimary
                            )
                            Text(
                                text = "© Tous droits réservés à SIRA (Chemin d'aujourd'hui, Avenir de demain)",
                                fontSize = 10.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SleekTextSecondary)
                }
            }
        }
    }

    // Modal to Inscrire / Inviter un nouveau Commerçant
    if (showNewUserDialog) {
        NewMerchantDialog(
            adminEmail = currentUser.email,
            onDismiss = { showNewUserDialog = false },
            onSave = { name, shop, city, phone, email, role ->
                adminManager.registerNewMerchant(
                    email = email,
                    displayName = name,
                    shopName = shop,
                    city = city,
                    phone = phone,
                    role = role,
                    adminEmail = currentUser.email
                )
                refreshAccounts()
                showNewUserDialog = false
            }
        )
    }

    // Modal to Diffuser une Directive Administrative
    if (showNewDirectiveDialog) {
        NewDirectiveDialog(
            adminEmail = currentUser.email,
            onDismiss = { showNewDirectiveDialog = false },
            onPublish = { title, message, priority ->
                adminManager.broadcastDirective(title, message, currentUser.email, priority)
                showNewDirectiveDialog = false
            }
        )
    }

    // Modal to Change Role
    selectedUserForRoleChange?.let { user ->
        ChangeRoleDialog(
            user = user,
            onDismiss = { selectedUserForRoleChange = null },
            onSelectRole = { newRole ->
                adminManager.setUserRole(user.id, newRole, currentUser.email)
                refreshAccounts()
                selectedUserForRoleChange = null
            }
        )
    }

    // Modal to Edit API Keys
    if (showEditKeysDialog) {
        EditApiKeysDialog(
            initialKeys = apiKeys,
            adminEmail = currentUser.email,
            onDismiss = { showEditKeysDialog = false },
            onSave = { updated ->
                adminManager.saveApiKeys(updated, currentUser.email)
                showEditKeysDialog = false
            }
        )
    }

    // Modal: Embedded Web Portal
    if (showEmbeddedWebPortal) {
        EmbeddedWebPortalDialog(
            url = adminManager.localAssetUrl,
            onDismiss = { showEmbeddedWebPortal = false }
        )
    }

    // Modal: Création d'une nouvelle clé API de licence (Monétisation)
    if (showCreateLicenseDialog) {
        CreateLicenseDialog(
            onDismiss = { showCreateLicenseDialog = false },
            onCreate = { mName, sName, maxU, color, appN, layout ->
                adminManager.createLicenseKey(
                    merchantName = mName,
                    shopName = sName,
                    maxUsers = maxU,
                    themeColor = color,
                    appName = appN,
                    keypadLayout = layout,
                    adminEmail = currentUser.email
                )
                showCreateLicenseDialog = false
            }
        )
    }
}

@Composable
private fun CreateLicenseDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int, String, String, String) -> Unit
) {
    var merchantName by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var maxUsersText by remember { mutableStateOf("1") }
    var appName by remember { mutableStateOf("SIRA Business") }
    var selectedColor by remember { mutableStateOf("#005AC1") }
    var selectedLayout by remember { mutableStateOf("GRID_4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF2563EB))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Générer Clé API Monétisée", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("Nom du Commerçant Titulaire") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Nom de la Boutique / Enseigne") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxUsersText,
                    onValueChange = { maxUsersText = it.filter { c -> c.isDigit() } },
                    label = { Text("Quota Max d'Appareils / Utilisateurs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("Nom de l'App (Branding)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quota = maxUsersText.toIntOrNull() ?: 1
                    onCreate(merchantName.ifBlank { "Commerçant" }, shopName.ifBlank { "Boutique" }, quota, selectedColor, appName, selectedLayout)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Créer la Clé API", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun MetricSmallCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SleekOutline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SleekTextPrimary)
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = SleekTextSecondary)
            Text(text = subtitle, fontSize = 9.sp, color = color)
        }
    }
}

@Composable
private fun KeyItemRow(label: String, isConfigured: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = SleekTextPrimary)
        Surface(
            shape = CircleShape,
            color = if (isConfigured) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
        ) {
            Text(
                text = if (isConfigured) "CONFIGURÉ" else "MANQUANT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConfigured) Color(0xFF16A34A) else Color(0xFFDC2626),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun NewMerchantDialog(
    adminEmail: String,
    onDismiss: () -> Unit,
    onSave: (name: String, shop: String, city: String, phone: String, email: String, role: SiraRole) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var shop by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Ouagadougou") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(SiraRole.MERCHANT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Inscrire un Commerçant au Réseau", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet du commerçant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shop,
                    onValueChange = { shop = it },
                    label = { Text("Nom de la boutique") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Ville (ex: Ouagadougou, Bobo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone (+226...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email professionnel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onSave(name, shop, city, phone, email, role)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Text("Valider Inscription")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun NewDirectiveDialog(
    adminEmail: String,
    onDismiss: () -> Unit,
    onPublish: (title: String, message: String, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("REGLEMENTAIRE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Diffuser une Directive Réseau", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la directive") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Consignes officielles aux commerçants") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && message.isNotBlank()) {
                        onPublish(title, message, priority)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text("Diffuser")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun ChangeRoleDialog(
    user: MerchantAccountInfo,
    onDismiss: () -> Unit,
    onSelectRole: (SiraRole) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = SleekBluePrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Nommer & Modifier Rôle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choisissez le niveau d'habilitation pour ${user.displayName} (${user.email}) :",
                    fontSize = 12.sp,
                    color = SleekTextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                SiraRole.values().forEach { r ->
                    val isSelected = user.role == r
                    val isAdminRole = r == SiraRole.SUPER_ADMIN
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            if (isAdminRole) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                        } else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) {
                                if (isAdminRole) Color(0xFFD97706) else SleekBluePrimary
                            } else SleekOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRole(r) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAdminRole) Icons.Default.Shield else Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = if (isAdminRole) Color(0xFFD97706) else SleekBluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = r.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isAdminRole) Color(0xFFB45309) else SleekTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isAdminRole) {
                                        "Accès complet : audit des utilisateurs, agréments, gestion des clés API et monétisation."
                                    } else {
                                        "Accès standard : caisse enregistreuse, gestion des ventes, inventaire et opérations."
                                    },
                                    fontSize = 10.sp,
                                    color = SleekTextSecondary
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isAdminRole) Color(0xFFD97706) else SleekBluePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun EditApiKeysDialog(
    initialKeys: AdminApiKeys,
    adminEmail: String,
    onDismiss: () -> Unit,
    onSave: (AdminApiKeys) -> Unit
) {
    var gemini by remember { mutableStateOf(initialKeys.geminiApiKey) }
    var cinetpayKey by remember { mutableStateOf(initialKeys.cinetPayApiKey) }
    var cinetpaySite by remember { mutableStateOf(initialKeys.cinetPaySiteId) }
    var orangeCode by remember { mutableStateOf(initialKeys.orangeMoneyMerchantCode) }
    var orangeKey by remember { mutableStateOf(initialKeys.orangeMoneyApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Trousseau de Clés API (Admin)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Modifications réservées à : $adminEmail",
                    fontSize = 11.sp,
                    color = SleekTextSecondary
                )

                OutlinedTextField(
                    value = gemini,
                    onValueChange = { gemini = it },
                    label = { Text("Clé API Moteur SIRA IA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cinetpayKey,
                    onValueChange = { cinetpayKey = it },
                    label = { Text("Clé API CinetPay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cinetpaySite,
                    onValueChange = { cinetpaySite = it },
                    label = { Text("Site ID CinetPay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = orangeCode,
                    onValueChange = { orangeCode = it },
                    label = { Text("Code Marchand Orange Money BF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = orangeKey,
                    onValueChange = { orangeKey = it },
                    label = { Text("Clé API Orange Money BF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        AdminApiKeys(
                            geminiApiKey = gemini,
                            cinetPayApiKey = cinetpayKey,
                            cinetPaySiteId = cinetpaySite,
                            orangeMoneyMerchantCode = orangeCode,
                            orangeMoneyApiKey = orangeKey
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary)
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EmbeddedWebPortalDialog(
    url: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0B1120),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.sira_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Portail Web Direction SIRA",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Gouvernance réseau • Agréments & Directives",
                                fontSize = 10.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }

                // Embedded Web Interface
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            loadUrl(url)
                        }
                    }
                )
            }
        }
    }
}
