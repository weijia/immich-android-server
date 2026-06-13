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