package com.castle.infrastructure.db.postgres

import com.castle.infrastructure.db.Database
import io.vertx.core.Vertx
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions

class Postgresql(
    config: JsonObject,
    vertx: Vertx,
) : Database(config, vertx) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun createClient(
        vertx: Vertx,
        config: JsonObject,
    ): Pool {
        val connectOptions = PgConnectOptions()
            .setPort(config.getString("port").toInt())
            .setHost(config.getString("host"))
            .setDatabase(config.getString("database"))
            .setUser(config.getString("user"))
            .setPassword(config.getString("password"))
            .setReconnectAttempts(config.getInteger("reconnect_attempts"))
            .setReconnectInterval(config.getLong("reconnect_interval"))

        val poolOptions = PoolOptions()
            .setMaxSize(config.getInteger("pool_max_size"))
            .setIdleTimeout(config.getInteger("idle_timeout"))
            .setConnectionTimeout(config.getInteger("connection_timeout"))

        val clientBuilder = PgBuilder
            .pool()
            .using(vertx)
            .with(poolOptions)
            .connectingTo(connectOptions)

        val client = clientBuilder.build()

        client.connection
            .toCompletionStage()
            .thenAccept {
                logger.info("[CREATE_CLIENT] Connected to database: {} ",)
            }.exceptionally { e ->
                logger.error("[CREATE_CLIENT] Error connecting to database: {} ")

                null
            }

        return client
    }
}
