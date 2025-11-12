package com.castle.application.service.auth

import auth.v1.Auth
import auth.v1.AuthServiceGrpcKt
import io.vertx.core.internal.logging.LoggerFactory
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class AuthService : AuthServiceGrpcKt.AuthServiceCoroutineImplBase(
    coroutineContext = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun login(request: Auth.LoginRequest): Auth.LoginResponse {
        return super.login(request)
    }

    override suspend fun refreshToken(request: Auth.RefreshRequest): Auth.TokenResponse {
        return super.refreshToken(request)
    }

    override suspend fun validateToken(request: Auth.ValidateRequest): Auth.ValidateResponse {
        return super.validateToken(request)
    }

    override suspend fun logout(request: Auth.LogoutRequest): Auth.LogoutResponse {
        return super.logout(request)
    }
}