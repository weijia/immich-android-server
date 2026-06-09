package com.immich.server.discovery

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.core.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * UDP Discovery Client
 * Sends broadcast discovery request and waits for server responses
 */
class DiscoveryClient(
    private val port: Int = DiscoveryProtocol.DISCOVERY_PORT,
    private val timeoutMs: Long = 5000
) {

    suspend fun discover(): List<DiscoveryProtocol.DiscoveryResponse> = withContext(Dispatchers.IO) {
        val selector = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selector)
            .udp()
            .bind(InetSocketAddress("0.0.0.0", 0))

        val responses = mutableListOf<DiscoveryProtocol.DiscoveryResponse>()

        try {
            // Note: broadcast is enabled by default on most systems

            // Send discovery request
            val requestPacket = ByteReadPacket(DiscoveryProtocol.DISCOVER_REQUEST.encodeToByteArray())
            val broadcastAddress = InetSocketAddress(DiscoveryProtocol.BROADCAST_ADDRESS, port)
            val requestDatagram = Datagram(requestPacket, broadcastAddress)

            socket.send(requestDatagram)
            println("Discovery broadcast sent to ${DiscoveryProtocol.BROADCAST_ADDRESS}:$port")

            // Wait for responses with timeout
            withTimeout(timeoutMs) {
                while (true) {
                    val datagram = socket.receive()
                    val response = datagram.packet.readText()

                    DiscoveryProtocol.parseResponse(response)?.let {
                        responses.add(it)
                        println("Discovered server: ${it.serverUrl}")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            // Expected - timeout reached
            println("Discovery timeout reached, found ${responses.size} server(s)")
        } catch (e: Exception) {
            println("Discovery error: ${e.message}")
        } finally {
            socket.close()
            selector.close()
        }

        responses
    }
}
