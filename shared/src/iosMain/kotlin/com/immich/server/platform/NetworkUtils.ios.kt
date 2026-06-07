package com.immich.server.platform

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat
import platform.UIKit.UIDevice

actual object NetworkUtils {
    actual fun getLocalIpAddress(): String? {
        // iOS: Use platform-specific API to get WiFi IP
        // This is a simplified version - in production, use proper iOS networking APIs
        return null
    }

    actual fun getServerUrl(port: Int): String {
        val ip = getLocalIpAddress() ?: "localhost"
        return "http://$ip:$port"
    }
}
