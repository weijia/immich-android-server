package com.immich.server.android

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 版本信息（由 GitHub Actions 构建时注入 versionName 和 versionCode）
 * 通过 PackageManager 读取 APK 的版本信息
 */
object BuildInfo {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val pm: PackageManager?
        get() = appContext?.packageManager

    private val packageInfo: android.content.pm.PackageInfo?
        get() {
            val ctx = appContext ?: return null
            return try {
                @Suppress("DEPRECATION")
                pm?.getPackageInfo(ctx.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }

    /**
     * 版本号（如 1.1.0 或 0.0.0-20260609.143000CST）
     */
    val versionName: String
        get() = packageInfo?.versionName ?: "unknown"

    /**
     * 版本代码
     */
    val versionCode: Long
        get() {
            val info = packageInfo ?: return -1
            @Suppress("DEPRECATION")
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }

    /**
     * 构建类型（tag 或 datetime）
     * 从 versionName 推断
     */
    val buildType: String
        get() {
            val name = versionName
            // tag 构建：versionName 不含时间戳（如 1.1.0）
            // datetime 构建：versionName 含时间戳（如 0.0.0-20260609.143000CST）
            return if (name.contains(".")) {
                val parts = name.split("-")
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
            val name = versionName
            if (buildType == "tag") {
                return "v$name"
            }
            // datetime 构建，从 versionName 提取时间
            val dashIndex = name.indexOf('-')
            return if (dashIndex >= 0) {
                val base = name.substring(0, dashIndex)
                val timestamp = name.substring(dashIndex + 1)
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
                name
            }
        }

    /**
     * 完整信息字符串（用于日志和调试）
     */
    fun fullInfo(): String {
        return """
            Immich Android Server
            Version: $versionName ($versionCode)
            Build Type: $buildType
            Display: $display
            Android SDK: ${Build.VERSION.SDK_INT}
            Package: ${appContext?.packageName ?: "unknown"}
        """.trimIndent()
    }
}
