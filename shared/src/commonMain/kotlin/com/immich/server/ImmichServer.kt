package com.immich.server

import com.immich.server.api.authRoutes
import com.immich.server.api.getPlatformVersion
import com.immich.server.api.serverInfoRoutes
import com.immich.server.db.ImmichDatabase
import com.immich.server.discovery.DiscoveryServer
import com.immich.server.platform.Logger
import com.immich.server.platform.PlatformDatabaseDriverFactory
import com.immich.server.platform.PlatformFileStorage
import com.immich.server.platform.PlatformNotification
import com.immich.server.platform.NetworkUtils
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    private var discoveryServer: DiscoveryServer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var database: ImmichDatabase
    private lateinit var authService: AuthService

    fun start() {
        Logger.i("[ImmichServer] Starting server on port $port")

        // Initialize database
        Logger.d("[ImmichServer] Initializing database")
        database = ImmichDatabase(driverFactory.createDriver())
        authService = AuthService(database)
        Logger.i("[ImmichServer] Database initialized")

        // Create notification channel (if supported)
        Logger.d("[ImmichServer] Creating notification channel")
        notification.createNotificationChannel()
        notification.showNotification("Immich Server", "Server starting on port $port")

        // Start HTTP server
        Logger.i("[ImmichServer] Starting HTTP server on port $port")
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
                    Logger.e("[ImmichServer] HTTP error", cause)
                    call.respondText("Error: ${cause.localizedMessage}")
                }
            }

            routing {
                // Well-known endpoint (outside /api)
                get("/.well-known/immich") {
                    call.respond(mapOf("api" to mapOf("endpoint" to "/api")))
                }

                route("/api") {
                    serverInfoRoutes()
                    authRoutes(authService)
                    // TODO: Add assets, albums routes
                }
            }
        }.start(wait = false)
        Logger.i("[ImmichServer] HTTP server started on port $port")

        // Log server URL
        val serverUrl = getServerUrl()
        Logger.i("[ImmichServer] Server URL: $serverUrl")

        // Start discovery server (UDP broadcast)
        Logger.i("[ImmichServer] Starting discovery server")
        startDiscoveryServer()

        notification.showNotification("Immich Server", "Running on port $port")
        Logger.i("[ImmichServer] Server fully started and ready")
    }

    fun stop() {
        Logger.i("[ImmichServer] Stopping server")
        discoveryServer?.stop()
        server?.stop(1000, 2000)
        notification.cancelNotification()
        Logger.i("[ImmichServer] Server stopped")
    }

    fun isRunning(): Boolean {
        val running = server != null
        Logger.d("[ImmichServer] isRunning=$running")
        return running
    }

    fun getDatabase(): ImmichDatabase = database

    fun getServerUrl(): String {
        val url = NetworkUtils.getServerUrl(port)
        Logger.d("[ImmichServer] getServerUrl()=$url")
        return url
    }

    private fun startDiscoveryServer() {
        val serverUrl = getServerUrl()
        Logger.i("[ImmichServer] Starting discovery server with URL: $serverUrl")

        discoveryServer = DiscoveryServer(
            getServerUrl = { getServerUrl() }
        )
        scope.launch {
            try {
                discoveryServer?.start()
            } catch (e: Exception) {
                Logger.e("[ImmichServer] Discovery server failed to start", e)
            }
        }
        Logger.i("[ImmichServer] Discovery server launched")
    }
}
