package com.castle.infrastructure.db

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Pool
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
abstract class Database(
    config: JsonObject,
    vertx: Vertx,
) {
    private var client = AtomicReference(createClient(vertx, config))

    protected abstract fun createClient(
        vertx: Vertx,
        config: JsonObject,
    ): Pool

    fun getClient(): Pool = client.load()
}
