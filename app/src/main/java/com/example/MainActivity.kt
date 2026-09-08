package com.example

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.SiraRole
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.SiraNavTab
import com.example.viewmodel.SiraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private val viewModel: SiraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SiraTheme {
                val licenseConfig by viewModel.licenseConfig.collectAsState()

                if (!licenseConfig.isActivated || licenseConfig.isRevoked) {
                    AppActivationScreen(
                        viewModel = viewModel,
                        licenseConfig = licenseConfig,
                        onActivated = { }
                    )
                } else {
                    val currentTab by viewModel.currentTab.collectAsState()
                    val currentUser by viewModel.currentUser.collectAsState()
                    val lastInvoice by viewModel.lastGeneratedInvoiceSale.collectAsState()
                    val profile by viewModel.merchantProfile.collectAsState()
                    val products by viewModel.products.collectAsState()
                    val customers by viewModel.customers.collectAsState()
                    val currentEngine by viewModel.paymentManager.currentEngine.collectAsState()
                    val isSyncing by viewModel.isSyncing.collectAsState()
                    val accent = remember(licenseConfig.themeColorHex) { parseThemeColor(licenseConfig.themeColorHex) }

                    var showGlobalSearch by remember { mutableStateOf(false) }
                    var showNewSaleDialog by remember { mutableStateOf(false) }
                    var showAddEditProductDialog by remember { mutableStateOf(false) }
                    var productToEdit by remember { mutableStateOf<Product?>(null) }
                    var showKycCustomerDialog by remember { mutableStateOf(false) }
                    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
                    var showCashOpDialog by remember { mutableStateOf(false) }
                    var showGoogleAuthDialog by remember { mutableStateOf(false) }
                    var showTermsDialog by remember { mutableStateOf(false) }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = SleekBackground,
                        topBar = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SleekSurface.copy(alpha = 0.98f))
                                    .statusBarsPadding()
                                    .padding(horizontal = 18.dp, vertical = 9.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LicensedImage(
                                            url = licenseConfig.logoUrl,
                                            fallbackRes = R.drawable.sira_logo,
                                            contentDescription = "Logo de l'organisation",
                                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = licenseConfig.appName.ifBlank { "SIRA Business" },
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-0.35).sp,
                                                color = SleekTextPrimary
                                            )
                                            Text(
                                                text = "${profile.shopName} • ${profile.city} • ${licenseConfig.stockModel}",
                                                fontSize = 10.sp,
                                                color = SleekTextSecondary
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { showTermsDialog = true }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Gavel, contentDescription = "CGU & Politique", tint = SleekTextSecondary, modifier = Modifier.size(19.dp))
                                        }
                                        IconButton(onClick = { viewModel.performCloudSync() }, modifier = Modifier.size(36.dp).testTag("top_bar_sync_button")) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accent)
                                            } else {
                                                Icon(Icons.Default.CloudSync, contentDescription = "Synchro Cloud", tint = accent, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(3.dp))
                                        LicensedImage(
                                            url = licenseConfig.profilePhotoUrl,
                                            fallbackRes = R.drawable.sira_logo,
                                            contentDescription = "Profil de l'organisation",
                                            modifier = Modifier.size(36.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                            fallbackTint = if (currentUser.role == SiraRole.SUPER_ADMIN) accent else SleekSecondary
                                        )
                                    }
                                }

                                Spacer(Modifier.height(9.dp))

                                Surface(
                                    shape = SiraPillShape,
                                    color = SleekSurfaceVariant,
                                    border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clickable { showGlobalSearch = true }
                                        .testTag("top_search_bar_trigger")
                                ) {
                                    Row(Modifier.fillMaxSize().padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = SleekTextSecondary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Rechercher un produit, client ou document", fontSize = 12.sp, color = SleekTextSecondary)
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            SiraBottomDock(currentTab = currentTab, onTabSelected = viewModel::selectTab)
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                        ) {
                            when (currentTab) {
                                SiraNavTab.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    onOpenNewSale = { showNewSaleDialog = true },
                                    onOpenAddProduct = {
                                        productToEdit = null
                                        showAddEditProductDialog = true
                                    },
                                    onOpenCashOp = { showCashOpDialog = true },
                                    onOpenGlobalSearch = { showGlobalSearch = true },
                                    onNavigateToTab = { viewModel.selectTab(it) },
                                    onViewSaleInvoice = { viewModel.showInvoiceForSale(it) }
                                )
                                SiraNavTab.STOCK -> InventoryScreen(
                                    viewModel = viewModel,
                                    onAddProductClick = {
                                        productToEdit = null
                                        showAddEditProductDialog = true
                                    },
                                    onEditProductClick = {
                                        productToEdit = it
                                        showAddEditProductDialog = true
                                    }
                                )
                                SiraNavTab.VENTES -> SalesPurchasesScreen(
                                    viewModel = viewModel,
                                    onNewSaleClick = { showNewSaleDialog = true },
                                    onViewSaleInvoice = { viewModel.showInvoiceForSale(it) }
                                )
                                SiraNavTab.CAISSE -> CashflowScreen(
                                    viewModel = viewModel,
                                    onNewCashOpClick = { showCashOpDialog = true }
                                )
                                SiraNavTab.PARTENAIRES -> PartenairesScreen(
                                    viewModel = viewModel,
                                    onAddCustomerClick = {
                                        customerToEdit = null
                                        showKycCustomerDialog = true
                                    },
                                    onEditCustomerClick = {
                                        customerToEdit = it
                                        showKycCustomerDialog = true
                                    }
                                )
                                SiraNavTab.INTELLIGENCE -> SiraAiScreen(viewModel = viewModel)
                                SiraNavTab.ADMIN_CENTER -> AdminDashboardScreen(
                                    viewModel = viewModel,
                                    onOpenTerms = { showTermsDialog = true },
                                    onOpenAuth = { showGoogleAuthDialog = true }
                                )
                                SiraNavTab.PARAMETRES -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }

                    if (showGlobalSearch) {
                        GlobalSearchDialog(viewModel = viewModel, onDismiss = { showGlobalSearch = false }, onNavigateToTab = { tab -> viewModel.selectTab(tab); showGlobalSearch = false }, onSelectSale = { sale -> viewModel.showInvoiceForSale(sale); showGlobalSearch = false })
                    }
                    if (showNewSaleDialog) {
                        NewSaleDialog(products = products, customers = customers, currentPaymentEngine = currentEngine, onDismiss = { showNewSaleDialog = false }, onCompleteSale = { sale, items -> viewModel.completeSale(sale, items); showNewSaleDialog = false })
                    }
                    if (showAddEditProductDialog) {
                        AddEditProductDialog(initialProduct = productToEdit, onDismiss = { showAddEditProductDialog = false }, onSave = { product -> viewModel.saveProduct(product); showAddEditProductDialog = false })
                    }
                    if (showKycCustomerDialog) {
                        KycCustomerDialog(initialCustomer = customerToEdit, viewModel = viewModel, onDismiss = { showKycCustomerDialog = false }, onSave = { customer -> viewModel.saveCustomer(customer); showKycCustomerDialog = false })
                    }
                    if (showCashOpDialog) {
                        CashOperationDialog(onDismiss = { showCashOpDialog = false }, onSave = { tx -> viewModel.recordCashOp(tx); showCashOpDialog = false })
                    }
                    lastInvoice?.let { (sale, items) -> ReceiptDialog(sale = sale, items = items, profile = profile, onDismiss = { viewModel.closeInvoiceDialog() }) }
                    if (showGoogleAuthDialog) {
                        GoogleAuthDialog(authManager = viewModel.authManager, onDismiss = { showGoogleAuthDialog = false }, onUserSwitched = { newUser -> viewModel.switchUser(newUser) })
                    }
                    if (showTermsDialog) {
                        TermsAndPrivacyDialog(onDismiss = { showTermsDialog = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun LicensedImage(
    url: String,
    fallbackRes: Int,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackTint: Color = SleekBluePrimary
) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = url) {
        value = if (url.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.doInput = true
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
                    .also { conn.disconnect() }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier = modifier.background(fallbackTint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = fallbackRes), contentDescription = contentDescription, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = contentScale)
        }
    }
}

private fun parseThemeColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(SleekBluePrimary)

@Composable
private fun SiraBottomDock(currentTab: SiraNavTab, onTabSelected: (SiraNavTab) -> Unit) {
    val items = listOf(
        SiraDockItem(SiraNavTab.DASHBOARD, Icons.Default.Home, "Accueil", "nav_dashboard"),
        SiraDockItem(SiraNavTab.STOCK, Icons.Default.Inventory2, "Stock", "nav_stock"),
        SiraDockItem(SiraNavTab.VENTES, Icons.Default.ReceiptLong, "Ventes", "nav_ventes"),
        SiraDockItem(SiraNavTab.CAISSE, Icons.Default.AccountBalanceWallet, "Caisse", "nav_caisse"),
        SiraDockItem(SiraNavTab.PARTENAIRES, Icons.Default.People, "Clients", "nav_partenaires"),
        SiraDockItem(SiraNavTab.INTELLIGENCE, Icons.Default.AutoAwesome, "SIRA IA", "nav_intelligence"),
        SiraDockItem(SiraNavTab.PARAMETRES, Icons.Default.Settings, "Plus", "nav_parametres")
    )
    Surface(color = SleekSurface.copy(alpha = 0.98f), shadowElevation = 6.dp, tonalElevation = 0.dp, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.72f)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val selected = currentTab == item.tab
                Column(modifier = Modifier.weight(1f).testTag(item.tag).clickable { onTabSelected(item.tab) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = SiraPillShape, color = if (selected) SleekBlueContainer else Color.Transparent, modifier = Modifier.height(32.dp).fillMaxWidth()) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(item.icon, contentDescription = item.label, tint = if (selected) SleekBlueOnContainer else SleekTextTertiary, modifier = Modifier.size(if (selected) 20.dp else 19.dp))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, fontSize = 8.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) SleekBlueOnContainer else SleekTextTertiary, maxLines = 1)
                }
            }
        }
    }
}

private data class SiraDockItem(val tab: SiraNavTab, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val tag: String)
