package com.immich.server.platform

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.nio.ByteOrder

actual object NetworkUtils {
    private var context: Context? = null

    fun initialize(ctx: Context) {
        context = ctx.applicationContext
    }

    actual fun getLocalIpAddress(): String? {
        val ctx = context ?: return null
        val wifiManager = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val wifiInfo = wifiManager.connectionInfo ?: return null

        var ip = wifiInfo.ipAddress
        // Convert from little-endian to big-endian if needed
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            ip = Integer.reverseBytes(ip)
        }

        val ipByteArray = ip.toBigInteger().toByteArray()
        return try {
            InetAddress.getByAddress(ipByteArray).hostAddress
        } catch (e: Exception) {
            null
        }
    }

    actual fun getServerUrl(port: Int): String {
        val ip = getLocalIpAddress() ?: "localhost"
        return "http://$ip:$port"
    }
}
