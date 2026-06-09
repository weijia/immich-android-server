package com.immich.server.platform

import android.util.Log

actual object Logger {
    private const val TAG = "ImmichServer"

    actual fun d(message: String) {
        Log.d(TAG, message)
    }

    actual fun i(message: String) {
        Log.i(TAG, message)
    }

    actual fun w(message: String) {
        Log.w(TAG, message)
    }

    actual fun e(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}
