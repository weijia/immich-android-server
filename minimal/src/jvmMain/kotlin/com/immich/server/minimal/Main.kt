package com.immich.server.minimal

import com.immich.server.ImmichServer
import com.immich.server.platform.PlatformDatabaseDriverFactory
import com.immich.server.platform.PlatformFileStorage
import com.immich.server.platform.PlatformNotification

fun main() {
    println("Immich Server - Minimal Edition")
    println("================================")

    val driverFactory = PlatformDatabaseDriverFactory()
    val fileStorage = PlatformFileStorage()
    val notification = PlatformNotification()

    val server = ImmichServer(driverFactory, fileStorage, notification, port = 2283)

    println("Starting server on port 2283...")
    server.start()

    println("Server is running!")
    println("Connect with Immich app at http://localhost:2283")
    println()
    println("Press Enter to stop the server...")

    readLine()

    println("Stopping server...")
    server.stop()
    println("Server stopped.")
}
