package com.immich.server.platform

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.nio.ByteOrder

actual object NetworkUtils {
    private var context: Context? = null

    fun initialize(ctx: Context) {
        context = ctx.applicationContext
        Logger.i("[NetworkUtils] Initialized with context")
    }

    actual fun getLocalIpAddress(): String? {
        val ctx = context
        if (ctx == null) {
            Logger.e("[NetworkUtils] Context not initialized, cannot get IP", null)
            return null
        }

        val wifiManager = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            Logger.e("[NetworkUtils] WiFiManager not available", null)
            return null
        }

        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo == null) {
            Logger.w("[NetworkUtils] WiFi connection info is null")
            return null
        }

        val ipAddress = wifiInfo.ipAddress
        Logger.d("[NetworkUtils] Raw IP address from WiFiInfo: $ipAddress")

        if (ipAddress == 0) {
            Logger.w("[NetworkUtils] IP address is 0, not connected to WiFi")
            return null
        }

        // Convert from little-endian to big-endian if needed
        var ip = ipAddress
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            ip = Integer.reverseBytes(ip)
            Logger.d("[NetworkUtils] Reversed bytes for little-endian: $ip")
        }

        val ipByteArray = ip.toBigInteger().toByteArray()
        return try {
            val address = InetAddress.getByAddress(ipByteArray)
            val hostAddress = address.hostAddress
            Logger.i("[NetworkUtils] Local IP address: $hostAddress")
            hostAddress
        } catch (e: Exception) {
            Logger.e("[NetworkUtils] Failed to convert IP address", e)
            null
        }
    }

    actual fun getServerUrl(port: Int): String {
        Logger.d("[NetworkUtils] Getting server URL for port $port")
        val ip = getLocalIpAddress()
        val url = if (ip != null) {
            "http://$ip:$port"
        } else {
            Logger.w("[NetworkUtils] Could not get local IP, falling back to localhost")
            "http://localhost:$port"
        }
        Logger.i("[NetworkUtils] Server URL: $url")
        return url
    }
}
