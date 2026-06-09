package com.immich.server.discovery

import com.immich.server.platform.Logger
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.core.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UDP Discovery Server
 * Listens for broadcast discovery requests and responds with server info
 */
class DiscoveryServer(
    private val getServerUrl: () -> String,
    private val port: Int = DiscoveryProtocol.DISCOVERY_PORT
) {
    private var socket: BoundDatagramSocket? = null
    private var selector: SelectorManager? = null

    suspend fun start() {
        Logger.i("[DiscoveryServer] Starting discovery server on UDP port $port")
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

                    val response = DiscoveryProtocol.createResponse(serverUrl)
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
