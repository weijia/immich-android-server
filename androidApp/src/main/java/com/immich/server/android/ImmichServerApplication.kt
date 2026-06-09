package com.immich.server.android

import android.app.Application
import android.util.Log
import com.immich.server.ImmichServer
import com.immich.server.platform.NetworkUtils
import com.immich.server.platform.PlatformDatabaseDriverFactory
import com.immich.server.platform.PlatformFileStorage
import com.immich.server.platform.PlatformNotification

class ImmichServerApplication : Application() {

    companion object {
        private const val TAG = "ImmichServerApp"
        lateinit var instance: ImmichServerApplication
            private set
    }

    lateinit var server: ImmichServer
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application onCreate")
        instance = this

        // Initialize network utilities for IP detection
        Log.d(TAG, "Initializing NetworkUtils")
        NetworkUtils.initialize(this)

        Log.d(TAG, "Creating platform components")
        val driverFactory = PlatformDatabaseDriverFactory(this)
        val fileStorage = PlatformFileStorage(this)
        val notification = PlatformNotification(this)

        Log.d(TAG, "Creating ImmichServer instance")
        server = ImmichServer(driverFactory, fileStorage, notification, port = 2283)
        Log.i(TAG, "Application initialized, server created (not started yet)")
    }
}
