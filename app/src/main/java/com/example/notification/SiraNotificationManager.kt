package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.NumberFormat
import java.util.Locale

object SiraNotificationManager {
    private const val CHANNEL_ID = "sira_transactions_channel"
    private const val CHANNEL_NAME = "Transactions SIRA"
    private const val CHANNEL_DESC = "Notifications officielles de validation des transactions SIRA"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Triggers the exact verified transaction notification requested:
     * - "Transaction validée"
     * - Identifiant de la transaction
     * - Client / Bénéficiaire parent
     * - Commission (frais)
     * - Solde avant et solde après
     */
    fun showTransactionValidatedNotification(
        context: Context,
        transactionId: String,
        customerName: String,
        amount: Double,
        commission: Double,
        balanceBefore: Double,
        balanceAfter: Double,
        channelLabel: String
    ) {
        initChannel(context)
        val numberFormat = NumberFormat.getIntegerInstance(Locale.FRENCH)
        val formattedAmount = numberFormat.format(amount.toLong())
        val formattedCommission = numberFormat.format(commission.toLong())
        val formattedBefore = numberFormat.format(balanceBefore.toLong())
        val formattedAfter = numberFormat.format(balanceAfter.toLong())

        val title = "Transaction validée : $transactionId"
        val shortContent = "Montant: $formattedAmount F | Commission: $formattedCommission F | Solde: $formattedAfter F"
        
        val bigText = """
            ✓ Transaction validée avec succès
            • Réf Transaction : $transactionId
            • Client / Bénéficiaire : $customerName
            • Canal : $channelLabel
            • Montant : $formattedAmount FCFA
            • Commission : $formattedCommission FCFA
            • Solde avant : $formattedBefore FCFA
            • Solde après : $formattedAfter FCFA
        """.trimIndent()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Handled gracefully if notification permission wasn't granted yet
        }
    }
}
