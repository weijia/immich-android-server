package com.immich.server.platform

import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

actual class PlatformFileStorage {
    private val fileManager = NSFileManager.defaultManager

    private fun getDocumentsDirectory(): String {
        val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        return (urls.first() as platform.Foundation.NSURL).path ?: NSHomeDirectory()
    }
    
    private fun getUploadDirectory(): String {
        val uploadDir = "${getDocumentsDirectory()}/Immich/uploads"
        fileManager.createDirectoryAtPath(uploadDir, true, null, null)
        return uploadDir
    }

    actual fun saveFile(path: String, data: ByteArray) {
        val fullPath = "${getDocumentsDirectory()}/$path"
        val nsData = data.toNSData()
        nsData.writeToFile(fullPath, atomically = true)
    }

    actual fun readFile(path: String): ByteArray? {
        val fullPath = "${getDocumentsDirectory()}/$path"
        val nsData = NSData.dataWithContentsOfFile(fullPath)
        return nsData?.toByteArray()
    }

    actual fun deleteFile(path: String) {
        val fullPath = "${getDocumentsDirectory()}/$path"
        fileManager.removeItemAtPath(fullPath, null)
    }

    actual fun getStoragePath(): String {
        return getDocumentsDirectory()
    }

    actual fun fileExists(path: String): Boolean {
        val fullPath = "${getDocumentsDirectory()}/$path"
        return fileManager.fileExistsAtPath(fullPath)
    }
    
    actual fun saveAsset(assetId: String, filename: String, data: ByteArray): String {
        val uploadDir = getUploadDirectory()
        val now = kotlinx.datetime.Clock.System.now()
        val localDateTime = kotlinx.datetime.LocalDateTime.parse(now.toString())
        val yearMonthDir = "$uploadDir/${localDateTime.year}/${localDateTime.monthNumber.toString().padStart(2, '0')}"
        fileManager.createDirectoryAtPath(yearMonthDir, true, null, null)
        
        val fullPath = "$yearMonthDir/$assetId-$filename"
        val nsData = data.toNSData()
        nsData.writeToFile(fullPath, atomically = true)
        
        return fullPath
    }
    
    actual fun getAssetPath(assetId: String): String? {
        val uploadDir = getUploadDirectory()
        // Recursively search for asset
        val contents = fileManager.contentsOfDirectoryAtPath(uploadDir, null) ?: return null
        
        for (item in contents) {
            val itemName = (item as String)
            if (itemName.startsWith(assetId)) {
                return "$uploadDir/$itemName"
            }
        }
        return null
    }
    
    actual fun deleteAsset(assetId: String): Boolean {
        val path = getAssetPath(assetId)
        if (path != null) {
            fileManager.removeItemAtPath(path, null)
            return true
        }
        return false
    }
    
    actual fun getAssetSize(assetId: String): Long {
        val path = getAssetPath(assetId)
        if (path != null) {
            val attributes = fileManager.attributesOfItemAtPath(path, null)
            return (attributes?.get("NSFileSize") as? Long) ?: 0L
        }
        return 0L
    }
}

// Extension functions for ByteArray <-> NSData conversion
private fun ByteArray.toNSData(): NSData {
    return this.usePinned {
        NSData.dataWithBytes(it.addressOf(0), this.size.toULong())
    }
}

private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    return ByteArray(size).apply {
        usePinned {
            memcpy(it.addressOf(0), this@toByteArray.bytes, size.toULong())
        }
    }
}
