package com.immich.server.api

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * Well-known endpoints for Immich client discovery
 * These are registered at the root level (outside /api)
 */
fun Route.wellKnownRoutes() {
    // Client uses this to discover the API endpoint
    get("/.well-known/immich") {
        call.respond(mapOf("api" to mapOf("endpoint" to "/api")))
    }
}
