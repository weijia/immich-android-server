package com.immich.server.api

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

/**
 * Well-known endpoints for Immich client discovery
 * These are registered at the root level (outside /api)
 */
@Serializable
data class WellKnownResponse(val api: WellKnownApi)

@Serializable
data class WellKnownApi(val endpoint: String)

fun Route.wellKnownRoutes() {
    get("/.well-known/immich") {
        call.respond(WellKnownResponse(api = WellKnownApi(endpoint = "/api")))
    }
}
