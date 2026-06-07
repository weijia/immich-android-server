package com.immich.server.platform

import android.content.Context
import java.io.File

actual class PlatformFileStorage(private val context: Context) {
    actual fun saveFile(path: String, data: ByteArray) {
        val file = File(context.filesDir, path)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    actual fun readFile(path: String): ByteArray? {
        val file = File(context.filesDir, path)
        return if (file.exists()) file.readBytes() else null
    }

    actual fun deleteFile(path: String) {
        val file = File(context.filesDir, path)
        file.delete()
    }

    actual fun getStoragePath(): String {
        return context.filesDir.absolutePath
    }

    actual fun fileExists(path: String): Boolean {
        return File(context.filesDir, path).exists()
    }
}
