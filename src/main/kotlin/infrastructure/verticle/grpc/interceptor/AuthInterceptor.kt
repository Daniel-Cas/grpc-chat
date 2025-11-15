package com.castle.infrastructure.verticle.grpc.interceptor

import com.castle.application.service.auth.KeyManager
import com.castle.application.service.auth.PasetoService
import com.castle.domain.dto.auth.TokenClaims
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusException
import io.vertx.core.internal.logging.LoggerFactory

class AuthInterceptor(
    private val pasetoService: PasetoService,
    private val symmetricKey: ByteArray,
    private val publicKeyBase64: String,
) : ServerInterceptor {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        val CLAIMS_CONTEXT_KEY: Context.Key<TokenClaims> = Context.key("claims")
        private const val AUTHORIZATION_HEADER = "authorization"
        private const val BEARER_PREFIX = "Bearer "

        private val PUBLIC_METHODS = setOf(
            "auth.v1.AuthService/login",
            "auth.v1.AuthService/refreshToken"
        )
    }

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        logger.info("[INTERCEPT_CALL] Intercepting")
        val methodName = call.methodDescriptor.fullMethodName

        if (methodName in PUBLIC_METHODS) {
            return next.startCall(call, headers)
        }

        return try {
            val token = extractToken(headers)
            val claims = validateToken(token)

            val context = Context.current().withValue(CLAIMS_CONTEXT_KEY, claims)

            Contexts.interceptCall(context, call, headers, next)
        } catch (e: Exception) {
            logger.warn("Auth failed for $methodName: ${e.message}")
            call.close(Status.UNAUTHENTICATED.withDescription(e.message), Metadata())

            object : ServerCall.Listener<ReqT>() {}
        }
    }

    private fun extractToken(headers: Metadata): String {
        val authHeader = headers.get(Metadata.Key.of(AUTHORIZATION_HEADER, Metadata.ASCII_STRING_MARSHALLER))
            ?: throw StatusException(Status.UNAUTHENTICATED.withDescription("Missing authorization header"))

        if (!authHeader.startsWith(BEARER_PREFIX)) {
            throw StatusException(Status.UNAUTHENTICATED.withDescription("Invalid authorization format"))
        }

        return authHeader.removePrefix(BEARER_PREFIX)
    }

    private fun validateToken(token: String): TokenClaims = when {
        token.startsWith("v4.local.") -> {
            pasetoService.validateLocalToken(token, symmetricKey)
        }
        token.startsWith("v4.public.") -> {
            val publicKey = publicKeyBase64.let { KeyManager.publicKeyFromBase64(it) }
            pasetoService.validatePublicToken(token, publicKey)
        }

        else -> throw StatusException(Status.UNAUTHENTICATED.withDescription("Invalid token format"))
    }
}
