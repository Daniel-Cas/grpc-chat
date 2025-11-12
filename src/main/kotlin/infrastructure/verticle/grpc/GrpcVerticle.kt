package com.castle.infrastructure.verticle.grpc

import com.castle.application.service.chat.ChatService
import com.castle.application.service.UserService
import com.castle.application.service.auth.AuthService
import com.castle.application.service.auth.KeyManager
import com.castle.application.service.auth.PasetoService
import com.castle.infrastructure.verticle.grpc.interceptor.AuthInterceptor
import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.CompressorRegistry
import io.grpc.DecompressorRegistry
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle
import java.util.concurrent.TimeUnit

class GrpcVerticle : CoroutineVerticle() {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val defaultPort: Int = 9090

    private lateinit var grpcServer: Server

    override suspend fun start() = try {
        val port = config.getInteger("port", defaultPort)

        val userService = UserService()
        val symmetricKey = KeyManager.symmetricKeyFromBase64("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        val publicKeyBase64 = "MCowBQYDK2VwAyEAXrJSLf9z8Y8pN3qK5mH2wT1vR4xE6nU9cF7bA8sD2gI="
        val pasetoService = PasetoService(
            issuer = "grpc-chat",
            audience = "grpc-chat-api",
            objectMapper = ObjectMapper().apply {
                findAndRegisterModules()
            }
        )
        val authInterceptor = AuthInterceptor(
            pasetoService = pasetoService,
            symmetricKey = symmetricKey,
            publicKeyBase64 = publicKeyBase64,
        )
        grpcServer = ServerBuilder.forPort(port)
            .intercept(authInterceptor)
            .addService(ChatService(userService))
            .addService(AuthService())
            .addService(ProtoReflectionService.newInstance())
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(5, TimeUnit.SECONDS)
            .permitKeepAliveWithoutCalls(true)
            .maxInboundMessageSize(4 * 1024 * 1024)
            .compressorRegistry(CompressorRegistry.getDefaultInstance())
            .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
            .build()
            .start()

        logger.info("gRPC server started, listening on ${grpcServer.port}")

    } catch (e: Exception) {
        logger.error("[START] Error starting grpc server", e)

        throw e
    }

    override suspend fun stop() {
        try {
            logger.info("[STOP] Stopping grpc server")
            grpcServer.shutdown()

            if (!grpcServer.awaitTermination(5, TimeUnit.SECONDS)) {
                grpcServer.shutdownNow()
            }
        } catch (e: Exception) {
            logger.error("[STOP] Error stopping grpc server", e)
        }
    }
}