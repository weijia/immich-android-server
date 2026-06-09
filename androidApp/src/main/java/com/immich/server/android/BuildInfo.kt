package com.immich.server.android

import android.content.Context
import android.os.Build

/**
 * 版本信息（由 GitHub Actions 构建时注入 versionName 和 versionCode）
 * 通过 BuildConfig 读取 Gradle 传入的版本号
 */
object BuildInfo {

    /**
     * 版本号（如 1.1.0 或 0.0.0-20260609.143000CST）
     * 由 Gradle 的 -PversionName 参数传入
     */
    val versionName: String
        get() = BuildConfig.VERSION_NAME

    /**
     * 版本代码（如 1 或 20260609143000）
     * 由 Gradle 的 -PversionCode 参数传入
     */
    val versionCode: Int
        get() = BuildConfig.VERSION_CODE

    /**
     * 构建类型（tag 或 datetime）
     * 从 versionName 推断
     */
    val buildType: String
        get() {
            // tag 构建：versionName 不含时间戳（如 1.1.0）
            // datetime 构建：versionName 含时间戳（如 0.0.0-20260609.143000CST）
            return if (versionName.contains(".")) {
                val parts = versionName.split("-")
                if (parts.size > 1 && parts[1].length >= 15) "datetime" else "tag"
            } else {
                "unknown"
            }
        }

    /**
     * 格式化显示
     * tag 构建：v1.1.0
     * datetime 构建：0.0.0 (2026-06-09 14:30 CST)
     */
    val display: String
        get() {
            if (buildType == "tag") {
                return "v$versionName"
            }
            // datetime 构建，从 versionName 提取时间
            val dashIndex = versionName.indexOf('-')
            return if (dashIndex >= 0) {
                val base = versionName.substring(0, dashIndex)
                val timestamp = versionName.substring(dashIndex + 1)
                // timestamp 格式：20260609.143000CST
                val cleanTimestamp = timestamp.removeSuffix("CST")
                val formatted = if (cleanTimestamp.length >= 15) {
                    "${cleanTimestamp.substring(0, 4)}-${cleanTimestamp.substring(4, 6)}-${cleanTimestamp.substring(6, 8)} " +
                    "${cleanTimestamp.substring(9, 11)}:${cleanTimestamp.substring(11, 13)}"
                } else {
                    cleanTimestamp
                }
                "$base ($formatted CST)"
            } else {
                versionName
            }
        }

    /**
     * 完整信息字符串（用于日志和调试）
     */
    fun fullInfo(context: Context): String {
        return """
            Immich Android Server
            Version: $versionName ($versionCode)
            Build Type: $buildType
            Display: $display
            Android SDK: ${Build.VERSION.SDK_INT}
            Package: ${context.packageName}
        """.trimIndent()
    }
}
