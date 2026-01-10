package com.castle.infrastructure.db.postgres.repository

import com.castle.infrastructure.config.model.PostgresConfig
import com.castle.infrastructure.db.Database
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple

abstract class BaseRepository(
    private val database: Database<PostgresConfig, Pool>,
) {
    open val insertQueryString: String = ""
    open val updateQueryString: String = ""
    open val upsertQueryString: String = ""

    protected suspend fun save(params: Tuple, returnAll: Boolean = false, conflictIgnore: Boolean = false): Row {
        val insertReturningClause = if (returnAll) { "RETURNING *" } else { "RETURNING id" }
        val onConflictClause = if (conflictIgnore) { "ON CONFLICT DO NOTHING" } else ""
        val queryString = "$insertQueryString $onConflictClause $insertReturningClause"

        return database
            .getClient()
            .preparedQuery(queryString)
            .execute(params)
            .otherwise { t -> throw t }
            .coAwait()
            .first()
    }

    protected suspend fun update(
        params: Tuple,
        returnAll: Boolean = false,
        conflictIgnore: Boolean = false,
    ): Row {
        val updateReturningClause = if (returnAll) { "RETURNING *" } else { "RETURNING id" }
        val onConflictClause = if (conflictIgnore) { "ON CONFLICT DO NOTHING" } else ""
        val queryString = "$updateQueryString $onConflictClause $updateReturningClause".trimIndent()

        return database
            .getClient()
            .preparedQuery(queryString)
            .execute(params)
            .otherwise { t -> throw t }
            .coAwait()
            .first()
    }

    protected suspend fun updateAll(
        params: List<Tuple>,
        returningColumn: String? = null,
    ): RowSet<Row> {
        val queryToExecute = if (returningColumn != null) {
            "$updateQueryString RETURNING $returningColumn"
        } else {
            updateQueryString
        }

        return database
            .getClient()
            .preparedQuery(queryToExecute.trimIndent())
            .executeBatch(params)
            .otherwise { t -> throw t }
            .coAwait()
    }

    protected suspend fun saveWithoutReturn(
        params: Tuple,
        conflictIgnore: Boolean = false,
    ): RowSet<Row> {
        val onConflictClause = if (conflictIgnore) { "ON CONFLICT DO NOTHING" } else ""
        val queryString = "$insertQueryString $onConflictClause".trimIndent()

        return database
            .getClient()
            .preparedQuery(queryString)
            .execute(params)
            .otherwise { t -> throw t }
            .coAwait()
    }

    protected suspend fun exec(
        queryString: String,
        params: Tuple,
    ): RowSet<Row> = database.getClient()
        .preparedQuery(queryString.trimIndent())
        .execute(params)
        .otherwise { t -> throw t }
        .coAwait()

    protected suspend fun execAll(
        queryString: String,
        params: List<Tuple>,
        returningClause: String? = null,
    ): RowSet<Row> {
        val queryToExecute = if (returningClause != null) {
            "$queryString $returningClause"
        } else {
            queryString
        }

        return database
            .getClient()
            .preparedQuery(queryToExecute.trimIndent())
            .executeBatch(params)
            .otherwise { t -> throw t }
            .coAwait()
    }
}