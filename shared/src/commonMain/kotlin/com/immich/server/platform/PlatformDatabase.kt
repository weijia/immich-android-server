package com.immich.server.platform

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific database driver factory
 */
expect class PlatformDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
