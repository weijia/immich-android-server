package com.immich.server.platform

import java.io.File

actual class PlatformFileStorage {
    private val baseDir = File(System.getProperty("user.home"), ".immich-server")
    private val uploadDir = File(baseDir, "uploads")

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
    
    actual fun saveAsset(assetId: String, filename: String, data: ByteArray): String {
        uploadDir.mkdirs()
        val now = java.time.LocalDateTime.now()
        val yearMonthDir = File(uploadDir, "${now.year}/${now.monthValue.toString().padStart(2, '0')}")
        yearMonthDir.mkdirs()
        
        val file = File(yearMonthDir, "$assetId-$filename")
        file.writeBytes(data)
        return file.absolutePath
    }
    
    actual fun getAssetPath(assetId: String): String? {
        val files = uploadDir.walk().filter { it.name.startsWith(assetId) }.toList()
        return files.firstOrNull()?.absolutePath
    }
    
    actual fun deleteAsset(assetId: String): Boolean {
        val path = getAssetPath(assetId)
        if (path != null) {
            return File(path).delete()
        }
        return false
    }
    
    actual fun getAssetSize(assetId: String): Long {
        val path = getAssetPath(assetId)
        if (path != null) {
            return File(path).length()
        }
        return 0L
    }
}
