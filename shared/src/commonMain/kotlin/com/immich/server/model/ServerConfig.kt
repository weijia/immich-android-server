package com.immich.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val serverId: String,
    val serverName: String,
    val serverToken: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class TokenExchangeRequest(
    val clientId: String
)

@Serializable
data class TokenExchangeResponse(
    val serverToken: String,
    val serverId: String,
    val expiresAt: Long? = null
)