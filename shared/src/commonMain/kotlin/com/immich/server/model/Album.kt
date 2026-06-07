package com.immich.server.model

import kotlinx.serialization.Serializable

@Serializable
data class AlbumResponse(
    val id: String,
    val ownerId: String,
    val albumName: String,
    val description: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val albumThumbnailAssetId: String? = null,
    val shared: Boolean = false,
    val sharedUsers: List<UserResponse> = emptyList(),
    val assetCount: Int = 0,
    val assets: List<AssetResponse> = emptyList(),
    val isActivityEnabled: Boolean = true
)

@Serializable
data class CreateAlbumRequest(
    val albumName: String,
    val description: String? = null,
    val assetIds: List<String> = emptyList()
)

@Serializable
data class AddAssetsToAlbumRequest(
    val ids: List<String>
)
