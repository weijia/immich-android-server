package com.immich.server.service

import com.immich.server.db.ImmichDatabase
import com.immich.server.model.LoginResponse
import com.immich.server.model.UserResponse
import kotlinx.datetime.Clock

class AuthService(private val database: ImmichDatabase) {

    fun login(email: String, password: String): LoginResponse? {
        val user = database.userQueries.selectByEmail(email).executeAsOneOrNull() ?: return null
        // TODO: Proper password hashing (bcrypt)
        if (user.password_hash != password) return null

        return LoginResponse(
            accessToken = generateToken(user.id),
            userId = user.id,
            userEmail = user.email,
            name = user.name,
            isAdmin = user.is_admin == 1L
        )
    }

    fun createAdmin(email: String, password: String): LoginResponse? {
        if (hasAdmin()) return null

        val id = generateId()
        val now = Clock.System.now().epochSeconds
        database.userQueries.insert(
            id = id,
            email = email,
            name = "Admin",
            password_hash = password, // TODO: Hash password
            created_at = now,
            updated_at = now,
            is_admin = 1,
            storage_quota = 0,
            avatar_color = "#4285F4"
        )

        return LoginResponse(
            accessToken = generateToken(id),
            userId = id,
            userEmail = email,
            name = "Admin",
            isAdmin = true
        )
    }

    fun hasAdmin(): Boolean {
        return database.userQueries.selectAll().executeAsList().isNotEmpty()
    }

    fun getUserById(id: String): UserResponse? {
        val user = database.userQueries.selectById(id).executeAsOneOrNull() ?: return null
        return UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            avatarColor = user.avatar_color,
            storageQuota = user.storage_quota
        )
    }

    private fun generateToken(userId: String): String {
        // TODO: JWT implementation
        return "token_${userId}_${Clock.System.now().epochSeconds}"
    }

    private fun generateId(): String {
        return Clock.System.now().epochSeconds.toString() + (0..9999).random()
    }
}
