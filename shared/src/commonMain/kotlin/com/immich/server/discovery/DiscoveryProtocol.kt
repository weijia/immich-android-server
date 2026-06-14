package com.immich.server.discovery

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.immich.server.platform.Logger

/**
 * UDP Discovery Protocol for Immich Server
 *
 * Protocol Versions:
 * - v1.0: Basic discovery (DISCOVER_IMMICH_SERVER -> response)
 * - v2.0: Server ID matching (response includes serverId)
 * - v3.0: Token-based signing (request includes challenge nonce, response includes signature)
 *
 * Port: 2284 (one above the HTTP server port 2283)
 *
 * @see docs/discovery-protocol.md
 */
object DiscoveryProtocol {
    const val DISCOVERY_PORT = 2284
    const val BROADCAST_ADDRESS = "255.255.255.255"
    const val DISCOVER_REQUEST_V1 = "DISCOVER_IMMICH_SERVER"
    const val DISCOVER_REQUEST_PREFIX_V3 = "DISCOVER_IMMICH_SERVER:"
    const val DISCOVER_RESPONSE_PREFIX = "IMMICH_SERVER_RESPONSE:"
    
    private val log = Logger("DiscoveryProtocol")

    // Lenient JSON parser for discovery responses
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = false
    }

    // ==================== Response Models ====================

    /**
     * v1.0 Response - Basic discovery
     */
    @Serializable
    data class DiscoveryResponseV1(
        val serverUrl: String,
        val version: String = "3.0.0",
        val serverName: String = "Immich Android Server",
        val timestamp: Long = 0
    )

    /**
     * v2.0 Response - Server ID matching
     */
    @Serializable
    data class DiscoveryResponseV2(
        val serverId: String,
        val serverName: String,
        val serverUrl: String,
        val version: String = "3.0.0",
        val timestamp: Long = 0
    )

    /**
     * v3.0 Response - Token-based signing
     */
    @Serializable
    data class DiscoveryResponseV3(
        val serverId: String,
        val serverName: String,
        val serverUrl: String,
        val version: String = "3.0.0",
        val timestamp: Long,
        val challengeNonce: String,
        val signature: String
    )

    // ==================== Request Models ====================

    /**
     * v3.0 Request - Contains client ID and challenge nonce
     */
    @Serializable
    data class DiscoveryRequestV3(
        val clientId: String,
        val challengeNonce: String
    )

    // ==================== Response Creation ====================

    /**
     * Create v1.0 response (basic discovery)
     */
    fun createResponseV1(serverUrl: String): String {
        val url = ensureApiSuffix(serverUrl)
        val response = DiscoveryResponseV1(
            serverUrl = url,
            version = "3.0.0",
            serverName = "Immich Android Server",
            timestamp = Clock.System.now().epochSeconds
        )
        return DISCOVER_RESPONSE_PREFIX + json.encodeToString(response)
    }

    /**
     * Create v2.0 response (server ID matching)
     */
    fun createResponseV2(
        serverId: String,
        serverName: String,
        serverUrl: String
    ): String {
        val url = ensureApiSuffix(serverUrl)
        val response = DiscoveryResponseV2(
            serverId = serverId,
            serverName = serverName,
            serverUrl = url,
            version = "3.0.0",
            timestamp = Clock.System.now().epochSeconds
        )
        return DISCOVER_RESPONSE_PREFIX + json.encodeToString(response)
    }

    /**
     * Create v3.0 response (token-based signing)
     * 
     * @param serverId Server unique identifier
     * @param serverName Server display name
     * @param serverUrl Server URL
     * @param challengeNonce Client-provided nonce (must match request)
     * @param signFunction Function to generate signature
     */
    fun createResponseV3(
        serverId: String,
        serverName: String,
        serverUrl: String,
        challengeNonce: String,
        signFunction: (serverUrl: String, timestamp: Long, challengeNonce: String) -> String
    ): String {
        val url = ensureApiSuffix(serverUrl)
        val timestamp = Clock.System.now().epochSeconds
        val signature = signFunction(url, timestamp, challengeNonce)
        
        log.d("[DiscoveryProtocol] Creating v3.0 response: serverId=$serverId, nonce=$challengeNonce, sig=$signature")
        
        val response = DiscoveryResponseV3(
            serverId = serverId,
            serverName = serverName,
            serverUrl = url,
            version = "3.0.0",
            timestamp = timestamp,
            challengeNonce = challengeNonce,
            signature = signature
        )
        return DISCOVER_RESPONSE_PREFIX + json.encodeToString(response)
    }

    /**
     * Create response with automatic version detection based on request.
     * 
     * @param request Raw request string
     * @param serverId Server unique identifier
     * @param serverName Server display name
     * @param serverUrl Server URL
     * @param signFunction Function to generate signature (for v3.0)
     */
    fun createResponse(
        request: String,
        serverId: String,
        serverName: String,
        serverUrl: String,
        signFunction: (serverUrl: String, timestamp: Long, challengeNonce: String) -> String
    ): String {
        val parsedRequest = parseRequest(request)
        
        return when (parsedRequest) {
            is DiscoveryRequestV3 -> {
                // v3.0 request - return signed response
                createResponseV3(
                    serverId = serverId,
                    serverName = serverName,
                    serverUrl = serverUrl,
                    challengeNonce = parsedRequest.challengeNonce,
                    signFunction = signFunction
                )
            }
            null -> {
                // v1.0 request - return basic response (for compatibility)
                // But we'll include serverId for v2.0 clients
                createResponseV2(
                    serverId = serverId,
                    serverName = serverName,
                    serverUrl = serverUrl
                )
            }
        }
    }

    // ==================== Request Parsing ====================

    /**
     * Parse discovery request and detect version.
     * 
     * @return DiscoveryRequestV3 for v3.0 requests, null for v1.0 requests
     */
    fun parseRequest(data: String): DiscoveryRequestV3? {
        val trimmed = data.trim()
        
        // v1.0 request
        if (trimmed == DISCOVER_REQUEST_V1) {
            log.d("[DiscoveryProtocol] Received v1.0 request")
            return null
        }
        
        // v3.0 request: DISCOVER_IMMICH_SERVER:<clientId>:<challengeNonce>
        if (trimmed.startsWith(DISCOVER_REQUEST_PREFIX_V3)) {
            val parts = trimmed.removePrefix(DISCOVER_REQUEST_PREFIX_V3).split(":")
            if (parts.size >= 2) {
                val clientId = parts[0]
                val challengeNonce = parts[1]
                log.d("[DiscoveryProtocol] Received v3.0 request: clientId=$clientId, nonce=$challengeNonce")
                return DiscoveryRequestV3(clientId = clientId, challengeNonce = challengeNonce)
            }
        }
        
        log.w("[DiscoveryProtocol] Unknown request format: $trimmed")
        return null
    }

    /**
     * Check if data is a valid discovery request (any version).
     */
    fun isDiscoveryRequest(data: String): Boolean {
        val trimmed = data.trim()
        return trimmed == DISCOVER_REQUEST_V1 || trimmed.startsWith(DISCOVER_REQUEST_PREFIX_V3)
    }

    // ==================== Response Parsing ====================

    /**
     * Parse response JSON (any version).
     */
    fun parseResponse(data: String): Any? {
        return try {
            val jsonStr = if (data.startsWith(DISCOVER_RESPONSE_PREFIX)) {
                data.removePrefix(DISCOVER_RESPONSE_PREFIX)
            } else {
                data
            }
            
            // Try v3.0 first (has signature)
            try {
                json.decodeFromString<DiscoveryResponseV3>(jsonStr)
            } catch (e: Exception) {
                // Try v2.0 (has serverId)
                try {
                    json.decodeFromString<DiscoveryResponseV2>(jsonStr)
                } catch (e2: Exception) {
                    // Try v1.0
                    json.decodeFromString<DiscoveryResponseV1>(jsonStr)
                }
            }
        } catch (e: Exception) {
            log.e("[DiscoveryProtocol] Failed to parse response: $e")
            null
        }
    }

    /**
     * Parse v3.0 response specifically.
     */
    fun parseResponseV3(data: String): DiscoveryResponseV3? {
        return try {
            val jsonStr = if (data.startsWith(DISCOVER_RESPONSE_PREFIX)) {
                data.removePrefix(DISCOVER_RESPONSE_PREFIX)
            } else {
                data
            }
            json.decodeFromString<DiscoveryResponseV3>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse v2.0 response specifically.
     */
    fun parseResponseV2(data: String): DiscoveryResponseV2? {
        return try {
            val jsonStr = if (data.startsWith(DISCOVER_RESPONSE_PREFIX)) {
                data.removePrefix(DISCOVER_RESPONSE_PREFIX)
            } else {
                data
            }
            json.decodeFromString<DiscoveryResponseV2>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    // ==================== Helper Functions ====================

    /**
     * Ensure URL ends with /api suffix.
     */
    private fun ensureApiSuffix(url: String): String {
        return if (url.endsWith("/api")) url else "$url/api"
    }

    /**
     * Get server URL from any response version.
     */
    fun getServerUrl(response: Any): String? {
        return when (response) {
            is DiscoveryResponseV3 -> response.serverUrl
            is DiscoveryResponseV2 -> response.serverUrl
            is DiscoveryResponseV1 -> response.serverUrl
            else -> null
        }
    }

    /**
     * Get server ID from response (v2.0 and v3.0 only).
     */
    fun getServerId(response: Any): String? {
        return when (response) {
            is DiscoveryResponseV3 -> response.serverId
            is DiscoveryResponseV2 -> response.serverId
            else -> null
        }
    }
}