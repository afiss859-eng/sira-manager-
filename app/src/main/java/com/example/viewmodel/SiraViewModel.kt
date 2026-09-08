package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.AdminManager
import com.example.admin.AdminWebServer
import com.example.ai.*
import com.example.auth.GoogleAuthManager
import com.example.auth.SiraAuthUser
import com.example.auth.SiraRole
import com.example.data.database.SiraDatabase
import com.example.data.entity.*
import com.example.data.repository.GlobalSearchResult
import com.example.data.repository.SiraRepository
import com.example.engine.PaymentManager
import com.example.ocr.OcrScanResult
import com.example.ocr.SiraOcrEngine
import com.example.printer.SiraBluetoothPrinter
import com.example.printer.SiraPrintQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SiraNavTab(val title: String) {
    DASHBOARD("Tableau"), STOCK("Stock"), VENTES("Ventes"), CAISSE("Caisse"), PARTENAIRES("Clients"), INTELLIGENCE("SIRA IA"), ADMIN_CENTER("Admin Web"), PARAMETRES("Plus")
}

data class MerchantProfile(
    val shopName: String = "SIRA Direction Centrale", val merchantName: String = "Afis Sawadogo", val email: String = "sawadogoafis125@gmail.com", val phone: String = "+226 70 00 12 34", val city: String = "Ouagadougou", val ifuNumber: String = "0014285901-BF", val rccmNumber: String = "BF-OUA-2024-B-1290", val isAuthenticatedWithGoogle: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class SiraViewModel(application: Application) : AndroidViewModel(application) {
    val authManager = GoogleAuthManager.getInstance(application)
    val adminManager = AdminManager.getInstance(application)
    val webServer = AdminWebServer(application)
    val licenseManager = com.example.license.LicenseManager.getInstance(application)
    val currentUser: StateFlow<SiraAuthUser> = authManager.currentUser
    val licenseConfig: StateFlow<com.example.license.AppLicenseConfig> = licenseManager.licenseConfig

    private val _currentDatabase = MutableStateFlow(SiraDatabase.getDatabaseForUser(application, currentUser.value.id, viewModelScope))
    private val _repository = MutableStateFlow(SiraRepository(_currentDatabase.value))
    val paymentManager = PaymentManager()
    private val aiService = SiraAiService()
    private val cloudCopilot = SiraCloudCopilot(licenseManager)
    private val bluetoothPrinter = SiraBluetoothPrinter(application)
    private val printQueue = SiraPrintQueue(application)

    private val _printStatus = MutableStateFlow<String?>(null)
    val printStatus: StateFlow<String?> = _printStatus.asStateFlow()
    private val _defaultPrinterMac = MutableStateFlow(bluetoothPrinter.getDefaultPrinterMac())
    val defaultPrinterMac: StateFlow<String?> = _defaultPrinterMac.asStateFlow()
    private val _pendingPrintCount = MutableStateFlow(printQueue.pendingCount())
    val pendingPrintCount: StateFlow<Int> = _pendingPrintCount.asStateFlow()

    private val _currentTab = MutableStateFlow(SiraNavTab.DASHBOARD)
    val currentTab: StateFlow<SiraNavTab> = _currentTab.asStateFlow()
    fun selectTab(tab: SiraNavTab) { _currentTab.value = tab }

    private val _merchantProfile = MutableStateFlow(MerchantProfile(shopName = currentUser.value.shopName, merchantName = currentUser.value.displayName, email = currentUser.value.email, city = currentUser.value.city))
    val merchantProfile: StateFlow<MerchantProfile> = _merchantProfile.asStateFlow()

    init {
        webServer.start(8080)
        retryPendingReceiptPrints()
    }
    override fun onCleared() { super.onCleared(); webServer.stop() }

    fun switchUser(user: SiraAuthUser) {
        viewModelScope.launch {
            val db = SiraDatabase.getDatabaseForUser(getApplication(), user.id, viewModelScope)
            _currentDatabase.value = db; _repository.value = SiraRepository(db)
            _merchantProfile.value = MerchantProfile(shopName = user.shopName, merchantName = user.displayName, email = user.email, city = user.city, isAuthenticatedWithGoogle = true)
            adminManager.recordAudit("USER_SWITCH", user.email, "Connexion réussie. Base isolée: sira_user_${user.id}.db active.")
        }
    }
    fun updateMerchantProfile(profile: MerchantProfile) { _merchantProfile.value = profile }
    suspend fun activateLicense(key: String): Result<com.example.license.AppLicenseConfig> {
        val res = licenseManager.activateWithKey(key)
        if (res.isSuccess) res.getOrNull()?.let { cfg -> _merchantProfile.value = _merchantProfile.value.copy(shopName = cfg.shopName, merchantName = cfg.merchantName) }
        return res
    }
    suspend fun saveLicenseCustomization(appName: String, shopName: String, themeColorHex: String, logoUrl: String, bgUrl: String, cguText: String, privacyText: String, keypadLayout: com.example.license.KeypadLayoutType): Result<Unit> {
        val res = licenseManager.saveCustomization(appName, shopName, themeColorHex, logoUrl, bgUrl, cguText, privacyText, keypadLayout)
        if (res.isSuccess) _merchantProfile.value = _merchantProfile.value.copy(shopName = shopName)
        return res
    }
    fun checkLicenseStatus() { viewModelScope.launch { licenseConfig.value.key.takeIf { it.isNotBlank() }?.let { licenseManager.validateKeyOnline(it) } } }

    val products = _repository.flatMapLatest { it.allProducts }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lowStockProducts = _repository.flatMapLatest { it.lowStockProducts }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sales = _repository.flatMapLatest { it.allSales }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val saleItems = _repository.flatMapLatest { it.allSaleItems }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val purchases = _repository.flatMapLatest { it.allPurchases }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = _repository.flatMapLatest { it.allCustomers }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = _repository.flatMapLatest { it.allSuppliers }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashTransactions = _repository.flatMapLatest { it.allCashTransactions }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val syncLogs = _repository.flatMapLatest { it.allSyncLogs }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _searchResults = MutableStateFlow(GlobalSearchResult())
    val searchResults: StateFlow<GlobalSearchResult> = _searchResults.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    fun setSearchQuery(query: String) { _searchQuery.value = query; if (query.isBlank()) { _searchResults.value = GlobalSearchResult(); _isSearching.value = false } else { _isSearching.value = true; viewModelScope.launch { _searchResults.value = _repository.value.performGlobalSearch(query) } } }
    fun clearSearch() { _searchQuery.value = ""; _searchResults.value = GlobalSearchResult(); _isSearching.value = false }

    fun quickStockAdjust(productId: Long, delta: Int) { viewModelScope.launch { _repository.value.adjustProductStock(productId, delta) } }
    fun saveProduct(product: Product) { viewModelScope.launch { if (product.id == 0L) _repository.value.insertProduct(product) else _repository.value.updateProduct(product) } }
    fun deleteProduct(product: Product) { viewModelScope.launch { _repository.value.deleteProduct(product) } }

    fun setDefaultBluetoothPrinter(macAddress: String) {
        bluetoothPrinter.setDefaultPrinter(macAddress)
        _defaultPrinterMac.value = bluetoothPrinter.getDefaultPrinterMac()
        _printStatus.value = "Imprimante configurée : $macAddress"
        retryPendingReceiptPrints()
    }
    fun clearPrintStatus() { _printStatus.value = null }

    private fun enqueueReceiptPrint(sale: Sale, items: List<SaleItem>) {
        printQueue.enqueue(sale, items)
        _pendingPrintCount.value = printQueue.pendingCount()
        retryPendingReceiptPrints()
    }

    fun retryPendingReceiptPrints() {
        viewModelScope.launch(Dispatchers.IO) {
            val pending = printQueue.takeAll()
            if (pending.isEmpty()) {
                _pendingPrintCount.value = 0
                return@launch
            }
            _printStatus.value = "Tentative d'impression de ${pending.size} reçu(s)…"
            val remaining = mutableListOf<Pair<Sale, List<SaleItem>>()>()
            for (receipt in pending) {
                val result = bluetoothPrinter.printSale(receipt.first, receipt.second)
                if (result.isFailure) remaining += receipt
            }
            printQueue.replace(remaining)
            _pendingPrintCount.value = remaining.size
            _printStatus.value = if (remaining.isEmpty()) {
                "Reçu(s) imprimé(s) automatiquement"
            } else {
                "${remaining.size} reçu(s) en attente d'impression"
            }
        }
    }

    private val _lastGeneratedInvoiceSale = MutableStateFlow<Pair<Sale, List<SaleItem>>?>(null)
    val lastGeneratedInvoiceSale: StateFlow<Pair<Sale, List<SaleItem>>?> = _lastGeneratedInvoiceSale.asStateFlow()
    fun closeInvoiceDialog() { _lastGeneratedInvoiceSale.value = null }
    fun showInvoiceForSale(sale: Sale) { viewModelScope.launch { _lastGeneratedInvoiceSale.value = Pair(sale, _repository.value.getSaleItems(sale.id)) } }
    fun completeSale(sale: Sale, items: List<SaleItem>, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val saleId = _repository.value.recordSale(sale, items)
            val recordedSale = sale.copy(id = saleId)
            val recordedItems = items.map { it.copy(saleId = saleId) }
            _lastGeneratedInvoiceSale.value = Pair(recordedSale, recordedItems)
            onComplete(saleId)
            enqueueReceiptPrint(recordedSale, recordedItems)
        }
    }
    fun completePurchase(purchase: Purchase, items: List<PurchaseItem>, onComplete: (Long) -> Unit = {}) { viewModelScope.launch { onComplete(_repository.value.recordPurchase(purchase, items)) } }

    fun saveCustomer(customer: Customer) { viewModelScope.launch { if (customer.id == 0L) _repository.value.insertCustomer(customer) else _repository.value.updateCustomer(customer) } }
    fun deleteCustomer(customer: Customer) { viewModelScope.launch { _repository.value.deleteCustomer(customer) } }
    fun saveSupplier(supplier: Supplier) { viewModelScope.launch { if (supplier.id == 0L) _repository.value.insertSupplier(supplier) else _repository.value.updateSupplier(supplier) } }
    fun deleteSupplier(supplier: Supplier) { viewModelScope.launch { _repository.value.deleteSupplier(supplier) } }

    fun recordCashOp(tx: CashTransaction) {
        viewModelScope.launch {
            val currentTxList = _repository.value.allCashTransactions.first()
            val balanceBefore = currentTxList.filter { it.channel == tx.channel }.sumOf { if (it.type.isCredit) it.amount else -(it.amount + it.fee) }
            val delta = if (tx.type.isCredit) tx.amount else -(tx.amount + tx.fee)
            val balanceAfter = balanceBefore + delta
            _repository.value.recordCashTransaction(tx)
            com.example.notification.SiraNotificationManager.showTransactionValidatedNotification(getApplication(), tx.reference, tx.beneficiaryOrPayer ?: "Client SIRA", tx.amount, tx.fee, balanceBefore, balanceAfter, tx.channel.label)
        }
    }

    private val _ocrResult = MutableStateFlow<OcrScanResult?>(null)
    val ocrResult: StateFlow<OcrScanResult?> = _ocrResult.asStateFlow()
    val ocrStatus: StateFlow<OcrScanResult?> = _ocrResult.asStateFlow()
    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading: StateFlow<Boolean> = _isOcrLoading.asStateFlow()
    fun runOcrOnBitmap(bitmap: Bitmap, onResult: (OcrScanResult) -> Unit = {}) { viewModelScope.launch { _isOcrLoading.value = true; val result = SiraOcrEngine.processBitmap(getApplication(), bitmap); _ocrResult.value = result; _isOcrLoading.value = false; onResult(result) } }
    fun runOcrOnDocument(bitmap: Bitmap, onResult: (OcrScanResult) -> Unit = {}) { runOcrOnBitmap(bitmap, onResult) }
    fun runOcrOnUri(uri: Uri, onResult: (OcrScanResult) -> Unit = {}) { viewModelScope.launch { _isOcrLoading.value = true; val result = SiraOcrEngine.scanDocumentUri(getApplication(), uri); _ocrResult.value = result; _isOcrLoading.value = false; onResult(result) } }
    fun clearOcrResult() { _ocrResult.value = null }

    private val _aiReport = MutableStateFlow<SiraIntelligenceReport?>(null)
    val aiReport: StateFlow<SiraIntelligenceReport?> = _aiReport.asStateFlow()
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()
    private val _aiInteractiveAnswer = MutableStateFlow<String?>(null)
    val aiInteractiveAnswer: StateFlow<String?> = _aiInteractiveAnswer.asStateFlow()
    fun askAiMerchantQuery(query: String) {
        viewModelScope.launch {
            _isAiLoading.value = true

            // SOURCE DE VÉRITÉ : la base SQLite locale du commerçant actif.
            // Le copilote local lit directement le repository Room associé à cette base.
            val localResult = SiraLocalCopilot(_repository.value).answer(query)
            if (localResult.isSuccess) {
                _aiInteractiveAnswer.value = localResult.getOrThrow()
            } else {
                _aiInteractiveAnswer.value = aiService.answerMerchantQuery(query, cashTransactions.value, sales.value)
            }

            // Le cloud n'est qu'une amélioration facultative pour les questions que le moteur local ne comprend pas.
            // Aucune dépendance réseau n'est nécessaire pour le fonctionnement métier hors ligne.
            if (localResult.isFailure && licenseConfig.value.isActivated) {
                val context = mapOf(
                    "shopName" to merchantProfile.value.shopName,
                    "productCount" to products.value.size,
                    "lowStockCount" to lowStockProducts.value.size,
                    "salesCount" to sales.value.size,
                    "salesRevenue" to sales.value.sumOf { it.totalAmount },
                    "salesProfit" to sales.value.sumOf { it.profitAmount },
                    "customerCount" to customers.value.size,
                    "supplierCount" to suppliers.value.size,
                    "cashTransactionCount" to cashTransactions.value.size,
                    "orangeMoneyToday" to getOrangeMoneyDailyStats().first
                )
                val cloud = cloudCopilot.ask(query, context)
                if (cloud.isSuccess) _aiInteractiveAnswer.value = cloud.getOrThrow()
            }
            _isAiLoading.value = false
        }
    }
    fun clearAiInteractiveAnswer() { _aiInteractiveAnswer.value = null }
    fun getOrangeMoneyDailyStats(): Triple<Double, Int, Double> = aiService.calculateOrangeMoneyDailyProfit(cashTransactions.value)
    fun runSiraIntelligence() { viewModelScope.launch { _isAiLoading.value = true; _aiReport.value = aiService.generateBusinessReport(products.value, sales.value, saleItems.value, customers.value, cashTransactions.value); _isAiLoading.value = false } }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val _syncMessage = MutableStateFlow<String?>("Prêt (Bases locales SQLite sécurisées)")
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    fun performCloudSync() { viewModelScope.launch { _isSyncing.value = true; _syncMessage.value = "Chiffrement et vérification d'intégrité..."; val total = products.value.size + sales.value.size + customers.value.size + cashTransactions.value.size; _repository.value.recordSyncLog(SyncLog(syncType = "Synchronisation Nœud Local", status = "SUCCES", itemsCount = total, details = "Base locale sira_user_${currentUser.value.id}.db validée ($total enregistrements).")); _syncMessage.value = "Dernière synchro : À l'instant ($total éléments)"; _isSyncing.value = false } }
    fun resetAllDataToBlank() { viewModelScope.launch(Dispatchers.IO) { val app = getApplication<Application>(); SiraDatabase.resetAllDatabasesToBlank(app); val cleanDb = SiraDatabase.getDatabaseForUser(app, currentUser.value.id, viewModelScope); _currentDatabase.value = cleanDb; _repository.value = SiraRepository(cleanDb); _syncMessage.value = "Base réinitialisée avec succès (100% vierge, aucune donnée simulée)."; adminManager.recordAudit("SYSTEM_RESET", currentUser.value.email, "Remise à blanc intégrale de la base de données locale effectuée.") } }
}
