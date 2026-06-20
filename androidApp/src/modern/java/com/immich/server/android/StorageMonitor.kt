package com.immich.server.android

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Storage status monitor
 * Tracks internal and external storage space
 */
class StorageMonitor(private val context: Context) {
    
    data class StorageState(
        val totalSpace: Long = 0,           // Total storage space in bytes
        val freeSpace: Long = 0,            // Free storage space in bytes
        val usedSpace: Long = 0,            // Used storage space in bytes
        val immichUsedSpace: Long = 0,      // Space used by Immich uploads in bytes
        val usedPercentage: Int = 0,        // Used percentage (0-100)
        val isLowSpace: Boolean = false,    // Less than 10% free
        val isCriticalSpace: Boolean = false, // Less than 5% free
        val storagePath: String = "",       // Storage path being monitored
        val estimatedPhotos: Int = 0,       // Estimated photos that can be stored
        val estimatedVideos: Int = 0        // Estimated videos that can be stored
    )
    
    private val _storageState = MutableStateFlow(StorageState())
    val storageState: StateFlow<StorageState> = _storageState.asStateFlow()
    
    // Immich storage directory
    private val immichDir: File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "Immich"
    )
    
    fun update() {
        // Get external storage stats (Documents directory is on external storage)
        val externalStorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        
        if (!externalStorage.exists()) {
            // Fallback to internal storage
            val internalStorage = context.filesDir
            updateStorageStats(internalStorage)
        } else {
            updateStorageStats(externalStorage)
        }
    }
    
    private fun updateStorageStats(storageDir: File) {
        val statFs = StatFs(storageDir.absolutePath)
        
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong
        
        val totalSpace = blockSize * totalBlocks
        val freeSpace = blockSize * availableBlocks
        val usedSpace = totalSpace - freeSpace
        
        val usedPercentage = if (totalSpace > 0) {
            ((usedSpace * 100) / totalSpace).toInt()
        } else 0
        
        // Calculate Immich directory size
        val immichUsedSpace = calculateDirectorySize(immichDir)
        
        // Low space warnings
        val freePercentage = 100 - usedPercentage
        val isLowSpace = freePercentage < 10
        val isCriticalSpace = freePercentage < 5
        
        // Estimate how many photos/videos can be stored
        // Average photo size: 5MB, Average video size: 100MB
        val avgPhotoSize = 5L * 1024 * 1024  // 5MB
        val avgVideoSize = 100L * 1024 * 1024  // 100MB
        
        val estimatedPhotos = if (freeSpace > 0) (freeSpace / avgPhotoSize).toInt() else 0
        val estimatedVideos = if (freeSpace > 0) (freeSpace / avgVideoSize).toInt() else 0
        
        _storageState.value = StorageState(
            totalSpace = totalSpace,
            freeSpace = freeSpace,
            usedSpace = usedSpace,
            immichUsedSpace = immichUsedSpace,
            usedPercentage = usedPercentage,
            isLowSpace = isLowSpace,
            isCriticalSpace = isCriticalSpace,
            storagePath = storageDir.absolutePath,
            estimatedPhotos = estimatedPhotos,
            estimatedVideos = estimatedVideos
        )
    }
    
    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        
        var totalSize = 0L
        directory.walk().forEach { file ->
            if (file.isFile) {
                totalSize += file.length()
            }
        }
        return totalSize
    }
    
    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val unitIndex = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val unitIndexClamped = unitIndex.coerceIn(0, units.length - 1)
        
        val size = bytes / Math.pow(1024.0, unitIndexClamped.toDouble())
        
        return when (unitIndexClamped) {
            0 -> "${bytes} ${units[0]}"
            1 -> "${(bytes / 1024)} ${units[1]}"
            2 -> "${(bytes / (1024 * 1024))} ${units[2]}"
            3 -> String.format("%.1f %s", size, units[3])
            4 -> String.format("%.2f %s", size, units[4])
            else -> String.format("%.1f %s", size, units[unitIndexClamped])
        }
    }
    
    fun getSpaceWarning(state: StorageState): String {
        if (state.isCriticalSpace) {
            return "⚠️ 存储空间严重不足！仅剩 ${formatSize(state.freeSpace)}，请立即清理"
        }
        if (state.isLowSpace) {
            return "⚠️ 存储空间不足，仅剩 ${formatSize(state.freeSpace)}，建议清理"
        }
        return ""
    }
    
    fun getStorageRecommendation(state: StorageState): String {
        if (state.freeSpace <= 0) {
            return "存储已满，无法同步更多数据"
        }
        
        val photosStr = if (state.estimatedPhotos > 1000) {
            "${state.estimatedPhotos / 1000}k 张"
        } else {
            "${state.estimatedPhotos} 张"
        }
        
        val videosStr = if (state.estimatedVideos > 100) {
            "${state.estimatedVideos / 100 * 100}+ 个"
        } else {
            "${state.estimatedVideos} 个"
        }
        
        return "预计可同步约 $photosStr 照片或 $videosStr 视频"
    }
}