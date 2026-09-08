package com.example.ai

import com.example.data.entity.CashChannel
import com.example.data.repository.SiraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

/**
 * Offline SIRA Copilot.
 * Reads the merchant's current local SQLite database directly through SiraRepository.
 * No network, API key or cloud service is required.
 */
class SiraLocalCopilot(private val repository: SiraRepository) {
    suspend fun answer(query: String): Result<String> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase(Locale.FRENCH)
        if (q.isBlank()) return@withContext Result.failure(IllegalArgumentException("Demande vide."))

        val products = repository.allProducts.first()
        val lowStock = repository.lowStockProducts.first()
        val sales = repository.allSales.first()
        val saleItems = repository.allSaleItems.first()
        val customers = repository.allCustomers.first()
        val suppliers = repository.allSuppliers.first()
        val cash = repository.allCashTransactions.first()

        val nf = NumberFormat.getIntegerInstance(Locale.FRENCH)
        fun fcfa(value: Double): String = "${nf.format(value.toLong())} FCFA"

        val orangeToday = cash.filter {
            it.channel == CashChannel.ORANGE_MONEY && it.timestamp >= startOfTodayMillis()
        }
        val orangeCommission = orangeToday.sumOf { it.fee }
        val revenue = sales.sumOf { it.totalAmount }
        val profit = sales.sumOf { it.profitAmount }
        val stockValue = products.sumOf { it.quantity * it.purchasePrice }
        val credit = customers.sumOf { it.creditBalance }

        when {
            hasAny(q, "stock", "inventaire", "marchandise", "produits") && hasAny(q, "combien", "total", "nombre", "sont") -> {
                return@withContext Result.success(
                    "📦 Votre base locale SIRA contient ${products.size} produit(s), dont ${lowStock.size} en stock faible. Valeur d'achat estimée du stock : ${fcfa(stockValue)}."
                )
            }

            hasAny(q, "rupture", "stock faible", "manque", "bientôt vide", "bientot vide") -> {
                if (lowStock.isEmpty()) return@withContext Result.success("✅ Aucun produit n'est actuellement signalé en stock faible dans la base locale SIRA.")
                val top = lowStock.take(8).joinToString("\n") { "• ${it.name} : ${it.quantity} unité(s)" }
                return@withContext Result.success("🔴 Produits à surveiller localement :\n$top")
            }

            hasAny(q, "orange money", "orange") && hasAny(q, "bénéfice", "benefice", "commission", "gain") -> {
                return@withContext Result.success(
                    "🟠 Aujourd'hui, SIRA a calculé ${fcfa(orangeCommission)} de commissions Orange Money sur ${orangeToday.size} opération(s). Calcul effectué directement depuis votre base locale, sans Internet."
                )
            }

            hasAny(q, "chiffre d'affaires", "ca", "vente", "ventes", "revenu") && hasAny(q, "aujourd", "total", "combien", "montant") -> {
                return@withContext Result.success(
                    "💰 Chiffre d'affaires enregistré dans la base locale : ${fcfa(revenue)} pour ${sales.size} vente(s)."
                )
            }

            hasAny(q, "bénéfice", "benefice", "marge", "profit") -> {
                return@withContext Result.success(
                    "📈 Bénéfice brut enregistré par SIRA : ${fcfa(profit)}. Ce résultat vient directement des ventes locales enregistrées."
                )
            }

            hasAny(q, "crédit", "credit", "dette", "doivent") -> {
                return@withContext Result.success(
                    "👥 Encours clients actuel : ${fcfa(credit)} sur ${customers.count { it.creditBalance > 0 }} client(s) ayant un crédit."
                )
            }

            hasAny(q, "client", "clients") && hasAny(q, "combien", "nombre", "total") -> {
                return@withContext Result.success("👥 ${customers.size} client(s) sont enregistrés localement dans SIRA.")
            }

            hasAny(q, "fournisseur", "fournisseurs") && hasAny(q, "combien", "nombre", "total") -> {
                return@withContext Result.success("🚚 ${suppliers.size} fournisseur(s) sont enregistrés localement dans SIRA.")
            }

            hasAny(q, "produit") -> {
                val match = products.firstOrNull {
                    q.contains(it.name.lowercase(Locale.FRENCH)) ||
                        (it.barcode?.isNotBlank() == true && q.contains(it.barcode!!.lowercase(Locale.FRENCH)))
                }
                if (match != null) {
                    return@withContext Result.success(
                        "📦 ${match.name} : ${match.quantity} unité(s) en stock, prix de vente ${fcfa(match.salePrice)}, prix d'achat ${fcfa(match.purchasePrice)}."
                    )
                }
            }
        }

        Result.success(
            "🧠 Je fonctionne hors ligne à partir de votre base locale SIRA. Essayez : « combien de produits ai-je ? », « quels produits sont en rupture ? », « quel est mon bénéfice ? », « combien me doivent mes clients ? » ou « quel est mon bénéfice Orange Money ? »"
        )
    }

    private fun hasAny(value: String, vararg terms: String): Boolean = terms.any { value.contains(it) }

    private fun startOfTodayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
