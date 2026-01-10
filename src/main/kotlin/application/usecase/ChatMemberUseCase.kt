package com.castle.application.usecase

import com.castle.domain.entity.ChatMember
import com.castle.infrastructure.db.postgres.repository.ChatMemberRepository
import com.castle.shared.Result
import org.slf4j.LoggerFactory

class ChatMemberUseCase(
    private val chatMemberRepository: ChatMemberRepository,
) : UseCase<List<ChatMember>, Result<List<ChatMember>>> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun invoke(input: List<ChatMember>): Result<List<ChatMember>> = try {
        logger.info("[CHAT_MEMBER_USE_CASE] Creating a new chat members")
        Result.Success(
            chatMemberRepository.saveAll(input)
        )
    } catch (e: Exception) {
        logger.error(e.message)
        Result.Failure(e)
    }

    suspend fun findByChatIdAndUserId(chatId: Long, userId: Long): List<ChatMember> =
        chatMemberRepository.findByChatIdAndUserId(userId, chatId)
}