package com.castle.infrastructure.verticle.factory

import com.castle.infrastructure.verticle.grpc.GrpcVerticle
import com.castle.infrastructure.verticle.http.ApiVerticle
import io.vertx.core.Deployable
import io.vertx.core.Promise
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.spi.VerticleFactory
import java.util.concurrent.Callable

class CustomVerticleFactory : VerticleFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun prefix(): String = "app"

    private val verticleByClass = mapOf(
        "grpc" to GrpcVerticle::class,
        "api" to ApiVerticle::class,
    )

    override fun createVerticle2(
        verticleName: String,
        classLoader: ClassLoader,
        promise: Promise<Callable<out Deployable>>,
    ) {
        val actualVerticleName = VerticleFactory.removePrefix(verticleName)
        val verticleClass = verticleByClass[actualVerticleName]

        requireNotNull(verticleClass) { "Verticle not found: $actualVerticleName" }

        try {
            val callable = Callable {
                verticleClass.java.getDeclaredConstructor().newInstance()
            }

            promise.complete(callable)
        } catch (e: Exception) {
            logger.error("Error creating verticle", e)

            promise.fail(e)
        }
    }
}
