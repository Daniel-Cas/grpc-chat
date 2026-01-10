package com.castle.infrastructure.db.postgres.repository

import com.castle.domain.entity.Message
import com.castle.infrastructure.db.postgres.Postgresql
import com.castle.shared.toMessage

const val INSERT_MESSAGE = """
    INSERT INTO messages (
    chat_id
    , sender_id
    , reply_to_id
    , client_message_id
    , content
    ) VALUES ($1, $2, $3, $4, $5)
"""

class MessageRepository(
    postgresql: Postgresql,
) : BaseRepository(postgresql) {
    override val insertQueryString: String = INSERT_MESSAGE

    suspend fun save(message: Message): Message {
        val params = message.toTuple()

        return save(params = params, returnAll = true).toMessage()
    }
}
