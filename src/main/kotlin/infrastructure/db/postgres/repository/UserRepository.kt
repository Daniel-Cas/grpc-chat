package com.castle.infrastructure.db.postgres.repository

import com.castle.domain.entity.User
import com.castle.infrastructure.db.postgres.Postgresql
import com.castle.shared.toUser
import io.vertx.sqlclient.Tuple

const val INSERT_USER = """
   INSERT INTO users (username, email, password) 
   VALUES ($1, $2, $3)
"""

const val FIND_BY_ID = """
    SELECT * FROM users WHERE id = ?
"""

const val FIND_BY_USERNAME = """
    SELECT 
        * 
    FROM users 
    WHERE username = $1
"""

class UserRepository(
    postgresql: Postgresql
) : BaseRepository(postgresql) {
    override val insertQueryString = INSERT_USER

    suspend fun create(user: User): User {
        val params = user.toTuple()

        return save(params, returnAll = true).toUser()
    }

    suspend fun findById(id: String): User? {
        val params = Tuple.of(id)

        return exec(FIND_BY_ID, params).map { it.toUser() }.firstOrNull()
    }

    suspend fun findByUsername(username: String): User? {
        val params = Tuple.of(username)

        return exec(FIND_BY_USERNAME, params).map { it.toUser() }.firstOrNull()
    }
}