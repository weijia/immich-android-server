package com.immich.server.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.immich.server.R

actual class PlatformNotification(private val context: Context) {
    private val channelId = "immich_server_channel"
    private val notificationId = 1
    private var notificationManager: NotificationManager? = null

    actual fun showNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()

        notificationManager?.notify(notificationId, notification)
    }

    actual fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Immich Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Immich server notifications"
            }
            notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    actual fun cancelNotification() {
        notificationManager?.cancel(notificationId)
    }
}
