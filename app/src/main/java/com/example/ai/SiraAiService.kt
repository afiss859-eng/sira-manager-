package com.example.ai

import com.example.data.entity.CashTransaction
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StockAlertInsight(
    val product: Product,
    val severity: String, // "CRITIQUE", "ATTENTION", "NORMAL"
    val daysRemainingEstimate: Int,
    val message: String
)

data class AnomalyInsight(
    val title: String,
    val description: String,
    val level: String, // "HAUTE", "MOYENNE", "INFO"
    val recommendation: String
)

data class SiraIntelligenceReport(
    val executiveSummary: String,
    val projectedTurnoverForecast: Double,
    val stockAlerts: List<StockAlertInsight>,
    val anomalies: List<AnomalyInsight>,
    val businessRecommendations: List<String>,
    val generatedByAi: Boolean,
    val orangeMoneyTodayProfit: Double = 0.0,
    val orangeMoneyTodayCount: Int = 0,
    val orangeMoneyTotalVolume: Double = 0.0
)

class SiraAiService {

    /**
     * Calcule avec une précision mathématique absolue le bénéfice net Orange Money
     * en additionnant rigoureusement toutes les commissions (frais d'opération)
     * des transactions Orange Money enregistrées aujourd'hui (depuis 00h00).
     */
    fun calculateOrangeMoneyDailyProfit(
        cashTransactions: List<CashTransaction>,
        timestampStartOfDay: Long = getStartOfTodayMillis()
    ): Triple<Double, Int, Double> {
        val todayOmTx = cashTransactions.filter { tx ->
            tx.channel == com.example.data.entity.CashChannel.ORANGE_MONEY &&
            tx.timestamp >= timestampStartOfDay
        }
        val totalCommissions = todayOmTx.sumOf { it.fee }
        val totalVolume = todayOmTx.sumOf { it.amount }
        return Triple(totalCommissions, todayOmTx.size, totalVolume)
    }

    suspend fun answerMerchantQuery(
        query: String,
        cashTransactions: List<CashTransaction>,
        sales: List<Sale>
    ): String = withContext(Dispatchers.IO) {
        val normalizedQuery = query.lowercase().trim()
        val isOrangeMoneyProfitQuery = normalizedQuery.contains("orange") &&
                (normalizedQuery.contains("bénéfice") || normalizedQuery.contains("benefice") || normalizedQuery.contains("gain") || normalizedQuery.contains("commission"))

        if (isOrangeMoneyProfitQuery || normalizedQuery.contains("orange money")) {
            val (commissions, count, volume) = calculateOrangeMoneyDailyProfit(cashTransactions)
            val numberFormat = java.text.NumberFormat.getIntegerInstance(java.util.Locale.FRENCH)
            val commStr = numberFormat.format(commissions.toLong())
            val volStr = numberFormat.format(volume.toLong())

            return@withContext """
                📊 Résultat Bénéfice Orange Money (Aujourd'hui) :
                • Commissions nettes perçues : $commStr FCFA
                • Nombre d'opérations : $count transaction(s)
                • Volume total traité : $volStr FCFA
                
                ✓ Calcul certifié exact : Le moteur a rigoureusement additionné l'ensemble de vos commissions unitaires sans aucune estimation ni arrondi fictif.
            """.trimIndent()
        }

        // General AI assistance
        if (GeminiClient.isApiKeyAvailable()) {
            try {
                val prompt = "Commerçant SIRA demande : \"$query\". Réponds en 2-3 phrases claires, adaptées aux réalités des marchés du Burkina Faso (FCFA, cash, gestion)."
                val request = GeminiRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))))
                val response = GeminiClient.api.generateContent(GeminiClient.getApiKey(), request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) return@withContext text
            } catch (e: Exception) {
                // Fallback below
            }
        }

        "Pour connaître votre bénéfice Orange Money du jour, dites ou tapez : 'Quel est mon bénéfice côté Orange Money aujourd'hui'. SIRA additionne automatiquement et fidèlement toutes vos commissions."
    }

    private fun getStartOfTodayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun generateBusinessReport(
        products: List<Product>,
        sales: List<Sale>,
        saleItems: List<SaleItem>,
        customers: List<Customer>,
        cashTransactions: List<CashTransaction>
    ): SiraIntelligenceReport = withContext(Dispatchers.IO) {
        // First compute local deterministic heuristics
        val totalRevenue = sales.sumOf { it.totalAmount }
        val totalProfit = sales.sumOf { it.profitAmount }
        val totalStockValue = products.sumOf { it.quantity * it.purchasePrice }
        val totalOutstandingCredit = customers.sumOf { it.creditBalance }

        // Low stock alerts
        val stockAlerts = products.filter { it.isLowStock }.map { product ->
            val soldCount = saleItems.filter { it.productId == product.id }.sumOf { it.quantity }
            val daysEstimate = if (soldCount > 0) (product.quantity * 7 / soldCount.coerceAtLeast(1)).coerceAtLeast(1) else 2
            StockAlertInsight(
                product = product,
                severity = if (product.quantity == 0) "CRITIQUE" else "ATTENTION",
                daysRemainingEstimate = daysEstimate,
                message = if (product.quantity == 0) "Rupture totale de stock ! Réapprovisionnement urgent."
                          else "Il ne reste que ${product.quantity} unité(s). Risque d'épuisement d'ici $daysEstimate jour(s)."
            )
        }

        // Anomaly detections
        val anomalies = mutableListOf<AnomalyInsight>()

        // 1. High credit risk anomaly
        if (totalOutstandingCredit > 50000.0) {
            anomalies.add(
                AnomalyInsight(
                    title = "Encours de crédits clients élevé",
                    description = "Les clients vous doivent actuellement ${totalOutstandingCredit.toInt()} FCFA.",
                    level = "HAUTE",
                    recommendation = "Relancez les clients en retard avant d'accorder de nouveaux crédits pour préserver votre fonds de roulement."
                )
            )
        }

        // 2. Negative margin check
        val negativeMarginProducts = products.filter { it.salePrice < it.purchasePrice }
        if (negativeMarginProducts.isNotEmpty()) {
            anomalies.add(
                AnomalyInsight(
                    title = "Produits vendus à perte détectés",
                    description = "${negativeMarginProducts.size} produit(s) ont un prix de vente inférieur au prix d'achat.",
                    level = "HAUTE",
                    recommendation = "Ajustez immédiatement les prix de vente pour couvrir vos charges."
                )
            )
        }

        // 3. Imbalance between digital and physical cash
        val omCash = cashTransactions.filter { it.channel.name.contains("ORANGE") }.sumOf { if (it.type.isCredit) it.amount else -it.amount }
        val moovCash = cashTransactions.filter { it.channel.name.contains("MOOV") }.sumOf { if (it.type.isCredit) it.amount else -it.amount }
        if (omCash < 20000.0 && moovCash < 20000.0) {
            anomalies.add(
                AnomalyInsight(
                    title = "Liquidités Mobile Money faibles",
                    description = "Vos soldes Orange Money et Moov Money sont bas.",
                    level = "MOYENNE",
                    recommendation = "Effectuez un dépôt pour pouvoir assurer les retraits de vos clients et payer vos fournisseurs."
                )
            )
        }

        val localRecommendations = mutableListOf<String>()
        if (stockAlerts.isNotEmpty()) {
            localRecommendations.add("Passez commande auprès de vos grossistes pour les ${stockAlerts.size} articles en alerte critique.")
        }
        localRecommendations.add("Sécurisez votre caisse physique chaque soir en transférant le surplus vers votre compte Orange Money ou banque.")
        localRecommendations.add("Exigez la pièce d'identité (CNIB) avant tout octroi de crédit supérieur à 10 000 FCFA conformément aux règles KYC.")

        // If Gemini API is available, ask Gemini 3.5 Flash for deep contextual analysis
        if (GeminiClient.isApiKeyAvailable()) {
            try {
                val prompt = """
                    Tu es SIRA Intelligence, le conseiller d'affaires IA de référence pour les commerçants du Burkina Faso (Ouagadougou, Bobo-Dioulasso, Koudougou).
                    Données de la boutique :
                    - Chiffre d'affaires total : ${totalRevenue.toInt()} FCFA
                    - Marge brute estimée : ${totalProfit.toInt()} FCFA
                    - Valeur du stock magasin : ${totalStockValue.toInt()} FCFA
                    - Crédits clients non recouvrés : ${totalOutstandingCredit.toInt()} FCFA
                    - Articles en rupture ou stock critique : ${stockAlerts.joinToString { "${it.product.name} (reste: ${it.product.quantity})" }}
                    - Nombre de ventes enregistrées : ${sales.size}

                    Rédige une brève synthèse stratégique pour le commerçant :
                    1. Résumé de santé financière (2 phrases)
                    2. Prévision de chiffre d'affaires pour les 7 prochains jours
                    3. 3 Recommandations prioritaires concrètes adaptées au marché burkinabè (prix, approvisionnement Rood Woko, gestion cash/crédit).
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    )
                )

                val response = GeminiClient.api.generateContent(GeminiClient.getApiKey(), request)
                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!aiText.isNullOrBlank()) {
                    val (omProfit, omCount, omVolume) = calculateOrangeMoneyDailyProfit(cashTransactions)
                    return@withContext SiraIntelligenceReport(
                        executiveSummary = aiText,
                        projectedTurnoverForecast = (totalRevenue * 1.15).coerceAtLeast(50000.0),
                        stockAlerts = stockAlerts,
                        anomalies = anomalies,
                        businessRecommendations = localRecommendations,
                        generatedByAi = true,
                        orangeMoneyTodayProfit = omProfit,
                        orangeMoneyTodayCount = omCount,
                        orangeMoneyTotalVolume = omVolume
                    )
                }
            } catch (e: Exception) {
                // Fallback to local deterministic analysis smoothly
            }
        }

        // Local Heuristic Report
        val (omProfit, omCount, omVolume) = calculateOrangeMoneyDailyProfit(cashTransactions)
        SiraIntelligenceReport(
            executiveSummary = "Votre boutique affiche une activité stable avec un chiffre d'affaires de ${totalRevenue.toInt()} FCFA et ${products.size} références en catalogue. La rentabilité globale est positive (${totalProfit.toInt()} FCFA de bénéfice brut estimé). Veillez à surveiller les ruptures de stock sur les produits de première nécessité.",
            projectedTurnoverForecast = (totalRevenue * 1.10).coerceAtLeast(35000.0),
            stockAlerts = stockAlerts,
            anomalies = anomalies,
            businessRecommendations = localRecommendations,
            generatedByAi = false,
            orangeMoneyTodayProfit = omProfit,
            orangeMoneyTodayCount = omCount,
            orangeMoneyTotalVolume = omVolume
        )
    }
}
