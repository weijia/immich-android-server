package com.immich.server.api

actual fun getPlatformVersion(): String {
    return "JVM ${System.getProperty("java.version")}"
}
