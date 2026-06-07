package com.immich.server.android.legacy

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Legacy Server Service for Android 4.1+ (API 16-20)
 * Uses standard Service without foreground on low API levels
 */
class LegacyServerService : Service() {

    private val notificationId = 1
    private val channelId = "immich_server_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // On API 26+, use foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(notificationId, createNotification())
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Immich Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Immich server running"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, LegacyMainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Immich Server")
            .setContentText("Server is running on port 2283")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
