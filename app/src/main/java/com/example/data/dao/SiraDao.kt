package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE quantity <= minAlertStock ORDER BY quantity ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET quantity = MAX(0, quantity + :delta) WHERE id = :productId")
    suspend fun adjustQuantity(productId: Long, delta: Int)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProductsSync(): List<Product>
}

@Dao
interface ProductQrCodeDao {
    @Query("SELECT * FROM product_qr_codes ORDER BY createdAt DESC, id DESC")
    fun getAllQrCodes(): Flow<List<ProductQrCode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQrCode(code: ProductQrCode): Long

    @Delete
    suspend fun deleteQrCode(code: ProductQrCode)

    @Query("DELETE FROM product_qr_codes WHERE productId = :productId")
    suspend fun deleteQrCodesForProduct(productId: Long)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    suspend fun getAllSalesSync(): List<Sale>

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getCount(): Int

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE reference LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%'")
    fun searchSales(query: String): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItems(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsSync(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items ORDER BY id DESC")
    fun getAllSaleItems(): Flow<List<SaleItem>>
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getPurchaseById(id: Long): Purchase?

    @Query("SELECT * FROM purchases WHERE reference LIKE '%' || :query || '%' OR supplierName LIKE '%' || :query || '%'")
    fun searchPurchases(query: String): Flow<List<Purchase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItem>)

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    fun getPurchaseItems(purchaseId: Long): Flow<List<PurchaseItem>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getPurchaseItemsSync(purchaseId: Long): List<PurchaseItem>
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY fullName ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR idDocumentNumber LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR companyName LIKE '%' || :query || '%'")
    fun searchSuppliers(query: String): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)
}

@Dao
interface CashDao {
    @Query("SELECT * FROM cash_transactions ORDER BY timestamp DESC")
    fun getAllCashTransactions(): Flow<List<CashTransaction>>

    @Query("SELECT * FROM cash_transactions WHERE reference LIKE '%' || :query || '%' OR beneficiaryOrPayer LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchCashTransactions(query: String): Flow<List<CashTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashTransaction(tx: CashTransaction): Long
}

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC")
    fun getAllSyncLogs(): Flow<List<SyncLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLog): Long
}
