package com.example.ai

import com.example.data.entity.CashTransaction
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import java.text.NumberFormat
import java.util.Locale

/**
 * Copilote local, sans réseau.
 * Les réponses sont calculées exclusivement à partir des instantanés de la base locale Room
 * fournis par le SiraViewModel. Aucun secret/API n'est nécessaire pour ce mode.
 */
class SiraOfflineCopilot {
    private val format = NumberFormat.getIntegerInstance(Locale.FRENCH)

    fun answerFromContext(query: String, context: Map<String, Any?>): String? {
        val q = query.lowercase(Locale.FRENCH).trim()
        if (q.isBlank()) return null

        val productCount = (context["productCount"] as? Number)?.toInt() ?: 0
        val lowStockCount = (context["lowStockCount"] as? Number)?.toInt() ?: 0
        val salesCount = (context["salesCount"] as? Number)?.toInt() ?: 0
        val revenue = (context["salesRevenue"] as? Number)?.toDouble() ?: 0.0
        val profit = (context["salesProfit"] as? Number)?.toDouble() ?: 0.0
        val customers = (context["customerCount"] as? Number)?.toInt() ?: 0
        val suppliers = (context["supplierCount"] as? Number)?.toInt() ?: 0
        val orangeToday = (context["orangeMoneyToday"] as? Number)?.toDouble() ?: 0.0

        // Orange Money is checked before generic profit questions so that
        // "bénéfice Orange Money" is answered with the correct metric.
        if (containsAny(q, "orange money", "orange", "commission")) {
            return "🟠 Orange Money aujourd'hui : ${format.format(orangeToday.toLong())} FCFA de commissions selon les données locales."
        }
        if (containsAny(q, "rupture", "stock faible", "inventaire")) {
            return if (lowStockCount == 0) {
                "✅ Aucun article n'est actuellement en alerte de stock faible dans la base locale."
            } else {
                "⚠️ $lowStockCount article(s) sont actuellement en alerte de stock. Ouvrez Stock pour voir les références concernées."
            }
        }
        if (containsAny(q, "chiffre d'affaires", "chiffre", "ca", "vente", "ventes", "revenu", "recette")) {
            return "📊 Votre base locale compte $salesCount vente(s), pour ${format.format(revenue.toLong())} FCFA de chiffre d'affaires cumulé."
        }
        if (containsAny(q, "marge", "profit", "bénéfice", "benefice", "perte")) {
            return "💰 Bénéfice brut cumulé dans la base locale : ${format.format(profit.toLong())} FCFA."
        }
        if (containsAny(q, "catalogue", "produits", "références", "references")) {
            return "📦 Le catalogue local contient $productCount référence(s), avec $lowStockCount alerte(s) de stock."
        }
        if (containsAny(q, "client", "clients")) {
            return "👥 $customers client(s) sont enregistrés dans votre base locale SIRA."
        }
        if (containsAny(q, "fournisseur", "fournisseurs")) {
            return "🚚 $suppliers fournisseur(s) sont enregistrés dans votre base locale SIRA."
        }
        if (containsAny(q, "bonjour", "salut", "hello")) {
            return "Bonjour 👋 Je suis SIRA Copilote. Je fonctionne hors ligne pour les analyses disponibles dans vos données locales."
        }

        return null
    }

    fun answer(
        query: String,
        products: List<Product> = emptyList(),
        sales: List<Sale> = emptyList(),
        saleItems: List<SaleItem> = emptyList(),
        cashTransactions: List<CashTransaction> = emptyList()
    ): String? {
        return answerFromContext(query, mapOf(
            "productCount" to products.size,
            "lowStockCount" to products.count { it.isLowStock },
            "salesCount" to sales.size,
            "salesRevenue" to sales.sumOf { it.totalAmount },
            "salesProfit" to sales.sumOf { it.profitAmount },
            "cashTransactionCount" to cashTransactions.size,
            "orangeMoneyToday" to calculateOrangeToday(cashTransactions).first
        ))
    }

    private fun containsAny(value: String, vararg terms: String): Boolean = terms.any(value::contains)

    private fun calculateOrangeToday(transactions: List<CashTransaction>): Triple<Double, Int, Double> {
        val start = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val today = transactions.filter { it.channel.name.contains("ORANGE") && it.timestamp >= start }
        return Triple(today.sumOf { it.fee }, today.size, today.sumOf { it.amount })
    }
}
