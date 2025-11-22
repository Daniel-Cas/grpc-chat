package com.castle.infrastructure.db.postgres

import com.castle.infrastructure.config.model.DatabaseConfig
import com.castle.infrastructure.db.Migration
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.kotlin.coroutines.awaitBlocking
import org.flywaydb.core.Flyway

class Flyway(
    private val config: DatabaseConfig,
) : Migration(config) {
    private val logger = LoggerFactory.getLogger(Flyway::class.java)

    override suspend fun migrate(): Boolean = awaitBlocking {
        try {
            logger.info("[MIGRATE] Executing database migrations")

            val postgresConfig = config.postgres

            Flyway.configure()
                .dataSource(
                    postgresConfig.url,
                    postgresConfig.user,
                    postgresConfig.password,
                ).load()
                .migrate()
                .success
        } catch (e: Exception) {
            logger.error("Error executing migration", e)

            throw e
        }
    }
}
