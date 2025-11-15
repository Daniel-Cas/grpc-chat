package com.castle.application.launcher

import com.castle.infrastructure.config.ConfigLoader
import com.castle.infrastructure.di.DependencyModule
import com.castle.infrastructure.verticle.factory.CustomVerticleFactory
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.launcher.application.HookContext
import io.vertx.launcher.application.VertxApplicationHooks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CustomHooks(
    private val hookDispatcher: CoroutineContext = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher(),
) : VertxApplicationHooks {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalTime::class)
    override fun afterVertxOptionsParsed(vertxOptions: JsonObject?): JsonObject? {
        startAsciiArt()

        logger.info("[AFTER_VERTX_OPTIONS_PARSED] Setting Vert.x options at: ${Clock.System.now()}")

        return super.afterVertxOptionsParsed(vertxOptions)
    }

    @OptIn(ExperimentalTime::class)
    override fun beforeStartingVertx(context: HookContext?) {
        logger.info("[BEFORE_STARTING_VERTX] Setting Vert.x options at: ${Clock.System.now()}")

        val options = context?.vertxOptions()

        options?.let {
            it.setEventLoopPoolSize(Runtime.getRuntime().availableProcessors() * 2)
            it.setWorkerPoolSize(20)
            it.setInternalBlockingPoolSize(20)
            it.setPreferNativeTransport(true)
        }

        super.beforeStartingVertx(context)
    }

    @OptIn(ExperimentalTime::class)
    override fun afterVertxStarted(context: HookContext?) {
        logger.info("[AFTER_VERTX_STARTED] Registering Verticle factory")
        val vertx = context?.vertx() ?: throw RuntimeException("[AFTER_VERTX_STARTED] Vertx unavailable")
        val config = CoroutineScope(hookDispatcher).async { ConfigLoader(vertx) }.asCompletableFuture().get()

        vertx.sharedData().getLocalMap<String, JsonObject>("app.config")["config"] = config

        DependencyModule.initialize(config)

        context.vertx()?.registerVerticleFactory(CustomVerticleFactory())

        logger.info("[AFTER_VERTX_STARTED] Verticle factory registered ${Clock.System.now()}")

        super.afterVertxStarted(context)
    }

    @OptIn(ExperimentalTime::class)
    override fun beforeDeployingVerticle(context: HookContext?) {
        logger.info("[BEFORE_DEPLOYING_VERTICLE] Deploying verticle at: ${Clock.System.now()}")
        val vertx = context?.vertx() ?: throw RuntimeException("[AFTER_VERTX_STARTED] Vertx unavailable")

        val config = vertx.sharedData().getLocalMap<String, JsonObject>("app.config")["config"]
        context.deploymentOptions().config = config

        super.beforeDeployingVerticle(context)
    }

    @OptIn(ExperimentalTime::class)
    override fun afterVerticleDeployed(context: HookContext?) {
        logger.info("[AFTER_VERTICLE_DEPLOYED] Verticle deployed at: ${Clock.System.now()}",)

        super.afterVerticleDeployed(context)
    }

    private fun startAsciiArt() = logger.info("""
        
      __  __                 __                 
     /\ \/\ \               /\ \__              
     \ \ \ \ \     __   _ __\ \ ,_\      __  _  
      \ \ \ \ \  /'__`\/\`'__\ \ \/     /\ \/'\ 
       \ \ \_/ \/\  __/\ \ \/ \ \ \_  __\/>  </ 
        \ `\___/\ \____\\ \_\  \ \__\/\_\/\_/\_\
        `\/__/  \/____/ \/_/   \/__/\/_/\//\/_/

    """.trimIndent()
    )
}
