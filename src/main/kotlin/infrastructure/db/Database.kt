package com.castle.infrastructure.db

import io.vertx.core.Vertx
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
abstract class Database<Config, Client>(config: Config, vertx: Vertx) {
    private var client = AtomicReference(createClient(vertx, config))

    protected abstract fun createClient(
        vertx: Vertx,
        config: Config,
    ): Client

    fun getClient(): Client = client.load()
}
