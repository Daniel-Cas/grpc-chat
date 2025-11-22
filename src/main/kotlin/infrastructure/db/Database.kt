package com.castle.infrastructure.db

import io.vertx.core.Vertx
import io.vertx.sqlclient.Pool
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
abstract class Database<T>(config: T, vertx: Vertx) {
    private var client = AtomicReference(createClient(vertx, config))

    protected abstract fun createClient(
        vertx: Vertx,
        config: T,
    ): Pool

    fun getClient(): Pool = client.load()
}
