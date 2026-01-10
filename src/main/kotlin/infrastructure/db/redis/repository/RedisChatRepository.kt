package com.castle.infrastructure.db.redis.repository

import com.castle.infrastructure.db.redis.Redis
import io.vertx.redis.client.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RedisChatRepository(
    redis: Redis,
) : RedisRepository(redis) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    suspend fun addSubscriber(chatId: Long, userId: Long): Response? {
        logger.debug("[ADD_SUBSCRIBERS] Subscriber $userId added in memory")

        return sadd("chat:$chatId:subscribers", userId.toString())
    }

    suspend fun getSubscribers(chatId: Long): Set<Long> = smembers("chat:$chatId:subscribers")?.mapTo(mutableSetOf()) {
        it.toLong()
    } ?: emptySet()
}