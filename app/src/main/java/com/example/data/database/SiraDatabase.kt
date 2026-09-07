package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        Sale::class,
        SaleItem::class,
        Purchase::class,
        PurchaseItem::class,
        Customer::class,
        Supplier::class,
        CashTransaction::class,
        SyncLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SiraDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun cashDao(): CashDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var DEFAULT_INSTANCE: SiraDatabase? = null
        private val userInstances = java.util.concurrent.ConcurrentHashMap<String, SiraDatabase>()

        fun purgeAllSimulatedData(context: Context) {
            try {
                val prefs = context.getSharedPreferences("sira_clean_slate", Context.MODE_PRIVATE)
                if (!prefs.getBoolean("purged_simulations_v3", false)) {
                    val dbDir = context.getDatabasePath("dummy").parentFile
                    if (dbDir != null && dbDir.exists()) {
                        val dbFiles = dbDir.listFiles() ?: emptyArray()
                        for (f in dbFiles) {
                            if (f.name.startsWith("sira_user_")) {
                                try {
                                    context.deleteDatabase(f.name)
                                } catch (ignored: Exception) {}
                            }
                        }
                    }
                    userInstances.clear()
                    DEFAULT_INSTANCE = null
                    prefs.edit().putBoolean("purged_simulations_v3", true).apply()
                }
            } catch (ignored: Exception) {}
        }

        fun resetAllDatabasesToBlank(context: Context) {
            val dbDir = context.getDatabasePath("dummy").parentFile
            if (dbDir != null && dbDir.exists()) {
                val dbFiles = dbDir.listFiles() ?: emptyArray()
                for (f in dbFiles) {
                    if (f.name.startsWith("sira_user_")) {
                        try {
                            context.deleteDatabase(f.name)
                        } catch (ignored: Exception) {}
                    }
                }
            }
            userInstances.clear()
            DEFAULT_INSTANCE = null
        }

        fun getDatabase(context: Context, scope: CoroutineScope): SiraDatabase {
            return getDatabaseForUser(context, "default", scope)
        }

        fun getDatabaseForUser(context: Context, userId: String, scope: CoroutineScope): SiraDatabase {
            purgeAllSimulatedData(context)
            val cleanId = if (userId.isBlank()) "default" else userId.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            return userInstances.computeIfAbsent(cleanId) {
                val dbName = "sira_user_${cleanId}.db"
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiraDatabase::class.java,
                    dbName
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                if (cleanId == "default") {
                    DEFAULT_INSTANCE = instance
                }
                instance
            }
        }
    }
}
