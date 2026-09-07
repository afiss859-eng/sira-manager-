package com.example

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
import androidx.compose.ui.graphics.Color
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
                        onActivated = {
                            // Activation débloque automatiquement l'application
                        }
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

                // Dialog states
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
                                .background(SleekSurface.copy(alpha = 0.95f))
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Header Top Row: Official SIRA Logo, Title, Role Pill & Account Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Official SIRA Brand Emblem
                                    Image(
                                        painter = painterResource(id = R.drawable.sira_logo),
                                        contentDescription = "Logo Officiel SIRA",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = licenseConfig.appName.ifBlank { "SIRA Business" },
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-0.3).sp,
                                                color = SleekTextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = if (currentUser.role == SiraRole.SUPER_ADMIN) Color(0xFFFEF3C7) else SleekBlueContainer
                                            ) {
                                                Text(
                                                    text = if (currentUser.role == SiraRole.SUPER_ADMIN) "ADMIN" else "MARCHAND",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentUser.role == SiraRole.SUPER_ADMIN) Color(0xFFD97706) else SleekBlueOnContainer,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${profile.shopName} • ${profile.city}",
                                            fontSize = 11.sp,
                                            color = SleekTextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Terms & Privacy Icon
                                    IconButton(
                                        onClick = { showTermsDialog = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Gavel,
                                            contentDescription = "CGU & Politique",
                                            tint = SleekTextSecondary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Cloud Sync button
                                    IconButton(
                                        onClick = { viewModel.performCloudSync() },
                                        modifier = Modifier.size(36.dp).testTag("top_bar_sync_button")
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = SleekBluePrimary
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.CloudSync,
                                                contentDescription = "Synchro Cloud",
                                                tint = SleekBluePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Merchant Google Avatar / Account Switcher
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (currentUser.role == SiraRole.SUPER_ADMIN) SleekBluePrimary else SleekSecondary)
                                            .clickable { showGoogleAuthDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser.displayName.take(2).uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Sleek Pill Search Bar
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SleekSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clickable { showGlobalSearch = true }
                                    .testTag("top_search_bar_trigger")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Rechercher",
                                        tint = SleekTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Rechercher produit, client, facture, caisse...",
                                        fontSize = 12.sp,
                                        color = SleekTextSecondary
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Surface(
                            shadowElevation = 10.dp,
                            border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.6f))
                        ) {
                            NavigationBar(
                                containerColor = SleekSurface,
                                tonalElevation = 0.dp,
                                modifier = Modifier.height(72.dp)
                            ) {
                            NavigationBarItem(
                                selected = currentTab == SiraNavTab.DASHBOARD,
                                onClick = { viewModel.selectTab(SiraNavTab.DASHBOARD) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                label = { Text("Tableau", fontSize = 9.sp, fontWeight = if (currentTab == SiraNavTab.DASHBOARD) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekBlueOnContainer,
                                    indicatorColor = SleekBlueContainer,
                                    unselectedIconColor = SleekTextSecondary.copy(alpha = 0.7f),
                                    selectedTextColor = SleekBlueOnContainer,
                                    unselectedTextColor = SleekTextSecondary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentTab == SiraNavTab.STOCK,
                                onClick = { viewModel.selectTab(SiraNavTab.STOCK) },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                label = { Text("Stock", fontSize = 9.sp, fontWeight = if (currentTab == SiraNavTab.STOCK) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekBlueOnContainer,
                                    indicatorColor = SleekBlueContainer,
                                    unselectedIconColor = SleekTextSecondary.copy(alpha = 0.7f),
                                    selectedTextColor = SleekBlueOnContainer,
                                    unselectedTextColor = SleekTextSecondary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_stock")
                            )

                            NavigationBarItem(
                                selected = currentTab == SiraNavTab.VENTES,
                                onClick = { viewModel.selectTab(SiraNavTab.VENTES) },
                                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                                label = { Text("Ventes", fontSize = 9.sp, fontWeight = if (currentTab == SiraNavTab.VENTES) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekBlueOnContainer,
                                    indicatorColor = SleekBlueContainer,
                                    unselectedIconColor = SleekTextSecondary.copy(alpha = 0.7f),
                                    selectedTextColor = SleekBlueOnContainer,
                                    unselectedTextColor = SleekTextSecondary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_ventes")
                            )

                            NavigationBarItem(
                                selected = currentTab == SiraNavTab.CAISSE,
                                onClick = { viewModel.selectTab(SiraNavTab.CAISSE) },
                                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                                label = { Text("Caisse", fontSize = 9.sp, fontWeight = if (currentTab == SiraNavTab.CAISSE) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekBlueOnContainer,
                                    indicatorColor = SleekBlueContainer,
                                    unselectedIconColor = SleekTextSecondary.copy(alpha = 0.7f),
                                    selectedTextColor = SleekBlueOnContainer,
                                    unselectedTextColor = SleekTextSecondary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_caisse")
                            )

                            NavigationBarItem(
                                selected = currentTab == SiraNavTab.PARTENAIRES,
                                onClick = { viewModel.selectTab(SiraNavTab.PARTENAIRES) },
                                icon = { Icon(Icons.Default.People, contentDescription = null) },
                                label = { Text("Clients", fontSize = 9.sp, fontWeight = if (currentTab == SiraNavTab.PARTENAIRES) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekBlueOnContainer,
                                    indicatorColor = SleekBlueContainer,
                                    unselectedIconColor = SleekTextSecondary.copy(alpha = 0.7f),
                                    selectedTextColor = SleekBlueOnContainer,
                                    unselectedTextColor = SleekTextSecondary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_partenaires")
                            )

                            NavigationBarItem(
                                selected = currentTab == SiraNavTab.INTELLIGENCE,
                                onClick = { viewModel.selectTab(SiraNavTab.INTELLIGENCE) },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                label = { Text("SIRA IA", fontSize = 9.sp, fontWeight = if (currentTab == SiraNavTab.INTELLIGENCE) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekBlueOnContainer,
                                    indicatorColor = SleekBlueContainer,
                                    unselectedIconColor = SleekTextSecondary.copy(alpha = 0.7f),
                                    selectedTextColor = SleekBlueOnContainer,
                                    unselectedTextColor = SleekTextSecondary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_intelligence")
                            )

                            NavigationBarItem(
                                selected = currentTab == SiraNavTab.PARAMETRES,
                                onClick = { viewModel.selectTab(SiraNavTab.PARAMETRES) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Plus", fontSize = 9.sp, fontWeight = if (currentTab == SiraNavTab.PARAMETRES) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekBlueOnContainer,
                                    indicatorColor = SleekBlueContainer,
                                    unselectedIconColor = SleekTextSecondary.copy(alpha = 0.7f),
                                    selectedTextColor = SleekBlueOnContainer,
                                    unselectedTextColor = SleekTextSecondary.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_parametres")
                            )
                        }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
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
                            SiraNavTab.ADMIN_CENTER -> AdminControlCenterScreen(
                                viewModel = viewModel,
                                onOpenTerms = { showTermsDialog = true },
                                onOpenAuth = { showGoogleAuthDialog = true }
                            )
                            SiraNavTab.PARAMETRES -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }

                // Global Search Modal
                if (showGlobalSearch) {
                    GlobalSearchDialog(
                        viewModel = viewModel,
                        onDismiss = { showGlobalSearch = false },
                        onNavigateToTab = { tab ->
                            viewModel.selectTab(tab)
                            showGlobalSearch = false
                        },
                        onSelectSale = { sale ->
                            viewModel.showInvoiceForSale(sale)
                            showGlobalSearch = false
                        }
                    )
                }

                // New Sale Dialog
                if (showNewSaleDialog) {
                    NewSaleDialog(
                        products = products,
                        customers = customers,
                        currentPaymentEngine = currentEngine,
                        onDismiss = { showNewSaleDialog = false },
                        onCompleteSale = { sale, items ->
                            viewModel.completeSale(sale, items)
                            showNewSaleDialog = false
                        }
                    )
                }

                // Add / Edit Product Dialog
                if (showAddEditProductDialog) {
                    AddEditProductDialog(
                        initialProduct = productToEdit,
                        onDismiss = { showAddEditProductDialog = false },
                        onSave = { product ->
                            viewModel.saveProduct(product)
                            showAddEditProductDialog = false
                        }
                    )
                }

                // KYC Customer Dialog
                if (showKycCustomerDialog) {
                    KycCustomerDialog(
                        initialCustomer = customerToEdit,
                        viewModel = viewModel,
                        onDismiss = { showKycCustomerDialog = false },
                        onSave = { customer ->
                            viewModel.saveCustomer(customer)
                            showKycCustomerDialog = false
                        }
                    )
                }

                // Cash Operation Dialog
                if (showCashOpDialog) {
                    CashOperationDialog(
                        onDismiss = { showCashOpDialog = false },
                        onSave = { tx ->
                            viewModel.recordCashOp(tx)
                            showCashOpDialog = false
                        }
                    )
                }

                // Printable / Shareable Invoice Modal
                lastInvoice?.let { (sale, items) ->
                    ReceiptDialog(
                        sale = sale,
                        items = items,
                        profile = profile,
                        onDismiss = { viewModel.closeInvoiceDialog() }
                    )
                }

                // Google Authentication Dialog
                if (showGoogleAuthDialog) {
                    GoogleAuthDialog(
                        authManager = viewModel.authManager,
                        onDismiss = { showGoogleAuthDialog = false },
                        onUserSwitched = { newUser ->
                            viewModel.switchUser(newUser)
                        }
                    )
                }

                // Terms of Service & Privacy Dialog
                if (showTermsDialog) {
                    TermsAndPrivacyDialog(onDismiss = { showTermsDialog = false })
                }
                }
            }
        }
    }
}
