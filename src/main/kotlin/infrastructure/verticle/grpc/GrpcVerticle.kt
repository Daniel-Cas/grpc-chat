package com.castle.infrastructure.verticle.grpc

import com.castle.application.service.chat.ChatService
import com.castle.application.service.auth.AuthService
import com.castle.infrastructure.db.postgres.Flyway
import com.castle.infrastructure.di.ServiceRegistry
import com.castle.infrastructure.verticle.grpc.interceptor.AuthInterceptor
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
    private lateinit var grpcServer: Server

    override suspend fun start() = try {
        val port = config.getJsonObject("grpc").getInteger("port", 9090)

        ServiceRegistry.get<Flyway>().migrate()

        grpcServer = ServerBuilder.forPort(port)
            .intercept(ServiceRegistry.get<AuthInterceptor>())
            .addService(ServiceRegistry.get<ChatService>())
            .addService(ServiceRegistry.get<AuthService>())
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