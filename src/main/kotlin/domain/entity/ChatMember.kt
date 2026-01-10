package com.castle.domain.entity

import com.castle.domain.enums.ChatRole
import io.vertx.sqlclient.Tuple
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class ChatMember(
    val active: Boolean = true,
    val chatId: Long,
    val createdAt: Instant = Clock.System.now(),
    val id: Long = 0,
    val role: ChatRole,
    val userId: Long,
) {
    fun toTuple(): Tuple = Tuple.of(active, chatId, role, userId)
}
