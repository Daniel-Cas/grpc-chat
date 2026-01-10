package com.castle.application.usecase

import com.castle.domain.entity.Message
import com.castle.infrastructure.db.postgres.repository.MessageRepository
import com.castle.shared.Result
import org.slf4j.LoggerFactory

class CreateMessageUseCase(
    private val messageRepository: MessageRepository,
) : UseCase<Message, Result<Message>> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun invoke(input: Message): Result<Message> = try {
        Result.Success(messageRepository.save(input))
    } catch (e: Exception) {
        logger.error(e.message, e)

        Result.Failure(e)
    }
}
