package com.immich.server.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.immich.server.db.ImmichDatabase

actual class PlatformDatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:immich.db")
        ImmichDatabase.Schema.create(driver)
        return driver
    }
}
