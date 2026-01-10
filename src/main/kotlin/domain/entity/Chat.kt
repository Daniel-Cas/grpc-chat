package com.castle.domain.entity

import com.castle.domain.enums.ChatType
import com.castle.domain.enums.ChatVisibility
import io.vertx.sqlclient.Tuple
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class Chat(
    val id: Long = 0,
    val createdBy: Long,
    val description: String,
    val name: String,
    val type: ChatType,
    val visibility: ChatVisibility = ChatVisibility.VISIBLE,
    val createdAt: Instant = Clock.System.now(),
) {
    fun toTuple(): Tuple = Tuple.of(createdBy, description, name, type, visibility)
}
