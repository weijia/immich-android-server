package com.immich.server.api

import com.immich.server.model.AssetResponse
import com.immich.server.model.ErrorResponse
import com.immich.server.platform.Logger
import com.immich.server.platform.PlatformFileStorage
import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.PartData
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Asset Routes
 * 
 * Implements asset upload and management APIs.
 * 
 * POST /api/assets - Upload asset (multipart)
 * GET /api/assets/:id - Get asset info
 * GET /api/assets/:id/original - Download original file
 * GET /api/assets/:id/thumbnail - Download thumbnail
 * DELETE /api/assets/:id - Delete asset
 */
fun Route.assetRoutes(fileStorage: PlatformFileStorage) {
    route("/assets") {
        // POST /api/assets - Upload asset
        post {
            Logger.i("[AssetRoutes] POST /assets received")
            
            try {
                val multipart = call.receiveMultipart()
                var deviceAssetId: String? = null
                var deviceId: String? = null
                var fileCreatedAt: String? = null
                var fileModifiedAt: String? = null
                var isFavorite: Boolean = false
                var duration: String? = null
                var filename: String? = null
                var fileData: ByteArray? = null
                
                // Parse multipart parts using forEachPart
                multipart.forEachPart { part ->
                    when (part.name) {
                        "deviceAssetId" -> {
                            if (part is PartData.FormItem) {
                                deviceAssetId = part.value
                            }
                        }
                        "deviceId" -> {
                            if (part is PartData.FormItem) {
                                deviceId = part.value
                            }
                        }
                        "fileCreatedAt" -> {
                            if (part is PartData.FormItem) {
                                fileCreatedAt = part.value
                            }
                        }
                        "fileModifiedAt" -> {
                            if (part is PartData.FormItem) {
                                fileModifiedAt = part.value
                            }
                        }
                        "isFavorite" -> {
                            if (part is PartData.FormItem) {
                                isFavorite = part.value.toBoolean()
                            }
                        }
                        "duration" -> {
                            if (part is PartData.FormItem) {
                                duration = part.value
                            }
                        }
                        "assetData" -> {
                            if (part is PartData.FileItem) {
                                filename = part.originalFileName ?: "unknown"
                                fileData = part.streamProvider().readBytes()
                            }
                        }
                    }
                    // Dispose part after processing
                    part.dispose()
                }
                
                // Validate required fields
                if (deviceAssetId == null || deviceId == null || fileData == null) {
                    Logger.w("[AssetRoutes] Missing required fields")
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(message = "Missing required fields"))
                    return@post
                }
                
                // Generate asset ID
                val assetId = UUID.randomUUID().toString()
                
                // Save file to external storage
                val savedPath = fileStorage.saveAsset(assetId, filename ?: "unknown", fileData!!)
                Logger.i("[AssetRoutes] Saved asset to: $savedPath")
                
                // Determine asset type
                val extension = savedPath.substringAfterLast(".")
                val mimeType = when (extension.lowercase()) {
                    "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "raw", "dng" -> "image/${extension.lowercase()}"
                    "mp4", "mov", "avi", "mkv", "webm" -> "video/${extension.lowercase()}"
                    else -> "application/octet-stream"
                }
                val assetType = if (mimeType.startsWith("image")) "IMAGE" else "VIDEO"
                
                // Build response
                val response = AssetResponse(
                    id = assetId,
                    ownerId = "admin-user-id",  // TODO: Get from auth token
                    deviceAssetId = deviceAssetId!!,
                    deviceId = deviceId!!,
                    type = assetType,
                    originalPath = savedPath,
                    thumbhash = null,
                    fileCreatedAt = fileCreatedAt ?: java.time.LocalDateTime.now().toString(),
                    fileModifiedAt = fileModifiedAt ?: java.time.LocalDateTime.now().toString(),
                    createdAt = java.time.LocalDateTime.now().toString(),
                    updatedAt = java.time.LocalDateTime.now().toString(),
                    isFavorite = isFavorite,
                    isArchived = false,
                    isTrashed = false,
                    originalFileName = filename ?: "unknown",
                    mimeType = mimeType,
                    fileSize = fileData!!.size.toLong(),
                    width = null,
                    height = null,
                    duration = duration,
                    description = null,
                    latitude = null,
                    longitude = null,
                    checksum = null,
                    livePhotoVideoId = null
                )
                
                Logger.i("[AssetRoutes] Asset uploaded successfully: $assetId")
                call.respond(HttpStatusCode.Created, response)
                
            } catch (e: Exception) {
                Logger.e("[AssetRoutes] Upload error", e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(message = "Upload failed: ${e.message}"))
            }
        }
        
        // GET /api/assets/:id - Get asset info
        get("/{id}") {
            val assetId = call.parameters["id"]
            Logger.i("[AssetRoutes] GET /assets/$assetId received")
            
            if (assetId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(message = "Missing asset ID"))
                return@get
            }
            
            val assetPath = fileStorage.getAssetPath(assetId)
            if (assetPath == null) {
                Logger.w("[AssetRoutes] Asset not found: $assetId")
                call.respond(HttpStatusCode.NotFound, ErrorResponse(message = "Asset not found"))
                return@get
            }
            
            val extension = assetPath.substringAfterLast(".")
            val mimeType = when (extension.lowercase()) {
                "jpg", "jpeg", "png", "gif", "webp", "heic", "heif" -> "image/${extension.lowercase()}"
                "mp4", "mov", "avi", "mkv", "webm" -> "video/${extension.lowercase()}"
                else -> "application/octet-stream"
            }
            
            val filename = assetPath.substringAfterLast("/")
            
            val response = AssetResponse(
                id = assetId,
                ownerId = "admin-user-id",
                deviceAssetId = "unknown",
                deviceId = "unknown",
                type = if (mimeType.startsWith("image")) "IMAGE" else "VIDEO",
                originalPath = assetPath,
                thumbhash = null,
                fileCreatedAt = java.time.LocalDateTime.now().toString(),
                fileModifiedAt = java.time.LocalDateTime.now().toString(),
                createdAt = java.time.LocalDateTime.now().toString(),
                updatedAt = java.time.LocalDateTime.now().toString(),
                isFavorite = false,
                isArchived = false,
                isTrashed = false,
                originalFileName = filename,
                mimeType = mimeType,
                fileSize = fileStorage.getAssetSize(assetId),
                width = null,
                height = null,
                duration = null,
                description = null,
                latitude = null,
                longitude = null,
                checksum = null,
                livePhotoVideoId = null
            )
            
            call.respond(response)
        }
        
        // GET /api/assets/:id/original - Download original file
        get("/{id}/original") {
            val assetId = call.parameters["id"]
            Logger.i("[AssetRoutes] GET /assets/$assetId/original received")
            
            if (assetId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(message = "Missing asset ID"))
                return@get
            }
            
            val assetPath = fileStorage.getAssetPath(assetId)
            if (assetPath == null) {
                Logger.w("[AssetRoutes] Asset not found: $assetId")
                call.respond(HttpStatusCode.NotFound, ErrorResponse(message = "Asset not found"))
                return@get
            }
            
            val fileData = fileStorage.readFile(assetPath.substringAfter(fileStorage.getStoragePath() + "/"))
            if (fileData == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse(message = "File not found"))
                return@get
            }
            
            val extension = assetPath.substringAfterLast(".")
            val contentType = when (extension.lowercase()) {
                "jpg", "jpeg" -> ContentType.Image.JPEG
                "png" -> ContentType.Image.PNG
                "gif" -> ContentType.Image.GIF
                "mp4" -> ContentType.Video.MP4
                "mov" -> ContentType.Video.QuickTime
                else -> ContentType.Application.OctetStream
            }
            
            call.respondBytes(fileData, contentType)
        }
        
        // GET /api/assets/:id/thumbnail - Download thumbnail (stub)
        get("/{id}/thumbnail") {
            val assetId = call.parameters["id"]
            Logger.i("[AssetRoutes] GET /assets/$assetId/thumbnail received")
            
            // For now, return the original file as thumbnail
            // TODO: Generate actual thumbnails
            val assetPath = fileStorage.getAssetPath(assetId ?: "")
            if (assetPath == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse(message = "Asset not found"))
                return@get
            }
            
            val fileData = fileStorage.readFile(assetPath.substringAfter(fileStorage.getStoragePath() + "/"))
            if (fileData == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse(message = "File not found"))
                return@get
            }
            
            call.respondBytes(fileData, ContentType.Image.JPEG)
        }
        
        // DELETE /api/assets/:id - Delete asset (via GET for simplicity)
        get("/{id}/delete") {
            val assetId = call.parameters["id"]
            Logger.i("[AssetRoutes] DELETE /assets/$assetId received (via GET)")
            
            if (assetId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(message = "Missing asset ID"))
                return@get
            }
            
            val deleted = fileStorage.deleteAsset(assetId)
            if (deleted) {
                Logger.i("[AssetRoutes] Asset deleted: $assetId")
                call.respond(HttpStatusCode.NoContent)
            } else {
                Logger.w("[AssetRoutes] Asset not found for deletion: $assetId")
                call.respond(HttpStatusCode.NotFound, ErrorResponse(message = "Asset not found"))
            }
        }
    }
}

@Serializable
data class AssetUploadResponse(
    val id: String,
    val duplicate: Boolean = false
)