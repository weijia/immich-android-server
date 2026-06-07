package com.immich.server.platform

import java.net.InetAddress
import java.net.NetworkInterface

actual object NetworkUtils {
    actual fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    actual fun getServerUrl(port: Int): String {
        val ip = getLocalIpAddress() ?: "localhost"
        return "http://$ip:$port"
    }
}
