package com.immich.server.api

import com.immich.server.model.LoginRequest
import com.immich.server.model.LoginResponse
import com.immich.server.model.UserResponse
import com.immich.server.service.AuthService
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.authRoutes(authService: AuthService) {
    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        val response = authService.login(request.email, request.password)
        if (response != null) {
            call.respond(response)
        } else {
            call.respond(io.ktor.http.HttpStatusCode.Unauthorized, mapOf("message" to "Invalid credentials"))
        }
    }

    post("/auth/logout") {
        // TODO: Invalidate token
        call.respond(mapOf("successful" to true))
    }

    get("/auth/admin-sign-up") {
        // Check if admin exists
        val hasAdmin = authService.hasAdmin()
        call.respond(mapOf("isAdmin" to hasAdmin))
    }

    post("/auth/admin-sign-up") {
        val request = call.receive<LoginRequest>()
        val response = authService.createAdmin(request.email, request.password)
        if (response != null) {
            call.respond(response)
        } else {
            call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("message" to "Admin already exists"))
        }
    }

    get("/users/me") {
        // TODO: Get current user from auth
        call.respond(UserResponse("", "", ""))
    }
}
