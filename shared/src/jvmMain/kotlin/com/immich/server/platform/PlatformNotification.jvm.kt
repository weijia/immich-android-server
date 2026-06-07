package com.immich.server.platform

actual class PlatformNotification {
    actual fun showNotification(title: String, message: String) {
        println("[NOTIFICATION] $title: $message")
    }

    actual fun createNotificationChannel() {
        // No-op for JVM
    }

    actual fun cancelNotification() {
        // No-op for JVM
    }
}
