package com.immich.server.discovery

import com.immich.server.platform.Logger
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

    suspend fun discover(): List<Any> = withContext(Dispatchers.IO) {
        Logger.i("[DiscoveryClient] Starting discovery on port $port, timeout=${timeoutMs}ms")

        val selector = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selector)
            .udp()
            .bind(InetSocketAddress("0.0.0.0", 0))

        val localAddress = socket.localAddress
        Logger.d("[DiscoveryClient] Bound to local address: $localAddress")

        val responses = mutableListOf<Any>()

        try {
            // Send discovery request (v1.0)
            val requestPacket = ByteReadPacket(DiscoveryProtocol.DISCOVER_REQUEST_V1.encodeToByteArray())
            val broadcastAddress = InetSocketAddress(DiscoveryProtocol.BROADCAST_ADDRESS, port)
            val requestDatagram = Datagram(requestPacket, broadcastAddress)

            Logger.i("[DiscoveryClient] Sending discovery broadcast to ${DiscoveryProtocol.BROADCAST_ADDRESS}:$port")
            Logger.d("[DiscoveryClient] Request payload: '${DiscoveryProtocol.DISCOVER_REQUEST_V1}'")

            socket.send(requestDatagram)
            Logger.i("[DiscoveryClient] Discovery broadcast sent successfully")

            // Wait for responses with timeout
            withTimeout(timeoutMs) {
                while (true) {
                    Logger.d("[DiscoveryClient] Waiting for response...")
                    val datagram = socket.receive()
                    val response = datagram.packet.readText()
                    val senderAddress = datagram.address as InetSocketAddress

                    Logger.d("[DiscoveryClient] Received packet from ${senderAddress.hostname}:${senderAddress.port}: '$response'")

                    DiscoveryProtocol.parseResponse(response)?.let {
                        responses.add(it)
                        val url = DiscoveryProtocol.getServerUrl(it)
                        val name = when (it) {
                            is DiscoveryProtocol.DiscoveryResponseV3 -> it.serverName
                            is DiscoveryProtocol.DiscoveryResponseV2 -> it.serverName
                            is DiscoveryProtocol.DiscoveryResponseV1 -> it.serverName
                            else -> "Unknown"
                        }
                        val version = when (it) {
                            is DiscoveryProtocol.DiscoveryResponseV3 -> it.version
                            is DiscoveryProtocol.DiscoveryResponseV2 -> it.version
                            is DiscoveryProtocol.DiscoveryResponseV1 -> it.version
                            else -> "Unknown"
                        }
                        Logger.i("[DiscoveryClient] Discovered server: $url (name=$name, version=$version)")
                    } ?: run {
                        Logger.w("[DiscoveryClient] Invalid response from ${senderAddress.hostname}:${senderAddress.port}: '$response'")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Logger.i("[DiscoveryClient] Discovery timeout reached, found ${responses.size} server(s)")
        } catch (e: Exception) {
            Logger.e("[DiscoveryClient] Discovery error", e)
        } finally {
            Logger.d("[DiscoveryClient] Closing socket and selector")
            socket.close()
            selector.close()
        }

        Logger.i("[DiscoveryClient] Discovery complete. Total servers found: ${responses.size}")
        responses.forEachIndexed { index, response ->
            val url = DiscoveryProtocol.getServerUrl(response)
            Logger.i("[DiscoveryClient] Server #${index + 1}: $url")
        }

        responses
    }
}