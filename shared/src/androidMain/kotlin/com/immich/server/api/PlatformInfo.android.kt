package com.immich.server.api

import android.os.Build

actual fun getPlatformVersion(): String {
    return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}
