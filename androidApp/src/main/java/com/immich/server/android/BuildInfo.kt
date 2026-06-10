package com.immich.server.android

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * 版本信息（由 GitHub Actions 构建时注入 versionName 和 versionCode）
 * 通过 PackageManager 读取 APK manifest 中的版本信息
 */
object BuildInfo {
    private const val TAG = "BuildInfo"

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "Initialized, package=${context.packageName}")
        Log.i(TAG, "Version: $versionName ($versionCode)")
    }

    /**
     * 版本号（如 1.1.0 或 0.0.0-20260609.143000CST）
     */
    val versionName: String
        get() {
            val ctx = appContext
            if (ctx == null) {
                Log.w(TAG, "appContext is null, returning default version")
                return "0.0.0"
            }
            return try {
                val pm = ctx.packageManager
                val pkgName = ctx.packageName
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkgName, 0)
                }
                val name = info.versionName
                Log.d(TAG, "PackageManager versionName=$name")
                if (name.isNullOrBlank() || name == "null") {
                    "0.0.0"
                } else {
                    name
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read version", e)
                "0.0.0"
            }
        }

    /**
     * 版本代码
     */
    val versionCode: Long
        get() {
            val ctx = appContext ?: return -1
            return try {
                val pm = ctx.packageManager
                val pkgName = ctx.packageName
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkgName, 0)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read versionCode", e)
                -1
            }
        }

    /**
     * 构建类型（tag 或 datetime）
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
