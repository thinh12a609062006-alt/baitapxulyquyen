package com.example.baitapxulyquyen

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

const val CHANNEL_ID = "thong_bao_chung"
const val EXTRA_TITLE = "extra_title"
const val EXTRA_CONTENT = "extra_content"


data class NotificationData(
    val title: String,
    val content: String
)


fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Thông báo chung",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo và nhắc nhở của ứng dụng"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

@SuppressLint("MissingPermission")
fun showNotification(context: Context, title: String, content: String) {
    if (!hasNotificationPermission(context)) return

    val notificationId = System.currentTimeMillis().toInt()

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_CONTENT, content)
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
}