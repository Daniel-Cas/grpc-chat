package com.castle.infrastructure.config

import com.castle.shared.replaceEnvironments
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait

object ConfigLoader {
    suspend operator fun invoke(vertx: Vertx): JsonObject {
        val env = System.getenv("ENV") ?: "dev"
        val config = ConfigRetrieverOptions()
            .addStore(yamlStore("config/application.yaml"))
            .addStore(yamlStore("config/application-$env.yaml"))
            .addStore(envStore())

        return ConfigRetriever.create(vertx, config).config.map { it.replaceEnvironments() }.coAwait()
    }

    private fun yamlStore(path: String) = ConfigStoreOptions()
        .setType("file")
        .setFormat("yaml")
        .setOptional(false)
        .setConfig(JsonObject().put("path", path))

    private fun envStore() = ConfigStoreOptions()
        .setType("env")
}
