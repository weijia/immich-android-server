package com.immich.server.android

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 版本信息（由 GitHub Actions 构建时注入 versionName 和 versionCode）
 * 通过 BuildConfig 直接读取 Gradle 构建时设置的版本信息
 */
object BuildInfo {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 版本号（如 1.1.0 或 0.0.0-20260609.143000CST）
     * 直接从 BuildConfig 读取，避免 PackageManager API 兼容性问题
     */
    val versionName: String
        get() = try {
            BuildConfig.VERSION_NAME ?: "0.0.0"
        } catch (e: Exception) {
            // Fallback to PackageManager if BuildConfig fails
            readVersionFromPackageManager() ?: "0.0.0"
        }

    /**
     * 版本代码
     */
    val versionCode: Int
        get() = try {
            BuildConfig.VERSION_CODE
        } catch (e: Exception) {
            -1
        }

    private fun readVersionFromPackageManager(): String? {
        val ctx = appContext ?: return null
        return try {
            val pm = ctx.packageManager
            val pkgName = ctx.packageName
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkgName, 0)
            }
            info.versionName
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 构建类型（tag 或 datetime）
     * 从 versionName 推断
     */
    val buildType: String
        get() {
            val name = versionName
            return if (name.contains("-")) {
                val parts = name.split("-")
                if (parts.size > 1 && parts[1].length >= 15) "datetime" else "tag"
            } else {
                "tag"
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
            val dashIndex = name.indexOf('-')
            return if (dashIndex >= 0) {
                val base = name.substring(0, dashIndex)
                val timestamp = name.substring(dashIndex + 1)
                val cleanTimestamp = timestamp.removeSuffix("CST")
                val formatted = if (cleanTimestamp.length >= 15) {
                    "${cleanTimestamp.substring(0, 4)}-${cleanTimestamp.substring(4, 6)}-${cleanTimestamp.substring(6, 8)} " +
                    "${cleanTimestamp.substring(9, 11)}:${cleanTimestamp.substring(11, 13)}"
                } else {
                    cleanTimestamp
                }
                "$base ($formatted CST)"
            } else {
                "v$name"
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
