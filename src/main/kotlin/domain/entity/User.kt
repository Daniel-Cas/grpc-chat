package com.castle.domain.entity

import io.vertx.sqlclient.Tuple
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class User(
    val id: Long = 0,
    val createdAt: Instant = Clock.System.now(),
    val email: String,
    val password: String,
    val updatedAt: Instant = Clock.System.now(),
    val username: String,
) {
    fun toTuple(): Tuple = Tuple.of(
        username,
        email,
        password,
    )
}
