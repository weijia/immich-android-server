package com.immich.server.platform

/**
 * Platform-specific file storage abstraction
 * Implemented differently on Android, iOS, and JVM
 */
expect class PlatformFileStorage {
    fun saveFile(path: String, data: ByteArray)
    fun readFile(path: String): ByteArray?
    fun deleteFile(path: String)
    fun getStoragePath(): String
    fun fileExists(path: String): Boolean
    
    // Asset-specific methods
    fun saveAsset(assetId: String, filename: String, data: ByteArray): String  // Returns file path
    fun getAssetPath(assetId: String): String?
    fun deleteAsset(assetId: String): Boolean
    fun getAssetSize(assetId: String): Long
}
