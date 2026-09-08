package com.sira.orangemoney

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OrangeLocalStore(context: Context) : SQLiteOpenHelper(context, "sira_orange_money.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, reference TEXT NOT NULL, amount INTEGER NOT NULL, fee INTEGER NOT NULL, timestamp INTEGER NOT NULL, type TEXT NOT NULL DEFAULT 'ENCAISSEMENT', counterparty TEXT NOT NULL DEFAULT '')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN type TEXT NOT NULL DEFAULT 'ENCAISSEMENT'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN counterparty TEXT NOT NULL DEFAULT ''")
        }
    }

    fun add(amount: Long, fee: Long, type: String = "ENCAISSEMENT", counterparty: String = "") {
        val values = ContentValues().apply {
            put("reference", "OM-${System.currentTimeMillis()}")
            put("amount", amount)
            put("fee", fee)
            put("timestamp", System.currentTimeMillis())
            put("type", type)
            put("counterparty", counterparty)
        }
        writableDatabase.insertOrThrow("transactions", null, values)
    }

    fun all(): List<OrangeTransaction> {
        val out = mutableListOf<OrangeTransaction>()
        readableDatabase.query(
            "transactions",
            arrayOf("reference", "amount", "fee", "timestamp", "type", "counterparty"),
            null,
            null,
            null,
            null,
            "timestamp DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out += OrangeTransaction(
                    reference = c.getString(0),
                    amount = c.getLong(1),
                    fee = c.getLong(2),
                    timestamp = c.getLong(3),
                    type = c.getString(4),
                    counterparty = c.getString(5)
                )
            }
        }
        return out
    }
}
