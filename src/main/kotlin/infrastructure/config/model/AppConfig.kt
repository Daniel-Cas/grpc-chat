package com.castle.infrastructure.config.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AppConfig(
    val auth: AuthConfig,
    val database: DatabaseConfig,
    val grpc: GrpcConfig,
    val security: SecurityConfig,
    val server: ServerConfig,
)

data class ServerConfig(val port: Int)

data class GrpcConfig(val port: Int)

data class AuthConfig(val paseto: PasetoConfig)

data class PasetoConfig(
    val audience: String,
    val issuer: String,
    @param:JsonProperty("public-key") val publickey: String,
    @param:JsonProperty("symmetric-key") val symmetricKey: String,
)

data class DatabaseConfig(val postgres: PostgresConfig)

data class PostgresConfig(
    @param:JsonProperty("connection-timeout") val connectionTimeout: Int,
    val database: String,
    val host: String,
    @param:JsonProperty("idle-timeout") val idleTimeout: Int,
    val password: String,
    @param:JsonProperty("pool-max-size") val poolMaxSize: Int,
    val port: Int,
    @param:JsonProperty("reconnect-attempts") val reconnectAttempts: Int,
    @param:JsonProperty("reconnect-interval") val reconnectInterval: Long,
    val url: String,
    val user: String,
)

data class SecurityConfig(
    val crypto: CryptoConfig
)

data class CryptoConfig(
    val algorithm: String,
    val secretKey: String,
)
