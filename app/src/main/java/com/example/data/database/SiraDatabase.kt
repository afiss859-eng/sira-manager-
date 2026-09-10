package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope

@Database(entities = [Product::class, ProductQrCode::class, Sale::class, SaleItem::class, Purchase::class, PurchaseItem::class, Customer::class, Supplier::class, CashTransaction::class, SyncLog::class], version = 3, exportSchema = false)
abstract class SiraDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productQrCodeDao(): ProductQrCodeDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun cashDao(): CashDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile private var DEFAULT_INSTANCE: SiraDatabase? = null
        private val userInstances = java.util.concurrent.ConcurrentHashMap<String, SiraDatabase>()
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS product_qr_codes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, productId INTEGER NOT NULL, productName TEXT NOT NULL, price REAL NOT NULL, currency TEXT NOT NULL, payload TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_product_qr_codes_productId ON product_qr_codes(productId)")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product_qr_codes ADD COLUMN variant TEXT NOT NULL DEFAULT ''")
            }
        }

        fun purgeAllSimulatedData(context: Context) {
            try {
                val prefs = context.getSharedPreferences("sira_clean_slate", Context.MODE_PRIVATE)
                if (!prefs.getBoolean("purged_simulations_v3", false)) {
                    val dbDir = context.getDatabasePath("dummy").parentFile
                    if (dbDir != null && dbDir.exists()) {
                        for (f in dbDir.listFiles() ?: emptyArray()) if (f.name.startsWith("sira_user_")) try { context.deleteDatabase(f.name) } catch (_: Exception) {}
                    }
                    userInstances.clear(); DEFAULT_INSTANCE = null
                    prefs.edit().putBoolean("purged_simulations_v3", true).apply()
                }
            } catch (_: Exception) {}
        }
        fun resetAllDatabasesToBlank(context: Context) {
            val dbDir = context.getDatabasePath("dummy").parentFile
            if (dbDir != null && dbDir.exists()) for (f in dbDir.listFiles() ?: emptyArray()) if (f.name.startsWith("sira_user_")) try { context.deleteDatabase(f.name) } catch (_: Exception) {}
            userInstances.clear(); DEFAULT_INSTANCE = null
        }
        fun getDatabase(context: Context, scope: CoroutineScope): SiraDatabase = getDatabaseForUser(context, "default", scope)
        fun getDatabaseForUser(context: Context, userId: String, scope: CoroutineScope): SiraDatabase {
            purgeAllSimulatedData(context)
            val cleanId = if (userId.isBlank()) "default" else userId.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            return userInstances.computeIfAbsent(cleanId) {
                val dbName = "sira_user_${cleanId}.db"
                val instance = Room.databaseBuilder(context.applicationContext, SiraDatabase::class.java, dbName)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .build()
                if (cleanId == "default") DEFAULT_INSTANCE = instance
                instance
            }
        }
    }
}
