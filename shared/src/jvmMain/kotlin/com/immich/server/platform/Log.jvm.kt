package com.immich.server.platform

actual object Logger {
    actual fun d(message: String) {
        println("[DEBUG] $message")
    }

    actual fun i(message: String) {
        println("[INFO] $message")
    }

    actual fun w(message: String) {
        println("[WARN] $message")
    }

    actual fun e(message: String, throwable: Throwable?) {
        println("[ERROR] $message")
        throwable?.printStackTrace()
    }
}
