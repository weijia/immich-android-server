package com.immich.server.service

import com.immich.server.db.ImmichDatabase
import com.immich.server.model.LoginResponse
import com.immich.server.model.UserResponse
import com.immich.server.platform.Logger
import kotlinx.datetime.Clock

class AuthService(private val database: ImmichDatabase) {

    fun login(email: String, password: String): LoginResponse? {
        Logger.d("[AuthService] login called: email=$email")
        
        val user = database.userQueries.selectByEmail(email).executeAsOneOrNull()
        
        if (user == null) {
            Logger.d("[AuthService] User not found: $email")
            
            // 第一个用户自动创建为 admin
            if (!hasAdmin()) {
                Logger.i("[AuthService] No users exist, creating first user as admin: $email")
                return createFirstUserAsAdmin(email, password)
            }
            
            Logger.w("[AuthService] Login failed: user not found and admin already exists")
            return null
        }
        
        Logger.d("[AuthService] User found: ${user.email}, isAdmin=${user.is_admin}")
        
        // TODO: Proper password hashing (bcrypt)
        if (user.password_hash != password) {
            Logger.w("[AuthService] Login failed: password mismatch for ${user.email}")
            return null
        }
        
        Logger.i("[AuthService] Login successful: ${user.email}")
        
        return LoginResponse(
            accessToken = generateToken(user.id),
            userId = user.id,
            userEmail = user.email,
            name = user.name,
            isAdmin = user.is_admin == 1L,
            isOnboarded = true,
            profileImagePath = "",
            shouldChangePassword = false
        )
    }

    /**
     * 创建第一个用户作为管理员
     */
    private fun createFirstUserAsAdmin(email: String, password: String): LoginResponse {
        val id = generateId()
        val now = Clock.System.now().epochSeconds
        
        // 从邮箱提取用户名
        val name = email.substringBefore("@").replace(".", " ").capitalize()
        
        database.userQueries.insert(
            id = id,
            email = email,
            name = name,
            password_hash = password, // TODO: Hash password
            created_at = now,
            updated_at = now,
            is_admin = 1,
            storage_quota = 0,
            avatar_color = "#4285F4"
        )
        
        Logger.i("[AuthService] First admin user created: id=$id, email=$email, name=$name")
        
        return LoginResponse(
            accessToken = generateToken(id),
            userId = id,
            userEmail = email,
            name = name,
            isAdmin = true,
            isOnboarded = true,
            profileImagePath = "",
            shouldChangePassword = false
        )
    }

    fun createAdmin(email: String, password: String): LoginResponse? {
        Logger.d("[AuthService] createAdmin called: email=$email")
        
        if (hasAdmin()) {
            Logger.w("[AuthService] createAdmin failed: admin already exists")
            return null
        }

        return createFirstUserAsAdmin(email, password)
    }

    fun hasAdmin(): Boolean {
        val count = database.userQueries.selectAll().executeAsList().size
        Logger.d("[AuthService] hasAdmin: user count=$count")
        return count > 0
    }

    fun getUserById(id: String): UserResponse? {
        Logger.d("[AuthService] getUserById: id=$id")
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
        val token = "token_${userId}_${Clock.System.now().epochSeconds}"
        Logger.d("[AuthService] Generated token for user: $userId")
        return token
    }

    private fun generateId(): String {
        return Clock.System.now().epochSeconds.toString() + (0..9999).random()
    }
}