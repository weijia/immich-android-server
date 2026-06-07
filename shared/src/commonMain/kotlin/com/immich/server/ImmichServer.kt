package com.immich.server

import com.immich.server.api.authRoutes
import com.immich.server.api.getPlatformVersion
import com.immich.server.api.serverInfoRoutes
import com.immich.server.db.ImmichDatabase
import com.immich.server.platform.PlatformDatabaseDriverFactory
import com.immich.server.platform.PlatformFileStorage
import com.immich.server.platform.PlatformNotification
import com.immich.server.service.AuthService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

/**
 * Cross-platform Immich-compatible server
 */
class ImmichServer(
    private val driverFactory: PlatformDatabaseDriverFactory,
    private val fileStorage: PlatformFileStorage,
    private val notification: PlatformNotification,
    private val port: Int = 2283
) {
    private var server: io.ktor.server.engine.ApplicationEngine? = null
    private lateinit var database: ImmichDatabase
    private lateinit var authService: AuthService

    fun start() {
        // Initialize database
        database = ImmichDatabase(driverFactory.createDriver())
        authService = AuthService(database)

        // Create notification channel (if supported)
        notification.createNotificationChannel()
        notification.showNotification("Immich Server", "Server starting on port $port")

        server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            install(CORS) {
                anyHost()
                allowHeader("Authorization")
                allowHeader("Content-Type")
                allowHeader("x-api-key")
            }

            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    call.respondText("Error: ${cause.localizedMessage}")
                }
            }

            routing {
                route("/api") {
                    serverInfoRoutes()
                    authRoutes(authService)
                    // TODO: Add assets, albums routes
                }
            }
        }.start(wait = false)

        notification.showNotification("Immich Server", "Running on port $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
        notification.cancelNotification()
    }

    fun isRunning(): Boolean = server != null

    fun getDatabase(): ImmichDatabase = database
}
