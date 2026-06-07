package com.immich.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfoResponse(
    val version: String = "1.108.0",
    val versionUrl: String = "https://github.com/immich-app/immich/releases/tag/v1.108.0",
    val newVersion: String? = null,
    val repositoryUrl: String = "https://github.com/immich-app/immich",
    val publicFeatures: PublicFeatures = PublicFeatures(),
    val serverTime: String = "",
    val operatingSystem: String = "cross-platform",
    val operatingSystemVersion: String = "",
    val supportedMediaTypes: SupportedMediaTypes = SupportedMediaTypes()
)

@Serializable
data class PublicFeatures(
    val oauthEnabled: Boolean = false,
    val oauthAutoLaunch: Boolean = false,
    val passwordLoginEnabled: Boolean = true,
    val configFile: Boolean = false,
    val trashDays: Int = 30,
    val peopleEnabled: Boolean = false,
    val tagsEnabled: Boolean = false,
    val foldersEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val email: Boolean = false,
    val sidecar: Boolean = true,
    val search: Boolean = false,
    val smartSearch: Boolean = false,
    val map: Boolean = false,
    val reverseGeocoding: Boolean = false,
    val memoriesEnabled: Boolean = false,
    val duplicateDetectionEnabled: Boolean = false,
    val facialRecognitionEnabled: Boolean = false,
    val license: Boolean = false,
    val externalLibrary: Boolean = false
)

@Serializable
data class SupportedMediaTypes(
    val image: List<String> = listOf(
        "image/jpeg", "image/jpg", "image/png", "image/gif",
        "image/webp", "image/heic", "image/heif", "image/dng",
        "image/x-adobe-dng", "image/avif", "image/jxl"
    ),
    val video: List<String> = listOf(
        "video/mp4", "video/webm", "video/ogg", "video/quicktime",
        "video/x-msvideo", "video/mpeg", "video/x-matroska"
    ),
    val sidecar: List<String> = listOf(
        "application/x-sidecar+xml"
    )
)
