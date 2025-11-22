package com.castle.infrastructure.db

import com.castle.infrastructure.config.model.DatabaseConfig

abstract class Migration(
    private val config: DatabaseConfig,
) {
    abstract suspend fun migrate(): Boolean
}
