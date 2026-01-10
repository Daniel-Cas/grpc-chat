package com.castle.infrastructure.verticle.http

import io.vertx.core.Promise
import org.slf4j.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle

class ApiVerticle : CoroutineVerticle(
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun start(startFuture: Promise<Void>?) {
        super.start(startFuture)
    }

    override suspend fun stop() {
        super.stop()
    }
}