package com.immich.server.api

import com.immich.server.model.AdminCheckResponse
import com.immich.server.model.ErrorResponse
import com.immich.server.model.LoginRequest
import com.immich.server.model.LoginResponse
import com.immich.server.model.SuccessResponse
import com.immich.server.model.UserResponse
import com.immich.server.service.AuthService
import com.immich.server.platform.Logger
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.authRoutes(authService: AuthService) {
    post("/auth/login") {
        Logger.i("[AuthRoutes] POST /auth/login received")
        try {
            val request = call.receive<LoginRequest>()
            Logger.d("[AuthRoutes] Login request: email=${request.email}")
            
            val response = authService.login(request.email, request.password)
            if (response != null) {
                Logger.i("[AuthRoutes] Login success: userId=${response.userId}")
                call.respond(response)
            } else {
                Logger.w("[AuthRoutes] Login failed: invalid credentials")
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized, ErrorResponse(message = "Invalid credentials"))
            }
        } catch (e: Exception) {
            Logger.e("[AuthRoutes] Login error", e)
            call.respond(io.ktor.http.HttpStatusCode.InternalServerError, ErrorResponse(message = "Server error: ${e.message}"))
        }
    }

    post("/auth/logout") {
        Logger.i("[AuthRoutes] POST /auth/logout received")
        call.respond(SuccessResponse())
    }

    get("/auth/admin-sign-up") {
        Logger.i("[AuthRoutes] GET /auth/admin-sign-up received")
        val hasAdmin = authService.hasAdmin()
        Logger.d("[AuthRoutes] hasAdmin=$hasAdmin")
        call.respond(AdminCheckResponse(isAdmin = hasAdmin))
    }

    post("/auth/admin-sign-up") {
        Logger.i("[AuthRoutes] POST /auth/admin-sign-up received")
        try {
            val request = call.receive<LoginRequest>()
            Logger.d("[AuthRoutes] Admin signup request: email=${request.email}")
            
            val response = authService.createAdmin(request.email, request.password)
            if (response != null) {
                Logger.i("[AuthRoutes] Admin created: userId=${response.userId}")
                call.respond(response)
            } else {
                Logger.w("[AuthRoutes] Admin signup failed: admin already exists")
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, ErrorResponse(message = "Admin already exists"))
            }
        } catch (e: Exception) {
            Logger.e("[AuthRoutes] Admin signup error", e)
            call.respond(io.ktor.http.HttpStatusCode.InternalServerError, ErrorResponse(message = "Server error: ${e.message}"))
        }
    }

    get("/users/me") {
        Logger.i("[AuthRoutes] GET /users/me received")
        // TODO: Get current user from auth
        call.respond(UserResponse("", "", ""))
    }
}