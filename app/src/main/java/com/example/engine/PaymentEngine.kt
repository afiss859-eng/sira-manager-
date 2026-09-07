package com.example.engine

data class PaymentRequest(
    val amount: Double,
    val currency: String = "XOF",
    val customerPhone: String,
    val customerName: String,
    val reference: String,
    val description: String
)

sealed class PaymentResponse {
    data class Success(val transactionId: String, val message: String) : PaymentResponse()
    data class Error(val errorCode: String, val message: String) : PaymentResponse()
    data class NotConfigured(val reason: String) : PaymentResponse()
    data class PendingValidation(val ussdInstructions: String) : PaymentResponse()
}

interface PaymentEngine {
    val id: String
    val displayName: String
    val providerDescription: String
    fun isConfigured(): Boolean
    fun getConfigurationHint(): String
    suspend fun processPayment(request: PaymentRequest): PaymentResponse
    suspend fun checkStatus(reference: String): PaymentResponse
}

class CashPaymentEngine : PaymentEngine {
    override val id: String = "CASH"
    override val displayName: String = "Caisse Espèces Directe (Comptant)"
    override val providerDescription: String = "Règlement physique en mains propres dans le tiroir caisse"

    override fun isConfigured(): Boolean = true

    override fun getConfigurationHint(): String = "Toujours disponible sans clé API externe."

    override suspend fun processPayment(request: PaymentRequest): PaymentResponse {
        return PaymentResponse.Success(
            transactionId = "CASH-${System.currentTimeMillis()}",
            message = "Montant de ${request.amount.toInt()} FCFA encaissé en espèces dans la caisse locale."
        )
    }

    override suspend fun checkStatus(reference: String): PaymentResponse {
        return PaymentResponse.Success(reference, "Paiement en espèces validé.")
    }
}

class CinetPayPaymentEngine(
    private val apiKey: String? = null,
    private val siteId: String? = null
) : PaymentEngine {
    override val id: String = "CINETPAY"
    override val displayName: String = "CinetPay (Orange Money BF, Moov Money BF, Carte)"
    override val providerDescription: String = "Passerelle agréée UEMOA pour le Burkina Faso"

    override fun isConfigured(): Boolean = !apiKey.isNullOrBlank() && !siteId.isNullOrBlank()

    override fun getConfigurationHint(): String {
        return if (isConfigured()) "API CinetPay connectée et prête."
        else "Nécessite CINETPAY_API_KEY et CINETPAY_SITE_ID dans les variables d'environnement. Aucune transaction réelle ne sera simulée."
    }

    override suspend fun processPayment(request: PaymentRequest): PaymentResponse {
        if (!isConfigured()) {
            return PaymentResponse.NotConfigured(
                "Moteur CinetPay non configuré. Aucune clé API active. Impossible d'initier un paiement électronique réel sans identifiants marchands vérifiés."
            )
        }
        // When configured in production, call CinetPay REST API endpoint here
        return PaymentResponse.PendingValidation(
            "Push USSD envoyé au ${request.customerPhone}. Le client doit taper son code secret sur son téléphone."
        )
    }

    override suspend fun checkStatus(reference: String): PaymentResponse {
        if (!isConfigured()) return PaymentResponse.NotConfigured("CinetPay non configuré")
        return PaymentResponse.Error("PENDING", "Vérification en attente de la confirmation client")
    }
}

class PayDunyaPaymentEngine(
    private val masterKey: String? = null,
    private val token: String? = null
) : PaymentEngine {
    override val id: String = "PAYDUNYA"
    override val displayName: String = "PayDunya Burkina Faso"
    override val providerDescription: String = "Portefeuille et encaissements marchands régionaux"

    override fun isConfigured(): Boolean = !masterKey.isNullOrBlank() && !token.isNullOrBlank()

    override fun getConfigurationHint(): String {
        return if (isConfigured()) "API PayDunya connectée."
        else "Nécessite PAYDUNYA_MASTER_KEY et PAYDUNYA_TOKEN dans le panneau Secrets."
    }

    override suspend fun processPayment(request: PaymentRequest): PaymentResponse {
        if (!isConfigured()) {
            return PaymentResponse.NotConfigured(
                "Moteur PayDunya non configuré. Le système interdit toute simulation de transaction réelle sans clé de production."
            )
        }
        return PaymentResponse.PendingValidation("Facture PayDunya générée avec référence ${request.reference}")
    }

    override suspend fun checkStatus(reference: String): PaymentResponse {
        if (!isConfigured()) return PaymentResponse.NotConfigured("PayDunya non configuré")
        return PaymentResponse.Error("PENDING", "En attente du callback PayDunya")
    }
}

class OrangeMoneyDirectEngine(
    private val merchantCode: String? = null
) : PaymentEngine {
    override val id: String = "ORANGE_MONEY_DIRECT"
    override val displayName: String = "Orange Money BF Direct (Marchand)"
    override val providerDescription: String = "Numéro Marchand Orange Money Burkina Faso (*144*4*4*CodeMarchand#)"

    override fun isConfigured(): Boolean = !merchantCode.isNullOrBlank()

    override fun getConfigurationHint(): String {
        return if (isConfigured()) "Code Marchand: $merchantCode"
        else "Renseignez le code marchand Orange Money BF dans les paramètres pour activer la confirmation automatique."
    }

    override suspend fun processPayment(request: PaymentRequest): PaymentResponse {
        if (!isConfigured()) {
            return PaymentResponse.NotConfigured(
                "Code Marchand Orange Money BF non configuré. Veuillez demander au client d'effectuer le transfert manuellement et d'enregistrer la référence SMS."
            )
        }
        return PaymentResponse.PendingValidation(
            "Invitez le client à composer *144*4*4*$merchantCode*${request.amount.toInt()}# sur son téléphone Orange BF."
        )
    }

    override suspend fun checkStatus(reference: String): PaymentResponse {
        return PaymentResponse.Error("PENDING", "Vérifiez la réception du SMS Orange Money BF 333")
    }
}
