package com.immich.server.api

import com.immich.server.model.PublicFeatures
import com.immich.server.model.ServerInfoResponse
import com.immich.server.model.SupportedMediaTypes
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Route.serverInfoRoutes() {
    get("/server-info") {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val response = ServerInfoResponse(
            serverTime = now.toString(),
            operatingSystemVersion = getPlatformVersion()
        )
        call.respond(response)
    }

    get("/server-info/ping") {
        call.respond(mapOf("res" to "pong"))
    }

    get("/server-info/version") {
        call.respond(mapOf(
            "version" to "1.108.0",
            "versionUrl" to "https://github.com/immich-app/immich/releases/tag/v1.108.0"
        ))
    }

    get("/server-info/features") {
        call.respond(PublicFeatures())
    }
}

// Platform-specific version info
expect fun getPlatformVersion(): String
