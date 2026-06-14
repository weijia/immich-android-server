package com.immich.server.service

import com.immich.server.db.ServerConfigQueries
import com.immich.server.model.ServerConfig
import com.immich.server.util.HmacUtils
import com.immich.server.util.Logger
import kotlinx.datetime.Clock
import kotlin.random.Random

class ServerConfigService(private val queries: ServerConfigQueries) {
    private val log = Logger("ServerConfigService")
    
    private var _config: ServerConfig? = null
    
    /**
     * Get or create server configuration.
     * On first startup, generates new serverId and serverToken.
     */
    fun getOrCreateConfig(): ServerConfig {
        if (_config != null) {
            return _config!!
        }
        
        // Check if config exists in database
        val existingConfig = queries.selectFirst().executeAsOneOrNull()
        
        if (existingConfig != null) {
            _config = ServerConfig(
                serverId = existingConfig.server_id,
                serverName = existingConfig.server_name,
                serverToken = existingConfig.server_token,
                createdAt = existingConfig.created_at,
                updatedAt = existingConfig.updated_at
            )
            log.i("[ServerConfigService] Loaded existing config: serverId=${_config!!.serverId}")
            return _config!!
        }
        
        // Generate new config
        val now = Clock.System.now().epochSeconds
        val serverId = generateServerId()
        val serverToken = generateServerToken()
        val serverName = "Immich Android Server"
        
        log.i("[ServerConfigService] Generating new config: serverId=$serverId")
        
        queries.insert(
            server_id = serverId,
            server_name = serverName,
            server_token = serverToken,
            created_at = now,
            updated_at = now
        )
        
        _config = ServerConfig(
            serverId = serverId,
            serverName = serverName,
            serverToken = serverToken,
            createdAt = now,
            updatedAt = now
        )
        
        log.i("[ServerConfigService] New config created and stored")
        return _config!!
    }
    
    /**
     * Get current server configuration.
     * Returns null if not initialized.
     */
    fun getConfig(): ServerConfig? = _config
    
    /**
     * Get server ID.
     */
    fun getServerId(): String {
        return getOrCreateConfig().serverId
    }
    
    /**
     * Get server token for signing.
     */
    fun getServerToken(): String {
        return getOrCreateConfig().serverToken
    }
    
    /**
     * Update server name.
     */
    fun updateServerName(newName: String) {
        val config = getOrCreateConfig()
        val now = Clock.System.now().epochSeconds
        
        queries.updateServerName(
            server_name = newName,
            updated_at = now,
            server_id = config.serverId
        )
        
        _config = config.copy(serverName = newName, updatedAt = now)
        log.i("[ServerConfigService] Server name updated to: $newName")
    }
    
    /**
     * Regenerate server token (admin operation).
     */
    fun regenerateServerToken(): String {
        val config = getOrCreateConfig()
        val newToken = generateServerToken()
        val now = Clock.System.now().epochSeconds
        
        queries.updateServerToken(
            server_token = newToken,
            updated_at = now,
            server_id = config.serverId
        )
        
        _config = config.copy(serverToken = newToken, updatedAt = now)
        log.i("[ServerConfigService] Server token regenerated")
        return newToken
    }
    
    /**
     * Generate HMAC-SHA256 signature for discovery response.
     */
    fun signDiscoveryResponse(
        serverUrl: String,
        timestamp: Long,
        challengeNonce: String
    ): String {
        val config = getOrCreateConfig()
        val dataToSign = "${config.serverId}|$serverUrl|$timestamp|$challengeNonce"
        
        log.d("[ServerConfigService] Signing data: $dataToSign")
        
        val signature = HmacUtils.hmacSha256Hex(
            key = config.serverToken,
            data = dataToSign
        )
        
        log.d("[ServerConfigService] Generated signature: $signature")
        return signature
    }
    
    /**
     * Verify if a signature is valid.
     */
    fun verifySignature(
        serverUrl: String,
        timestamp: Long,
        challengeNonce: String,
        signature: String
    ): Boolean {
        val expectedSignature = signDiscoveryResponse(serverUrl, timestamp, challengeNonce)
        return expectedSignature == signature
    }
    
    /**
     * Generate UUID v4 server ID.
     */
    private fun generateServerId(): String {
        val bytes = Random.nextBytes(16)
        // Set version bits (4 in UUID v4)
        bytes[6] = (bytes[6] and 0x0f) or 0x40
        // Set variant bits
        bytes[8] = (bytes[8] and 0x3f) or 0x80
        
        return bytesToUuid(bytes)
    }
    
    /**
     * Generate 256-bit (32 bytes) server token.
     */
    private fun generateServerToken(): String {
        val bytes = Random.nextBytes(32)
        return bytes.toHexString()
    }
    
    /**
     * Convert bytes to UUID string format.
     */
    private fun bytesToUuid(bytes: ByteArray): String {
        val hex = bytes.toHexString()
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
    }
}

/**
 * Extension to convert ByteArray to hex string.
 */
private fun ByteArray.toHexString(): String {
    return this.joinToString("") { byte -> "%02x".format(byte) }
}