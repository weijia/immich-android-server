package com.immich.server.service

import com.immich.server.db.ServerConfigQueries
import com.immich.server.model.ServerConfig
import com.immich.server.util.HmacUtils
import com.immich.server.platform.Logger
import kotlinx.datetime.Clock
import kotlin.random.Random

class ServerConfigService(private val queries: ServerConfigQueries) {
    
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
            Logger.i("[ServerConfigService] Loaded existing config: serverId=${_config!!.serverId}")
            return _config!!
        }
        
        // Generate new config
        val now = Clock.System.now().epochSeconds
        val serverId = generateServerId()
        val serverToken = generateServerToken()
        val serverName = "Immich Android Server"
        
        Logger.i("[ServerConfigService] Generating new config: serverId=$serverId")
        
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
        
        Logger.i("[ServerConfigService] New config created and stored")
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
        Logger.i("[ServerConfigService] Server name updated to: $newName")
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
        Logger.i("[ServerConfigService] Server token regenerated")
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
        
        Logger.d("[ServerConfigService] Signing data: $dataToSign")
        
        val signature = HmacUtils.hmacSha256Hex(
            key = config.serverToken,
            data = dataToSign
        )
        
        Logger.d("[ServerConfigService] Generated signature: $signature")
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
        // Generate random UUID v4 using standard format
        val random = Random.nextBytes(16)
        val hex = random.toHexString()
        
        // Format as UUID: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        // where 4 is version and y is variant (8, 9, a, or b)
        val timeLow = hex.substring(0, 8)
        val timeMid = hex.substring(8, 12)
        val timeHiAndVersion = "4" + hex.substring(13, 16)  // Version 4
        val clockSeqHiAndReserved = ((hex.substring(16, 17).toIntOrNull() ?: 0) % 4 + 8).toString()  // Variant (8-b)
        val clockSeqLow = hex.substring(17, 20)
        val node = hex.substring(20, 32)
        
        return "$timeLow-$timeMid-$timeHiAndVersion-$clockSeqHiAndReserved$clockSeqLow-$node"
    }
    
    /**
     * Generate 256-bit (32 bytes) server token.
     */
    private fun generateServerToken(): String {
        val bytes = Random.nextBytes(32)
        return bytes.toHexString()
    }
    
}

/**
 * Extension to convert ByteArray to hex string.
 */
private fun ByteArray.toHexString(): String {
    return this.joinToString("") { byte -> "%02x".format(byte) }
}