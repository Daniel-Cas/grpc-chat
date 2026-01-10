package com.castle.application.usecase

import com.castle.domain.dto.request.CreateChatRequest
import com.castle.domain.entity.Chat
import com.castle.domain.enums.ChatType
import com.castle.domain.enums.ChatVisibility
import com.castle.infrastructure.db.postgres.repository.ChatRepository
import com.castle.shared.Result
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

class CreateChatUseCase(
    private val chatRepository: ChatRepository,
) : UseCase<CreateChatRequest, Result<Chat>> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalTime::class)
    override suspend fun invoke(input: CreateChatRequest): Result<Chat> = try {
        validate(input)

        Result.Success(
            chatRepository.save(
                Chat(
                    createdBy = input.createdBy,
                    description = input.description,
                    type = input.type,
                    visibility = input.visibility,
                    name = input.name,
                )
            )
        )
    } catch (e: Exception) {
        logger.error("[CREATE_CHAT_USE_CASE] Error creating chat: {}", e.message)

        Result.Failure(e)
    }

    private fun validate(input: CreateChatRequest) {
        require(input.name.isNotBlank()) { "Chat must have name" }

        validateChatDirect(input)
    }

    private fun validateChatDirect(input: CreateChatRequest) {
        if (input.type == ChatType.DIRECT) {
            require(input.visibility == ChatVisibility.PRIVATE) { "Chat must be private" }
            require(input.memberIds.isNotEmpty()) { "Chat must be a member" }
        }
    }
}
