package com.immich.server.platform

/**
 * Platform-specific network utilities
 * For getting device IP address
 */
expect object NetworkUtils {
    fun getLocalIpAddress(): String?
    fun getServerUrl(port: Int = 2283): String
}
