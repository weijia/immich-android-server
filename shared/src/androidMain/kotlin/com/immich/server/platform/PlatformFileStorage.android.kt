package com.immich.server.platform

import android.content.Context
import android.os.Environment
import java.io.File

actual class PlatformFileStorage(private val context: Context) {
    
    // Use external Documents directory for persistent storage
    // This ensures files survive app uninstall
    private val externalDocumentsDir: File by lazy {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Immich")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    // Upload directory for assets
    private val uploadDir: File by lazy {
        val dir = File(externalDocumentsDir, "uploads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    // Thumbs directory for thumbnails
    private val thumbsDir: File by lazy {
        val dir = File(externalDocumentsDir, "thumbs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    actual fun saveFile(path: String, data: ByteArray) {
        val file = File(externalDocumentsDir, path)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }
    
    actual fun readFile(path: String): ByteArray? {
        val file = File(externalDocumentsDir, path)
        return if (file.exists()) file.readBytes() else null
    }
    
    actual fun deleteFile(path: String) {
        val file = File(externalDocumentsDir, path)
        file.delete()
    }
    
    actual fun getStoragePath(): String {
        return externalDocumentsDir.absolutePath
    }
    
    actual fun fileExists(path: String): Boolean {
        return File(externalDocumentsDir, path).exists()
    }
    
    // Asset-specific methods
    fun getUploadPath(): String = uploadDir.absolutePath
    fun getThumbsPath(): String = thumbsDir.absolutePath
    
    actual fun saveAsset(assetId: String, filename: String, data: ByteArray): String {
        // Create year/month directory structure
        val now = java.time.LocalDateTime.now()
        val yearMonthDir = File(uploadDir, "${now.year}/${now.monthValue.toString().padStart(2, '0')}")
        yearMonthDir.mkdirs()
        
        val file = File(yearMonthDir, "$assetId-$filename")
        file.writeBytes(data)
        return file.absolutePath
    }
    
    actual fun getAssetPath(assetId: String): String? {
        // Search in upload directory for asset
        val files = uploadDir.walk().filter { it.name.startsWith(assetId) }.toList()
        return files.firstOrNull()?.absolutePath
    }
    
    // Helper method for internal use
    fun getAssetFile(assetId: String): File? {
        val files = uploadDir.walk().filter { it.name.startsWith(assetId) }.toList()
        return files.firstOrNull()
    }
    
    actual fun deleteAsset(assetId: String): Boolean {
        val file = getAssetFile(assetId)
        return file?.delete() ?: false
    }
    
    actual fun getAssetSize(assetId: String): Long {
        val file = getAssetFile(assetId)
        return file?.length() ?: 0
    }
}