package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.entity.IdDocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class KycOcrResult(
    val detectedType: IdDocumentType,
    val fullName: String?,
    val documentNumber: String?,
    val dateOfBirth: String?,
    val expiryDate: String?,
    val issuingAuthority: String?,
    val rawExtractedText: String,
    val confidenceNotes: String,
    val isAiPowered: Boolean
)

class SiraKycOcrService {

    suspend fun analyzeDocument(bitmap: Bitmap): KycOcrResult = withContext(Dispatchers.IO) {
        if (!GeminiClient.isApiKeyAvailable()) {
            return@withContext fallbackLocalHeuristicOcr(
                "Mode hors-ligne / Clé API non configurée. Veuillez renseigner et vérifier manuellement les informations de la pièce d'identité selon les normes KYC en vigueur."
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                Tu es un système expert en vérification d'identité (KYC) et conformité pour les commerces au Burkina Faso.
                Analyse attentivement cette photo de document d'identité (CNIB du Burkina Faso, Passeport, Permis de conduire ou Carte consulaire).
                Extrais avec la plus grande précision les champs suivants sans rien inventer.
                Ne contourne jamais les exigences KYC.
                Format de réponse attendu :
                TYPE: [CNIB ou PASSEPORT ou PERMIS ou AUTRE]
                NOM_COMPLET: [Nom et Prénoms]
                NUMERO_PIECE: [ex: B12345678]
                DATE_NAISSANCE: [JJ/MM/AAAA si lisible]
                DATE_EXPIRATION: [JJ/MM/AAAA si lisible]
                AUTORITE: [ex: Office National d'Identification ONI Burkina Faso]
                OBSERVATIONS: [clarté de la photo, altérations visibles, intégrité]
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = "image/jpeg",
                                    data = base64Image
                                )
                            )
                        )
                    )
                )
            )

            val response = GeminiClient.api.generateContent(GeminiClient.getApiKey(), request)
            val outputText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            parseKycResponse(outputText)
        } catch (e: Exception) {
            fallbackLocalHeuristicOcr("Analyse IA indisponible (${e.localizedMessage ?: "Erreur réseau"}). Remplissage manuel vérifié obligatoire.")
        }
    }

    private fun parseKycResponse(text: String): KycOcrResult {
        var docType = IdDocumentType.CNIB
        var name: String? = null
        var docNum: String? = null
        var dob: String? = null
        var expiry: String? = null
        var authority: String? = null
        var observations = "Vérification visuelle requise par le commerçant avant enregistrement."

        text.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("TYPE:", ignoreCase = true) -> {
                    val value = trimmed.substringAfter(":").trim().uppercase()
                    docType = when {
                        value.contains("PASSEPORT") -> IdDocumentType.PASSEPORT
                        value.contains("PERMIS") -> IdDocumentType.PERMIS
                        value.contains("CONSULAIRE") -> IdDocumentType.CARTE_CONSULAIRE
                        else -> IdDocumentType.CNIB
                    }
                }
                trimmed.startsWith("NOM_COMPLET:", ignoreCase = true) -> {
                    name = trimmed.substringAfter(":").trim().takeIf { it.isNotBlank() && !it.contains("[") }
                }
                trimmed.startsWith("NUMERO_PIECE:", ignoreCase = true) -> {
                    docNum = trimmed.substringAfter(":").trim().takeIf { it.isNotBlank() && !it.contains("[") }
                }
                trimmed.startsWith("DATE_NAISSANCE:", ignoreCase = true) -> {
                    dob = trimmed.substringAfter(":").trim().takeIf { it.isNotBlank() && !it.contains("[") }
                }
                trimmed.startsWith("DATE_EXPIRATION:", ignoreCase = true) -> {
                    expiry = trimmed.substringAfter(":").trim().takeIf { it.isNotBlank() && !it.contains("[") }
                }
                trimmed.startsWith("AUTORITE:", ignoreCase = true) -> {
                    authority = trimmed.substringAfter(":").trim().takeIf { it.isNotBlank() && !it.contains("[") }
                }
                trimmed.startsWith("OBSERVATIONS:", ignoreCase = true) -> {
                    observations = trimmed.substringAfter(":").trim()
                }
            }
        }

        return KycOcrResult(
            detectedType = docType,
            fullName = name,
            documentNumber = docNum,
            dateOfBirth = dob,
            expiryDate = expiry,
            issuingAuthority = authority,
            rawExtractedText = text,
            confidenceNotes = observations,
            isAiPowered = true
        )
    }

    private fun fallbackLocalHeuristicOcr(message: String): KycOcrResult {
        return KycOcrResult(
            detectedType = IdDocumentType.CNIB,
            fullName = null,
            documentNumber = null,
            dateOfBirth = null,
            expiryDate = null,
            issuingAuthority = "Office National d'Identification (ONI BF)",
            rawExtractedText = message,
            confidenceNotes = "Mode manuel : le commerçant doit saisir et vérifier personnellement la pièce selon la réglementation burkinabè.",
            isAiPowered = false
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
