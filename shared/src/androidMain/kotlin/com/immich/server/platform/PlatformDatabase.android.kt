package com.immich.server.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.immich.server.db.ImmichDatabase

actual class PlatformDatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(ImmichDatabase.Schema, context, "immich.db")
    }
}
