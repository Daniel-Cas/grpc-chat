package com.castle.infrastructure.db.redis.repository

import com.castle.infrastructure.config.model.RedisConfig
import com.castle.infrastructure.db.Database
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.redis.client.Command
import io.vertx.redis.client.Redis
import io.vertx.redis.client.Request
import io.vertx.redis.client.Response

abstract class RedisRepository(
    private val database: Database<RedisConfig, Redis>,
    private val client: Redis = database.getClient(),
) {
    protected suspend fun get(key: String): Response? = client.send(Request.cmd(Command.GET).arg(key)).coAwait()

    protected suspend fun del(key: String): Response? = client.send(Request.cmd(Command.DEL).arg(key))?.coAwait()

    protected suspend fun exists(key: String): Response? = client.send(Request.cmd(Command.EXISTS).arg(key))?.coAwait()

    protected suspend fun sadd(key: String, value: String): Response? = client.send(
        Request.cmd(Command.SADD).arg(key).arg(value)
    ).coAwait()

    protected suspend fun set(key: String, value: String, exSeconds: Long? = null): Response? =
        Request.cmd(Command.SET).arg(key).arg(value)
            .apply { exSeconds?.let { arg("EX").arg(it) } }
            .let { client.send(it)?.coAwait() }

    protected suspend fun srem(key: String, value: String): Response? = client.send(
        Request.cmd(Command.SREM).arg(key).arg(value)
    ).coAwait()

    protected suspend fun smembers(key: String): List<String>? = client.send(
        Request.cmd(Command.SMEMBERS).arg(key)
    ).coAwait()?.map { it.toString() }
}
