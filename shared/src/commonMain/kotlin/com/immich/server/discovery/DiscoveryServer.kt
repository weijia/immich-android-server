package com.immich.server.discovery

import com.immich.server.platform.Logger
import com.immich.server.service.ServerConfigService
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * UDP Discovery Server
 * 
 * Supports protocol versions:
 * - v1.0: Basic discovery (DISCOVER_IMMICH_SERVER -> response)
 * - v2.0: Server ID matching (response includes serverId)
 * - v3.0: Token-based signing (request includes challenge nonce, response includes signature)
 * 
 * Listens for broadcast discovery requests and responds with server info.
 */
class DiscoveryServer(
    private val getServerUrl: () -> String,
    private val serverConfigService: ServerConfigService,
    private val port: Int = DiscoveryProtocol.DISCOVERY_PORT
) {
    private var socket: BoundDatagramSocket? = null
    private var selector: SelectorManager? = null

    suspend fun start() {
        Logger.i("[DiscoveryServer] Starting discovery server on UDP port $port")
        
        // Initialize server config (generate serverId and serverToken if needed)
        val config = serverConfigService.getOrCreateConfig()
        Logger.i("[DiscoveryServer] Server ID: ${config.serverId}")
        Logger.i("[DiscoveryServer] Server Name: ${config.serverName}")
        Logger.d("[DiscoveryServer] Server URL provider: ${getServerUrl()}")

        selector = SelectorManager(Dispatchers.IO)
        socket = aSocket(selector!!)
            .udp()
            .bind(InetSocketAddress("0.0.0.0", port))

        Logger.i("[DiscoveryServer] Discovery server started on UDP port $port")

        try {
            while (true) {
                Logger.d("[DiscoveryServer] Waiting for discovery request...")
                val datagram = socket!!.receive()
                val request = datagram.packet.readText()
                val clientAddress = datagram.address as InetSocketAddress

                Logger.d("[DiscoveryServer] Received packet from ${clientAddress.hostname}:${clientAddress.port}: '$request'")

                if (DiscoveryProtocol.isDiscoveryRequest(request)) {
                    Logger.i("[DiscoveryServer] Valid discovery request from ${clientAddress.hostname}:${clientAddress.port}")

                    val serverUrl = getServerUrl()
                    Logger.d("[DiscoveryServer] Server URL: $serverUrl")

                    // Create response with automatic version detection
                    val response = DiscoveryProtocol.createResponse(
                        request = request,
                        serverId = config.serverId,
                        serverName = config.serverName,
                        serverUrl = serverUrl,
                        signFunction = { url, timestamp, nonce ->
                            serverConfigService.signDiscoveryResponse(url, timestamp, nonce)
                        }
                    )
                    
                    Logger.d("[DiscoveryServer] Response payload: $response")

                    val responsePacket = ByteReadPacket(response.encodeToByteArray())
                    val responseDatagram = Datagram(responsePacket, clientAddress)

                    socket!!.send(responseDatagram)
                    Logger.i("[DiscoveryServer] Discovery response sent to ${clientAddress.hostname}:${clientAddress.port}: $serverUrl")
                } else {
                    Logger.w("[DiscoveryServer] Invalid request from ${clientAddress.hostname}:${clientAddress.port}: '$request'")
                }
            }
        } catch (e: Exception) {
            Logger.e("[DiscoveryServer] Discovery server error", e)
        }
    }

    fun stop() {
        Logger.i("[DiscoveryServer] Stopping discovery server")
        try {
            socket?.close()
            selector?.close()
            Logger.i("[DiscoveryServer] Discovery server stopped")
        } catch (e: Exception) {
            Logger.e("[DiscoveryServer] Error stopping discovery server", e)
        }
    }
}