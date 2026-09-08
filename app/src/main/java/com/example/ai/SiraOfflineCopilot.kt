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

    fun answer(
        query: String,
        products: List<Product> = emptyList(),
        sales: List<Sale> = emptyList(),
        saleItems: List<SaleItem> = emptyList(),
        cashTransactions: List<CashTransaction> = emptyList()
    ): String? {
        val q = query.lowercase(Locale.FRENCH).trim()
        if (q.isBlank()) return null

        if (containsAny(q, "stock", "rupture", "inventaire", "produit")) {
            val low = products.filter { it.isLowStock }.sortedBy { it.quantity }
            if (containsAny(q, "rupture", "manque", "bientôt", "bientot", "faible")) {
                if (low.isEmpty()) return "✅ Aucun produit n'est actuellement en alerte de stock faible dans les données locales."
                val details = low.take(8).joinToString("\n") { p ->
                    "• ${p.name} : ${p.quantity} unité(s)${if (p.quantity == 0) " — RUPTURE" else ""}"
                }
                return "📦 Produits à surveiller (${low.size}) :\n$details\n\nLes données viennent directement de votre stock local."
            }
            val value = products.sumOf { it.quantity * it.purchasePrice }
            return "📦 Votre base locale contient ${products.size} référence(s), pour une valeur d'achat de ${format.format(value.toLong())} FCFA."
        }

        if (containsAny(q, "chiffre", "ca", "vente", "ventes", "revenu", "recette")) {
            val revenue = sales.sumOf { it.totalAmount }
            val profit = sales.sumOf { it.profitAmount }
            return "📊 Données locales : ${sales.size} vente(s), chiffre d'affaires cumulé de ${format.format(revenue.toLong())} FCFA et bénéfice brut estimé de ${format.format(profit.toLong())} FCFA."
        }

        if (containsAny(q, "marge", "profit", "bénéfice", "benefice", "perte")) {
            val negative = products.filter { it.salePrice < it.purchasePrice }
            val profit = sales.sumOf { it.profitAmount }
            return if (negative.isEmpty()) {
                "💰 Bénéfice brut cumulé : ${format.format(profit.toLong())} FCFA. Aucun produit vendu sous son prix d'achat n'est détecté dans les données locales."
            } else {
                "⚠️ Bénéfice brut cumulé : ${format.format(profit.toLong())} FCFA. ${negative.size} produit(s) ont actuellement un prix de vente inférieur au prix d'achat."
            }
        }

        if (containsAny(q, "orange money", "orange", "commission")) {
            val (commission, count, volume) = calculateOrangeToday(cashTransactions)
            return "🟠 Orange Money aujourd'hui : ${format.format(commission.toLong())} FCFA de commissions, $count opération(s), volume ${format.format(volume.toLong())} FCFA."
        }

        if (containsAny(q, "client", "clients", "crédit", "credit", "dette")) {
            val credit = products // keep the function pure; customer totals are handled by the report path
            return "👥 Le copilote local peut analyser les clients et crédits dès que les données clients lui sont fournies par l'écran."
        }

        if (containsAny(q, "bonjour", "salut", "hello")) {
            return "Bonjour 👋 Je suis SIRA Copilote. Je peux déjà analyser hors ligne votre stock, vos ventes, vos marges et Orange Money à partir des données locales."
        }

        return "Je suis en mode hors ligne. Essayez : « produits en rupture », « mon chiffre d'affaires », « ma marge » ou « bénéfice Orange Money aujourd'hui »."
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
