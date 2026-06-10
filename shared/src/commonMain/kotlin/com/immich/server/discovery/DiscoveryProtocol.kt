package com.immich.server.discovery

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * UDP Discovery Protocol for Immich Server
 *
 * Protocol (v1.0):
 * 1. Client sends UDP broadcast: "DISCOVER_IMMICH_SERVER"
 * 2. Server responds with: "IMMICH_SERVER_RESPONSE:<JSON>"
 *
 * Port: 2284 (one above the HTTP server port 2283)
 *
 * @see docs/discovery-protocol.md
 */
object DiscoveryProtocol {
    const val DISCOVERY_PORT = 2284
    const val BROADCAST_ADDRESS = "255.255.255.255"
    const val DISCOVER_REQUEST = "DISCOVER_IMMICH_SERVER"
    const val DISCOVER_RESPONSE_PREFIX = "IMMICH_SERVER_RESPONSE:"

    // Lenient JSON parser for discovery responses
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Serializable
    data class DiscoveryResponse(
        val serverUrl: String,
        val version: String = "1.108.0",
        val serverName: String = "Immich Android Server",
        val timestamp: Long = 0
    )

    /**
     * Create response with IMMICH_SERVER_RESPONSE: prefix
     * The serverUrl should include /api suffix (e.g. http://192.168.1.21:2283/api)
     */
    fun createResponse(serverUrl: String): String {
        // Ensure serverUrl ends with /api for Immich client compatibility
        val url = if (serverUrl.endsWith("/api")) serverUrl else "$serverUrl/api"
        val response = DiscoveryResponse(
            serverUrl = url,
            version = "1.108.0",
            serverName = "Immich Android Server",
            timestamp = kotlinx.datetime.Clock.System.now().epochSeconds
        )
        return DISCOVER_RESPONSE_PREFIX + json.encodeToString(response)
    }

    fun parseResponse(data: String): DiscoveryResponse? {
        return try {
            val jsonStr = if (data.startsWith(DISCOVER_RESPONSE_PREFIX)) {
                data.removePrefix(DISCOVER_RESPONSE_PREFIX)
            } else {
                data
            }
            json.decodeFromString<DiscoveryResponse>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    fun isDiscoveryRequest(data: String): Boolean {
        return data.trim() == DISCOVER_REQUEST
    }
}
