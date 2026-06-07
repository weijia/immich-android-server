package com.immich.server.api

import platform.UIKit.UIDevice

actual fun getPlatformVersion(): String {
    return "iOS ${UIDevice.currentDevice.systemVersion}"
}
