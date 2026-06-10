package com.immich.server.api

import com.immich.server.model.PublicFeatures
import com.immich.server.model.ServerInfoResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

// --- Response DTOs ---

@Serializable
data class PingResponse(val res: String)

@Serializable
data class ServerVersionResponse(val major: Int, val minor: Int, val patch: Int)

@Serializable
data class ServerConfigResponse(
    val oauthButtonText: String = "",
    val isInitialized: Boolean = true,
    val isOnboarded: Boolean = true
)

// --- Routes ---

fun Route.serverInfoRoutes() {
    // Immich client compatibility endpoints
    // Note: /.well-known/immich is registered at root level in ImmichServer.kt

    // 1. /api/server/ping — Client pings this to check server availability
    get("/server/ping") {
        call.respond(PingResponse(res = "pong"))
    }

    // 2. /api/server/version — Client checks version compatibility
    //    major must match client's major version (currently 3.x)
    get("/server/version") {
        call.respond(ServerVersionResponse(major = 3, minor = 0, patch = 0))
    }

    // 3. /api/server/features — Client checks feature flags
    get("/server/features") {
        call.respond(PublicFeatures())
    }

    // 4. /api/server/config — Client checks server config
    get("/server/config") {
        call.respond(ServerConfigResponse())
    }

    // Legacy endpoints (for backward compatibility)
    get("/server-info") {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val response = ServerInfoResponse(
            serverTime = now.toString(),
            operatingSystemVersion = getPlatformVersion()
        )
        call.respond(response)
    }

    get("/server-info/ping") {
        call.respond(PingResponse(res = "pong"))
    }

    get("/server-info/version") {
        call.respond(ServerVersionResponse(major = 3, minor = 0, patch = 0))
    }

    get("/server-info/features") {
        call.respond(PublicFeatures())
    }
}

// Platform-specific version info
expect fun getPlatformVersion(): String
