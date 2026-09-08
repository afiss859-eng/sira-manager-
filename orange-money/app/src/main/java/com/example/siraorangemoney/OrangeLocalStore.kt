package com.sira.orangemoney

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OrangeLocalStore(context: Context) : SQLiteOpenHelper(context, "sira_orange_money.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, reference TEXT NOT NULL, amount INTEGER NOT NULL, fee INTEGER NOT NULL, timestamp INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun add(amount: Long, fee: Long) {
        val values = ContentValues().apply {
            put("reference", "OM-${System.currentTimeMillis()}")
            put("amount", amount)
            put("fee", fee)
            put("timestamp", System.currentTimeMillis())
        }
        writableDatabase.insertOrThrow("transactions", null, values)
    }

    fun all(): List<OrangeTransaction> {
        val out = mutableListOf<OrangeTransaction>()
        readableDatabase.query(
            "transactions",
            arrayOf("reference", "amount", "fee", "timestamp"),
            null,
            null,
            null,
            null,
            "timestamp DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out += OrangeTransaction(c.getString(0), c.getLong(1), c.getLong(2), c.getLong(3))
            }
        }
        return out
    }
}
