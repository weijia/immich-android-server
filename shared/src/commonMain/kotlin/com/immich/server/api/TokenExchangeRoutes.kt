package com.immich.server.api

import com.immich.server.model.TokenExchangeRequest
import com.immich.server.model.TokenExchangeResponse
import com.immich.server.service.ServerConfigService
import com.immich.server.service.AuthService
import com.immich.server.platform.Logger
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/**
 * Token Exchange Routes
 * 
 * POST /api/auth/token-exchange - Exchange server token for secure discovery (v3.0)
 * 
 * Requires valid Authorization header with Bearer token (accessToken from login)
 */
fun Route.tokenExchangeRoutes(
    serverConfigService: ServerConfigService,
    authService: AuthService
) {
    post("/auth/token-exchange") {
        Logger.i("[TokenExchangeRoutes] POST /auth/token-exchange received")
        
        try {
            // Check Authorization header
            val authHeader = call.request.header("Authorization")
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Logger.w("[TokenExchangeRoutes] Missing or invalid Authorization header")
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Missing Authorization header"))
                return@post
            }
            
            val accessToken = authHeader.removePrefix("Bearer ")
            Logger.d("[TokenExchangeRoutes] Access token: $accessToken")
            
            // Validate access token (format: token_userId_timestamp)
            val tokenParts = accessToken.split("_")
            if (tokenParts.size < 2) {
                Logger.w("[TokenExchangeRoutes] Invalid token format")
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Invalid token format"))
                return@post
            }
            
            val userId = tokenParts[1]
            Logger.d("[TokenExchangeRoutes] User ID from token: $userId")
            
            // Check if user exists
            val user = authService.getUserById(userId)
            if (user == null) {
                Logger.w("[TokenExchangeRoutes] User not found: $userId")
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "User not found"))
                return@post
            }
            
            // Process token exchange request
            val request = call.receive<TokenExchangeRequest>()
            Logger.d("[TokenExchangeRoutes] Client ID: ${request.clientId}")
            
            val config = serverConfigService.getOrCreateConfig()
            
            val response = TokenExchangeResponse(
                serverToken = config.serverToken,
                serverId = config.serverId,
                expiresAt = null  // Token does not expire
            )
            
            Logger.i("[TokenExchangeRoutes] Token exchange successful for client: ${request.clientId}, user: ${user.email}")
            call.respond(response)
        } catch (e: Exception) {
            Logger.e("[TokenExchangeRoutes] Token exchange failed", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Token exchange failed: ${e.message}"))
        }
    }
}