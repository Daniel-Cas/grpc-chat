package com.castle.infrastructure.verticle.grpc.service

import auth.v1.Auth
import auth.v1.AuthServiceGrpcKt
import auth.v1.loginResponse
import auth.v1.validateResponse
import com.castle.application.security.crypto.Cipher
import com.castle.application.service.auth.PasetoService
import com.castle.application.usecase.UserUseCase
import com.castle.domain.dto.auth.TokenFooter
import io.grpc.Status
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.LoggerFactory
import types.v1.Enums
import java.security.KeyPair
import java.util.concurrent.Executors
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi

class AuthService(
    private val cipher: Cipher,
    private val keyPair: KeyPair,
    private val pasetoService: PasetoService,
    private val symmetricKey: ByteArray,
    private val userUseCase: UserUseCase,
) : AuthServiceGrpcKt.AuthServiceCoroutineImplBase(
    coroutineContext = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher(),
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalTime::class)
    override suspend fun login(request: Auth.LoginRequest): Auth.LoginResponse = loginResponse {
        logger.info("[LOGIN] username: {}", request.username)
        val invalidCredentials = Status.UNAUTHENTICATED.withDescription("Invalid credentials").asException()

        require(request.username.isNotBlank()) { "username required" }
        require(request.password.isNotBlank()) { "password required" }

        val user = userUseCase.getByUsername(request.username) ?: throw invalidCredentials

        if (!cipher.verify(request.password, user.password)) {
            throw invalidCredentials
        }

        val roles = emptyList<String>()
        val permissions = emptyList<String>()
        val (accessToken, refreshToken) = when (request.tokenType) {
            Enums.TokenType.LOCAL -> generateLocalTokens(user.id.toString(), roles, permissions)
            Enums.TokenType.PUBLIC -> generatePublicTokens(user.id.toString(), roles, permissions)

            else -> throw Status.INVALID_ARGUMENT.withDescription("Invalid token type").asException()
        }

        this.accessToken = accessToken
        this.refreshToken = refreshToken
        expiresIn = Clock.System.now().plus(900L.toDuration(DurationUnit.SECONDS)).toEpochMilliseconds()
        tokenType = Enums.TokenType.LOCAL.toString()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateLocalTokens(userId: String, roles: List<String>, permissions: List<String>): Pair<String, String> {
        val footer = TokenFooter(keyId = "key-2024", type = "access")

        val accessToken = pasetoService.createLocalToken(
            subject = userId,
            roles = roles,
            permissions = permissions,
            ttlSeconds = PasetoService.DEFAULT_ACCESS_TOKEN_TTL,
            symmetricKey = symmetricKey,
            tokenFooter = footer,
        )

        val refreshToken = pasetoService.createLocalToken(
            subject = userId,
            roles = emptyList(),
            permissions = emptyList(),
            ttlSeconds = PasetoService.DEFAULT_REFRESH_TOKEN_TTL,
            symmetricKey = symmetricKey,
        )

        return accessToken to refreshToken
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generatePublicTokens(userId: String, roles: List<String>, permissions: List<String>): Pair<String, String> {
        val footer = TokenFooter(keyId = "key-2024", type = "access")

        val accessToken = pasetoService.createPublicToken(
            subject = userId,
            roles = roles,
            permissions = permissions,
            ttlSeconds = PasetoService.Companion.DEFAULT_ACCESS_TOKEN_TTL,
            privateKey = keyPair.private,
            tokenFooter = footer,
        )

        val refreshToken = pasetoService.createPublicToken(
            subject = userId,
            roles = emptyList(),
            permissions = emptyList(),
            ttlSeconds = PasetoService.Companion.DEFAULT_REFRESH_TOKEN_TTL,
            privateKey = keyPair.private,
        )

        return accessToken to refreshToken
    }

    override suspend fun refreshToken(request: Auth.RefreshRequest): Auth.TokenResponse {
        return super.refreshToken(request)
    }

    override suspend fun validateToken(request: Auth.ValidateRequest): Auth.ValidateResponse = validateResponse {
        try {
            val claims = when {
                request.token.startsWith("v4.local.") ->
                    pasetoService.validateLocalToken(request.token, symmetricKey)

                request.token.startsWith("v4.public.") ->
                    pasetoService.validatePublicToken(request.token, keyPair.public)

                else -> throw Status.INVALID_ARGUMENT.withDescription("Token inválido").asException()
            }

            valid = true
            subject = claims.subject
            roles.addAll(claims.roles)
            permissions.addAll(claims.permissions)
        } catch (e: Exception) {
            logger.warn("Token inválido: ${e.message}")

            valid = false
        }

    }

    override suspend fun logout(request: Auth.LogoutRequest): Auth.LogoutResponse {
        return super.logout(request)
    }
}