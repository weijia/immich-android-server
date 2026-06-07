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
    val isAdmin: Boolean = false
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val avatarColor: String = "#4285F4",
    val storageQuota: Long = 0
)
