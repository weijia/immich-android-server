package com.immich.server.platform

import java.io.File

actual class PlatformFileStorage {
    private val baseDir = File(System.getProperty("user.home"), ".immich-server")

    actual fun saveFile(path: String, data: ByteArray) {
        val file = File(baseDir, path)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    actual fun readFile(path: String): ByteArray? {
        val file = File(baseDir, path)
        return if (file.exists()) file.readBytes() else null
    }

    actual fun deleteFile(path: String) {
        val file = File(baseDir, path)
        file.delete()
    }

    actual fun getStoragePath(): String {
        return baseDir.absolutePath
    }

    actual fun fileExists(path: String): Boolean {
        return File(baseDir, path).exists()
    }
}
