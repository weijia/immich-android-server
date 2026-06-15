package com.immich.server.model

import kotlinx.serialization.Serializable

@Serializable
data class AssetResponse(
    val id: String,
    val ownerId: String,
    val deviceAssetId: String,
    val deviceId: String,
    val type: String, // IMAGE or VIDEO
    val originalPath: String,
    val thumbhash: String? = null,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
    val createdAt: String,
    val updatedAt: String,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val originalFileName: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0,
    val width: Int? = null,
    val height: Int? = null,
    val duration: String? = null,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val checksum: String? = null,
    val livePhotoVideoId: String? = null
)

@Serializable
data class AssetUploadRequest(
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
    val isFavorite: Boolean = false,
    val duration: String? = null
)

@Serializable
data class AssetBulkUploadCheck(
    val deviceAssetIds: List<String>,
    val deviceId: String
)

@Serializable
data class AssetBulkUploadResponse(
    val results: List<AssetExistence>
)

@Serializable
data class AssetExistence(
    val id: String,
    val deviceAssetId: String,
    val exists: Boolean
)

@Serializable
data class AssetUploadResponse(
    val id: String,
    val duplicate: Boolean = false
)
