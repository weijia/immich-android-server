package com.immich.server.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.immich.server.db.ImmichDatabase

actual class PlatformDatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(ImmichDatabase.Schema, "immich.db")
    }
}
