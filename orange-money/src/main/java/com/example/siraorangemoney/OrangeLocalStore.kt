package com.sira.orangemoney

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OrangeLocalStore(context: Context) : SQLiteOpenHelper(context, "sira_orange_money.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, reference TEXT NOT NULL UNIQUE, amount INTEGER NOT NULL, fee INTEGER NOT NULL, timestamp INTEGER NOT NULL, type TEXT NOT NULL DEFAULT 'ENCAISSEMENT', counterparty TEXT NOT NULL DEFAULT '', customerPhone TEXT NOT NULL DEFAULT '', recipientPhone TEXT NOT NULL DEFAULT '', idDocument TEXT NOT NULL DEFAULT '', balanceBefore INTEGER NOT NULL DEFAULT 0, balanceAfter INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL DEFAULT 'VALIDE', note TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE INDEX idx_transactions_timestamp ON transactions(timestamp DESC)")
        db.execSQL("CREATE INDEX idx_transactions_reference ON transactions(reference)")
        db.execSQL("CREATE INDEX idx_transactions_type ON transactions(type)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN type TEXT NOT NULL DEFAULT 'ENCAISSEMENT'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN counterparty TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN customerPhone TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN recipientPhone TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN idDocument TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN balanceBefore INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN balanceAfter INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'VALIDE'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp DESC)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_reference ON transactions(reference)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type)")
        }
    }

    fun add(
        amount: Long,
        fee: Long,
        type: String = "ENCAISSEMENT",
        counterparty: String = "",
        customerPhone: String = "",
        recipientPhone: String = "",
        idDocument: String = "",
        balanceBefore: Long = 0,
        balanceAfter: Long = 0,
        status: String = "VALIDE",
        note: String = ""
    ): String {
        val reference = "OM-${System.currentTimeMillis()}"
        val values = ContentValues().apply {
            put("reference", reference)
            put("amount", amount.coerceAtLeast(0))
            put("fee", fee.coerceAtLeast(0))
            put("timestamp", System.currentTimeMillis())
            put("type", type.take(32))
            put("counterparty", counterparty.take(120))
            put("customerPhone", customerPhone.take(24))
            put("recipientPhone", recipientPhone.take(24))
            put("idDocument", idDocument.take(64))
            put("balanceBefore", balanceBefore.coerceAtLeast(0))
            put("balanceAfter", balanceAfter.coerceAtLeast(0))
            put("status", status.take(24))
            put("note", note.take(180))
        }
        writableDatabase.insertOrThrow("transactions", null, values)
        return reference
    }

    fun all(): List<OrangeTransaction> {
        val out = mutableListOf<OrangeTransaction>()
        readableDatabase.query(
            "transactions",
            arrayOf("reference", "amount", "fee", "timestamp", "type", "counterparty", "customerPhone", "recipientPhone", "idDocument", "balanceBefore", "balanceAfter", "status", "note"),
            null, null, null, null, "timestamp DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out += OrangeTransaction(
                    reference = c.getString(0), amount = c.getLong(1), fee = c.getLong(2), timestamp = c.getLong(3),
                    type = c.getString(4), counterparty = c.getString(5), customerPhone = c.getString(6), recipientPhone = c.getString(7),
                    idDocument = c.getString(8), balanceBefore = c.getLong(9), balanceAfter = c.getLong(10), status = c.getString(11), note = c.getString(12)
                )
            }
        }
        return out
    }
}
