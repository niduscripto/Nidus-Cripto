package com.nidus.cripto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "nidus_tx_channel"

    private fun getStringRes(lang: String, key: String): String {
        return when (lang) {
            "en" -> when (key) {
                "channel_name" -> "Received Transactions"
                "channel_description" -> "Notifications when you receive new Bitcoins"
                "notification_title" -> "Received - New Transaction"
                else -> key
            }
            else -> when (key) {
                "channel_name" -> "Transacciones Recibidas"
                "channel_description" -> "Notificaciones cuando recibes nuevos Bitcoins"
                "notification_title" -> "Recibido - Nueva Transacción"
                else -> key
            }
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val currentLanguage = StorageUtils.getLanguage(context)
            val soundUri: Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.tx_receive}")

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                getStringRes(currentLanguage, "channel_name"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getStringRes(currentLanguage, "channel_description")
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReceivedTransactionNotification(context: Context, txid: String, amountBtc: String, usdValue: String) {
        createNotificationChannel(context)
        val currentLanguage = StorageUtils.getLanguage(context)

        val shortTxid = if (txid.length > 16) "${txid.take(8)}...${txid.takeLast(6)}" else txid
        val contentText = "+$amountBtc BTC (≈ $usdValue USD)\nTX: $shortTxid"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "history")
            putExtra("tx_filter", txid)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(getStringRes(currentLanguage, "notification_title"))
            .setContentText("+$amountBtc BTC  (≈ $usdValue USD)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.tx_receive}")
            builder.setSound(soundUri)
        }

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}