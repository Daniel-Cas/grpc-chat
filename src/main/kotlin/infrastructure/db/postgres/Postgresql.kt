package com.castle.infrastructure.db.postgres

import com.castle.infrastructure.config.model.PostgresConfig
import com.castle.infrastructure.db.Database
import io.vertx.core.Vertx
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions

class Postgresql(config: PostgresConfig, vertx: Vertx) : Database<PostgresConfig>(config, vertx) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun createClient(vertx: Vertx, config: PostgresConfig): Pool {
        val connectOptions = PgConnectOptions()
            .setPort(config.port)
            .setHost(config.host)
            .setDatabase(config.database)
            .setUser(config.user)
            .setPassword(config.password)
            .setReconnectAttempts(config.reconnectAttempts)
            .setReconnectInterval(config.reconnectInterval)

        val poolOptions = PoolOptions()
            .setMaxSize(config.poolMaxSize)
            .setIdleTimeout(config.idleTimeout)
            .setConnectionTimeout(config.connectionTimeout)

        val clientBuilder = PgBuilder
            .pool()
            .using(vertx)
            .with(poolOptions)
            .connectingTo(connectOptions)

        val client = clientBuilder.build()

        client.connection
            .toCompletionStage()
            .thenAccept {
                logger.info("[CREATE_CLIENT] Connected to database: $it ")
            }.exceptionally { e ->
                logger.error("[CREATE_CLIENT] Error connecting to database: ${e.message}")

                null
            }

        return client
    }
}
