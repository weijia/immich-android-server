package com.immich.server.platform

import platform.UIKit.UIApplication
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

actual class PlatformNotification {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    private val notificationId = "immich_server_notification"

    actual fun showNotification(title: String, message: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            notificationId,
            content,
            null
        )

        notificationCenter.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Notification error: ${error.localizedDescription}")
            }
        }
    }

    actual fun createNotificationChannel() {
        // iOS doesn't use notification channels
        notificationCenter.requestAuthorizationWithOptions(
            platform.UserNotifications.UNAuthorizationOptionAlert or
            platform.UserNotifications.UNAuthorizationOptionSound
        ) { granted, error ->
            if (!granted) {
                println("Notification permission denied")
            }
        }
    }

    actual fun cancelNotification() {
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(notificationId))
    }
}
