package com.castle

import com.castle.application.launcher.CustomHooks
import com.castle.application.launcher.CustomLauncher
import io.vertx.core.internal.logging.LoggerFactory

private val logger = LoggerFactory.getLogger("com.castle.Application")

fun main(args: Array<String>) {
    val errorCode = 0
    val launcher = CustomLauncher(args, CustomHooks())

    val code = launcher.launch()

    if (code == errorCode) {
        logger.info("Application started correctly")
    } else {
        logger.error("Error occurred during application launch, exit code: $code")
    }
}