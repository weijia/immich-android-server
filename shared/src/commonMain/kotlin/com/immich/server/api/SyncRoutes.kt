package com.immich.server.api

import com.immich.server.platform.Logger
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Sync Routes
 * 
 * Implements basic sync API stubs for client compatibility.
 * 
 * DELETE /api/sync/ack - Delete sync acknowledgments
 * POST /api/sync/stream - Stream sync changes
 * POST /api/sync/ack - Send sync acknowledgments
 */
fun Route.syncRoutes() {
    route("/sync") {
        // DELETE /api/sync/ack - Delete sync ack for specific types
        delete("/ack") {
            Logger.i("[SyncRoutes] DELETE /sync/ack received")
            
            try {
                val body = call.receive<String>()
                Logger.d("[SyncRoutes] Request body: $body")
                
                // Stub implementation - just return success
                // In a full implementation, this would delete sync ack entries from database
                Logger.i("[SyncRoutes] deleteSyncAck stub - returning success")
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                Logger.e("[SyncRoutes] deleteSyncAck error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Error: ${e.message}"))
            }
        }
        
        // POST /api/sync/ack - Send sync acknowledgments
        post("/ack") {
            Logger.i("[SyncRoutes] POST /sync/ack received")
            
            try {
                val body = call.receive<String>()
                Logger.d("[SyncRoutes] Request body: $body")
                
                // Stub implementation - just return success
                Logger.i("[SyncRoutes] sendSyncAck stub - returning success")
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                Logger.e("[SyncRoutes] sendSyncAck error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Error: ${e.message}"))
            }
        }
        
        // POST /api/sync/stream - Stream sync changes
        post("/stream") {
            Logger.i("[SyncRoutes] POST /sync/stream received")
            
            try {
                val body = call.receive<String>()
                Logger.d("[SyncRoutes] Request body: $body")
                
                // Stub implementation - return empty stream
                // In a full implementation, this would stream sync events as JSON lines
                Logger.i("[SyncRoutes] syncStream stub - returning empty response")
                
                // Return syncCompleteV1 event to signal end of sync
                val syncComplete = """{"type":"SyncCompleteV1","data":{},"ack":"sync-complete"}"""
                call.respond(syncComplete)
            } catch (e: Exception) {
                Logger.e("[SyncRoutes] syncStream error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Error: ${e.message}"))
            }
        }
    }
}

// DTOs for sync API
@Serializable
data class SyncAckDeleteDto(
    val types: List<String>? = null
)

@Serializable
data class SyncAckSetDto(
    val acks: List<String>? = null
)

@Serializable
data class SyncStreamDto(
    val types: List<String>? = null
)