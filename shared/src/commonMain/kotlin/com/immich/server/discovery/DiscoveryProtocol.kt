package com.immich.server.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * UDP Discovery Protocol for Immich Server
 *
 * Protocol:
 * 1. Client sends UDP broadcast: "DISCOVER_IMMICH_SERVER"
 * 2. Server responds with JSON containing server URL
 *
 * Port: 2284 (one above the HTTP server port 2283)
 */
object DiscoveryProtocol {
    const val DISCOVERY_PORT = 2284
    const val BROADCAST_ADDRESS = "255.255.255.255"
    const val DISCOVER_REQUEST = "DISCOVER_IMMICH_SERVER"
    const val DISCOVER_RESPONSE_PREFIX = "IMMICH_SERVER_RESPONSE:"

    @Serializable
    data class DiscoveryResponse(
        val serverUrl: String,
        val version: String = "1.108.0",
        val serverName: String = "Immich Android Server",
        val timestamp: Long = 0
    )

    fun createResponse(serverUrl: String): String {
        val response = DiscoveryResponse(
            serverUrl = serverUrl,
            timestamp = kotlinx.datetime.Clock.System.now().epochSeconds
        )
        return DISCOVER_RESPONSE_PREFIX + Json.encodeToString(response)
    }

    fun parseResponse(data: String): DiscoveryResponse? {
        return try {
            if (data.startsWith(DISCOVER_RESPONSE_PREFIX)) {
                val json = data.removePrefix(DISCOVER_RESPONSE_PREFIX)
                Json.decodeFromString<DiscoveryResponse>(json)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isDiscoveryRequest(data: String): Boolean {
        return data.trim() == DISCOVER_REQUEST
    }
}
