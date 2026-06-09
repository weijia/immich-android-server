package com.immich.server.platform

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual object Logger {
    private const val TAG = "ImmichServer"
    private const val MAX_LOG_ENTRIES = 500

    data class LogEntry(
        val timestamp: String,
        val level: String,
        val message: String
    )

    private val _logs = mutableListOf<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    val logs: List<LogEntry> get() = synchronized(_logs) { _logs.toList() }

    private fun addLog(level: String, message: String) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            message = message
        )
        synchronized(_logs) {
            _logs.add(entry)
            if (_logs.size > MAX_LOG_ENTRIES) {
                _logs.removeAt(0)
            }
        }
    }

    fun clearLogs() {
        synchronized(_logs) { _logs.clear() }
    }

    actual fun d(message: String) {
        Log.d(TAG, message)
        addLog("D", message)
    }

    actual fun i(message: String) {
        Log.i(TAG, message)
        addLog("I", message)
    }

    actual fun w(message: String) {
        Log.w(TAG, message)
        addLog("W", message)
    }

    actual fun e(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
        val fullMessage = if (throwable != null) "$message: ${throwable.message}" else message
        addLog("E", fullMessage)
    }
}
