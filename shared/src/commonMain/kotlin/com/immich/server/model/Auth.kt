package com.immich.server.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val userId: String,
    val userEmail: String,
    val name: String,
    val isAdmin: Boolean = false,
    val isOnboarded: Boolean = true,  // 客户端期望这个字段
    val profileImagePath: String = "",
    val shouldChangePassword: Boolean = false
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val avatarColor: String = "#4285F4",
    val storageQuota: Long = 0
)

@Serializable
data class UserAdminResponse(
    val id: String,  // UUID format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
    val email: String,
    val name: String,
    val avatarColor: String = "blue",  // Enum: primary, pink, red, yellow, blue, green, purple, orange, gray, amber
    val createdAt: String = "2024-01-01T00:00:00.000Z",  // ISO 8601 date-time
    val deletedAt: String? = null,  // ISO 8601 date-time, nullable
    val isAdmin: Boolean = false,
    val license: UserLicense? = null,  // Object, not String
    val oauthId: String = "",
    val profileChangedAt: String? = null,  // ISO 8601 date-time, not Long
    val profileImagePath: String = "",
    val quotaSizeInBytes: Long? = null,
    val quotaUsageInBytes: Long? = null,
    val shouldChangePassword: Boolean = false,
    val status: String = "active",  // Enum: active, removing, deleted
    val storageLabel: String? = null,
    val updatedAt: String = "2024-01-01T00:00:00.000Z"  // ISO 8601 date-time
)

@Serializable
data class UserLicense(
    val activatedAt: String? = null,  // ISO 8601 date-time
    val licenseKey: String? = null,
    val startAt: String? = null,  // ISO 8601 date-time
    val expiresAt: String? = null  // ISO 8601 date-time
)

@Serializable
data class ErrorResponse(
    val message: String
)

@Serializable
data class SuccessResponse(
    val successful: Boolean = true
)

@Serializable
data class AdminCheckResponse(
    val isAdmin: Boolean = false
)