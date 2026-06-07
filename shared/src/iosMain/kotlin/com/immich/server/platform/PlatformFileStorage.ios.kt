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
