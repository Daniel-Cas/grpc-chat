package com.castle.infrastructure.config

object EnvironmentReader {
    fun getEnvironmentValue(name: String): String? = System.getProperty(name) ?: System.getenv(name)
}
