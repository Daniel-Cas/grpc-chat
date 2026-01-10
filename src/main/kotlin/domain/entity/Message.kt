package com.castle.domain.entity

import io.vertx.sqlclient.Tuple
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class Message(
    val id: Long = 0,
    val chatId: Long,
    val senderId: Long,
    val replyToId: Long? = null,
    val clientMessageId: String? = null,
    val content: String,
    val sentAt: Instant = Clock.System.now(),
) {
    fun toTuple(): Tuple = Tuple.of(chatId, senderId, replyToId, clientMessageId, content)
}
