package com.castle.application.service.user

import com.castle.domain.entity.Chat
import com.castle.domain.enums.ChatType
import com.castle.infrastructure.db.postgres.repository.ChatRepository
import kotlin.time.ExperimentalTime

class ChatUseCase(
    private val chatRepository: ChatRepository,
) {
    @OptIn(ExperimentalTime::class)
    suspend fun create(type: ChatType): Chat = TODO()
}