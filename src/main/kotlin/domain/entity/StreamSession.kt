package com.castle.domain.entity

data class StreamSession(
    val userId: Long,
    val stream: String,
    val subscribedChats: Set<Long>,
)
