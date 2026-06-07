package com.immich.server.platform

/**
 * Platform-specific notification abstraction
 * For showing server status notifications
 */
expect class PlatformNotification {
    fun showNotification(title: String, message: String)
    fun createNotificationChannel()
    fun cancelNotification()
}
