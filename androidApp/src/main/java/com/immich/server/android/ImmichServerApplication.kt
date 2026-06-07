package com.immich.server.android

import android.app.Application
import com.immich.server.ImmichServer
import com.immich.server.platform.PlatformDatabaseDriverFactory
import com.immich.server.platform.PlatformFileStorage
import com.immich.server.platform.PlatformNotification

class ImmichServerApplication : Application() {

    lateinit var server: ImmichServer
        private set

    override fun onCreate() {
        super.onCreate()

        val driverFactory = PlatformDatabaseDriverFactory(this)
        val fileStorage = PlatformFileStorage(this)
        val notification = PlatformNotification(this)

        server = ImmichServer(driverFactory, fileStorage, notification, port = 2283)
    }

    companion object {
        lateinit var instance: ImmichServerApplication
            private set
    }
}
