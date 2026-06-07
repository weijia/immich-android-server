package com.immich.server.discovery

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
        selector = SelectorManager(Dispatchers.IO)
        socket = aSocket(selector!!)
            .udp()
            .bind(InetSocketAddress("0.0.0.0", port))

        println("Discovery server started on UDP port $port")

        try {
            while (true) {
                val datagram = socket!!.receive()
                val request = datagram.packet.readText()

                if (DiscoveryProtocol.isDiscoveryRequest(request)) {
                    val clientAddress = datagram.address as InetSocketAddress
                    println("Discovery request from ${clientAddress.hostname}:${clientAddress.port}")

                    val response = DiscoveryProtocol.createResponse(getServerUrl())
                    val responsePacket = ByteReadPacket(response.encodeToByteArray())
                    val responseDatagram = Datagram(responsePacket, clientAddress)

                    socket!!.send(responseDatagram)
                    println("Discovery response sent: ${getServerUrl()}")
                }
            }
        } catch (e: Exception) {
            println("Discovery server error: ${e.message}")
        }
    }

    fun stop() {
        try {
            socket?.close()
            selector?.close()
            println("Discovery server stopped")
        } catch (e: Exception) {
            println("Error stopping discovery server: ${e.message}")
        }
    }
}
