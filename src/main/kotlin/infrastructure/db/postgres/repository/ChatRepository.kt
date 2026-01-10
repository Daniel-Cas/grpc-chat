package com.castle.infrastructure.db.postgres.repository

import com.castle.domain.entity.Chat
import com.castle.infrastructure.db.postgres.Postgresql
import com.castle.shared.toChat

const val INSERT_CHAT = """
    INSERT INTO chats (
    created_by
    , description
    , name
    , type
    , visibility
    ) VALUES ($1, $2, $3, $4, $5)
"""

class ChatRepository(
    postgresql: Postgresql
) : BaseRepository(postgresql) {
    override val insertQueryString = INSERT_CHAT

    suspend fun save(chat: Chat): Chat {
        val params = chat.toTuple()

        return save(params = params, returnAll = true).toChat()
    }
}
