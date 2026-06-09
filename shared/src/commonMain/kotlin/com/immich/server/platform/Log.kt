package com.immich.server.platform

expect object Logger {
    fun d(message: String)
    fun i(message: String)
    fun w(message: String)
    fun e(message: String, throwable: Throwable? = null)
}
