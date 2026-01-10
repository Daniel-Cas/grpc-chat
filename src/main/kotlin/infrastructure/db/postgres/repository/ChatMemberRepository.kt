package com.castle.infrastructure.db.postgres.repository

import com.castle.domain.entity.ChatMember
import com.castle.infrastructure.db.postgres.Postgresql
import com.castle.shared.toChatMember
import io.vertx.sqlclient.Tuple

const val INSERT_CHAT_MEMBER = """
    INSERT INTO chat_members  (
    active
    , chat_id
    , role
    , user_id
    ) VALUES ($1, $2, $3, $4)
"""

const val SELECT_CHAT_MEMBER = """
    SELECT 
        * 
    FROM chat_members 
    WHERE chat_id = $1
    AND user_id = $2
"""

class ChatMemberRepository(
    postgresql: Postgresql,
) : BaseRepository(postgresql) {
    override val insertQueryString: String = INSERT_CHAT_MEMBER

    suspend fun save(chatMember: ChatMember): ChatMember {
        val params = chatMember.toTuple()

        return save(params = params, returnAll = true).toChatMember()
    }

    suspend fun saveAll(chatMembers: List<ChatMember>): List<ChatMember> {
        val params = chatMembers.map { it.toTuple() }

        return generateSequence(execAll(INSERT_CHAT_MEMBER, params, "RETURNING *")) { it.next() }
            .flatMap { it.asSequence() }
            .map { it.toChatMember() }
            .toList()
    }

    suspend fun findByChatIdAndUserId(userId: Long, chatId: Long): List<ChatMember> {
        val params = Tuple.of(chatId, userId)

        return exec(SELECT_CHAT_MEMBER, params).map { it.toChatMember() }
    }
}
