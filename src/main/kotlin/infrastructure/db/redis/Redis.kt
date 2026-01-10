package com.castle.infrastructure.db.redis

import com.castle.infrastructure.config.model.RedisConfig
import com.castle.infrastructure.db.Database
import io.vertx.core.Vertx
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisOptions
import org.slf4j.LoggerFactory

class Redis(config: RedisConfig, vertx: Vertx) : Database<RedisConfig, Redis>(config, vertx) {
    private val logger = LoggerFactory.getLogger(Redis::class.java)

    override fun createClient(
        vertx: Vertx,
        config: RedisConfig,
    ): Redis {
        val options = RedisOptions()
            .setConnectionString(config.url)

        val client = Redis.createClient(vertx, options)

        client.connect()
            .onSuccess {
                logger.info("[CREATE_CLIENT] Connected to redis")
            }
            .onFailure { e ->
                logger.error("[CREATE_CLIENT] Failed to connect to redis", e)
            }

        return client
    }
}
