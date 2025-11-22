package com.castle.shared

import com.castle.domain.dto.auth.TokenClaims
import com.castle.infrastructure.config.EnvironmentReader
import com.castle.infrastructure.config.model.AppConfig
import com.castle.infrastructure.verticle.grpc.interceptor.AuthInterceptor
import io.grpc.Context
import io.grpc.Status
import io.grpc.StatusException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlin.io.encoding.Base64

private val ENVIRONMENT_REGEX = """\$\{([^:}]+)(?::([^}]*))?\}""".toRegex()

fun Context.getClaims(): TokenClaims? = AuthInterceptor.CLAIMS_CONTEXT_KEY.get(this)

fun Context.requireClaims(): TokenClaims =
    getClaims() ?: throw StatusException(Status.UNAUTHENTICATED.withDescription("No authentication context"))

fun String.extractFooter(): String = split(".")
    .getOrNull(3)
    ?.takeIf { it.isNotEmpty() }
    ?.let { Base64.UrlSafe.decode(it).decodeToString() } ?: ""

fun JsonObject.replaceEnvironments(): JsonObject = apply {
    fieldNames().onEach { name ->
        when (val value = getValue(name)) {
            is String -> put(name, value.resolveEnvironmentVars())
            is JsonObject -> value.replaceEnvironments()
            is JsonArray -> value.replaceEnvironments()
        }
    }
}

fun JsonArray.replaceEnvironments() {
    for ((index, value) in withIndex()) {
        when (value) {
            is String -> list[index] = value.resolveEnvironmentVars()
            is JsonObject -> value.replaceEnvironments()
            is JsonArray -> value.replaceEnvironments()
        }
    }
}

fun String.resolveEnvironmentVars(): String = ENVIRONMENT_REGEX.replace(this) { match ->
    val (environmentVar, defaultValue) = match.destructured

    EnvironmentReader.getEnvironmentValue(environmentVar)
        ?: defaultValue.takeIf { it.isNotEmpty() }
        ?: throw IllegalStateException(
            "Environment var $environmentVar needed as config was not defined and no default provided."
        )
}

fun JsonObject.toAppConfig(): AppConfig = mapTo(AppConfig::class.java)
