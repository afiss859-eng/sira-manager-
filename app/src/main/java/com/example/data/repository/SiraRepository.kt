package com.example.data.repository

import com.example.data.database.SiraDatabase
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SiraRepository(private val db: SiraDatabase) {
    val allProducts: Flow<List<Product>> = db.productDao().getAllProducts()
    val lowStockProducts: Flow<List<Product>> = db.productDao().getLowStockProducts()
    val allSales: Flow<List<Sale>> = db.saleDao().getAllSales()
    val allSaleItems: Flow<List<SaleItem>> = db.saleDao().getAllSaleItems()
    val allPurchases: Flow<List<Purchase>> = db.purchaseDao().getAllPurchases()
    val allCustomers: Flow<List<Customer>> = db.customerDao().getAllCustomers()
    val allSuppliers: Flow<List<Supplier>> = db.supplierDao().getAllSuppliers()
    val allCashTransactions: Flow<List<CashTransaction>> = db.cashDao().getAllCashTransactions()
    val allSyncLogs: Flow<List<SyncLog>> = db.syncDao().getAllSyncLogs()

    suspend fun getProduct(id: Long) = db.productDao().getProductById(id)

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        db.productDao().insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        db.productDao().updateProduct(product)
    }

    suspend fun adjustProductStock(productId: Long, delta: Int) = withContext(Dispatchers.IO) {
        db.productDao().adjustQuantity(productId, delta)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        db.productDao().deleteProduct(product)
    }

    suspend fun recordSale(sale: Sale, items: List<SaleItem>): Long = withContext(Dispatchers.IO) {
        val saleId = db.saleDao().insertSale(sale)
        val itemsWithSaleId = items.map { it.copy(saleId = saleId) }
        db.saleDao().insertSaleItems(itemsWithSaleId)

        // Decrement stock for each item sold
        for (item in items) {
            db.productDao().adjustQuantity(item.productId, -item.quantity)
        }

        // Also record in cashbook if paid
        if (sale.status == SaleStatus.PAYE) {
            val channel = when (sale.paymentMethod) {
                PaymentMethod.ORANGE_MONEY -> CashChannel.ORANGE_MONEY
                PaymentMethod.MOOV_MONEY -> CashChannel.MOOV_MONEY
                PaymentMethod.CORIS_MONEY -> CashChannel.CORIS_MONEY
                PaymentMethod.ESPECES -> CashChannel.ESPECES
                PaymentMethod.A_CREDIT -> CashChannel.AUTRE
            }

            if (channel != CashChannel.AUTRE) {
                db.cashDao().insertCashTransaction(
                    CashTransaction(
                        type = CashOpType.VENTE_ENCAISSEMENT,
                        channel = channel,
                        amount = sale.totalAmount,
                        reference = "ENC-${sale.reference}",
                        beneficiaryOrPayer = sale.customerName,
                        description = "Vente ${sale.reference} encaissée"
                    )
                )
            } else if (sale.customerId != null) {
                // If credit sale, update customer credit balance
                val cust = db.customerDao().getCustomerById(sale.customerId)
                if (cust != null) {
                    db.customerDao().updateCustomer(cust.copy(creditBalance = cust.creditBalance + sale.totalAmount))
                }
            }
        }

        saleId
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItem> = withContext(Dispatchers.IO) {
        db.saleDao().getSaleItemsSync(saleId)
    }

    suspend fun recordPurchase(purchase: Purchase, items: List<PurchaseItem>): Long = withContext(Dispatchers.IO) {
        val purchaseId = db.purchaseDao().insertPurchase(purchase)
        val itemsWithPurchaseId = items.map { it.copy(purchaseId = purchaseId) }
        db.purchaseDao().insertPurchaseItems(itemsWithPurchaseId)

        // Increment stock for each item purchased
        for (item in items) {
            db.productDao().adjustQuantity(item.productId, item.quantity)
        }

        // Record decaissement in cashbook
        val channel = when (purchase.paymentMethod) {
            PaymentMethod.ORANGE_MONEY -> CashChannel.ORANGE_MONEY
            PaymentMethod.MOOV_MONEY -> CashChannel.MOOV_MONEY
            PaymentMethod.CORIS_MONEY -> CashChannel.CORIS_MONEY
            else -> CashChannel.ESPECES
        }

        db.cashDao().insertCashTransaction(
            CashTransaction(
                type = CashOpType.ACHAT_DECAISSEMENT,
                channel = channel,
                amount = purchase.totalAmount,
                reference = "DEC-${purchase.reference}",
                beneficiaryOrPayer = purchase.supplierName,
                description = "Achat marchandise ${purchase.reference}"
            )
        )

        purchaseId
    }

    suspend fun insertCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        db.customerDao().insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        db.customerDao().updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        db.customerDao().deleteCustomer(customer)
    }

    suspend fun insertSupplier(supplier: Supplier): Long = withContext(Dispatchers.IO) {
        db.supplierDao().insertSupplier(supplier)
    }

    suspend fun updateSupplier(supplier: Supplier) = withContext(Dispatchers.IO) {
        db.supplierDao().updateSupplier(supplier)
    }

    suspend fun deleteSupplier(supplier: Supplier) = withContext(Dispatchers.IO) {
        db.supplierDao().deleteSupplier(supplier)
    }

    suspend fun recordCashTransaction(tx: CashTransaction): Long = withContext(Dispatchers.IO) {
        db.cashDao().insertCashTransaction(tx)
    }

    suspend fun recordSyncLog(log: SyncLog): Long = withContext(Dispatchers.IO) {
        db.syncDao().insertSyncLog(log)
    }

    // Global Search across everything!
    suspend fun performGlobalSearch(query: String): GlobalSearchResult = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext GlobalSearchResult()

        val products = allProducts.first().filter {
            it.name.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            (it.barcode?.lowercase()?.contains(q) == true)
        }

        val customers = allCustomers.first().filter {
            it.fullName.lowercase().contains(q) ||
            it.phone.contains(q) ||
            (it.idDocumentNumber?.lowercase()?.contains(q) == true)
        }

        val sales = allSales.first().filter {
            it.reference.lowercase().contains(q) ||
            it.customerName.lowercase().contains(q)
        }

        val purchases = allPurchases.first().filter {
            it.reference.lowercase().contains(q) ||
            it.supplierName.lowercase().contains(q)
        }

        val cashTx = allCashTransactions.first().filter {
            it.reference.lowercase().contains(q) ||
            (it.beneficiaryOrPayer?.lowercase()?.contains(q) == true) ||
            it.description.lowercase().contains(q)
        }

        GlobalSearchResult(
            products = products,
            customers = customers,
            sales = sales,
            purchases = purchases,
            cashTransactions = cashTx
        )
    }
}

data class GlobalSearchResult(
    val products: List<Product> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val cashTransactions: List<CashTransaction> = emptyList()
) {
    val totalCount: Int
        get() = products.size + customers.size + sales.size + purchases.size + cashTransactions.size
}
