package com.castle.shared

import com.castle.domain.dto.auth.TokenClaims
import com.castle.domain.entity.Chat
import com.castle.domain.entity.ChatMember
import com.castle.domain.entity.Message
import com.castle.domain.entity.User
import com.castle.domain.enums.ChatRole
import com.castle.domain.enums.ChatType
import com.castle.domain.enums.ChatVisibility
import com.castle.infrastructure.config.EnvironmentReader
import com.castle.infrastructure.config.model.AppConfig
import com.castle.infrastructure.verticle.grpc.interceptor.AuthInterceptor
import io.grpc.Context
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import java.time.Instant as Instant4j
import java.time.ZoneOffset
import kotlin.io.encoding.Base64
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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

@OptIn(ExperimentalTime::class)
fun Row.toUser() = User(
    id = getLong("id"),
    username = getString("username"),
    password = getString("password"),
    email = getString("email"),
    createdAt = getLocalDateTime("created_at").toInstant(ZoneOffset.UTC).toKotlinInstant(),
    updatedAt = getLocalDateTime("created_at").toInstant(ZoneOffset.UTC).toKotlinInstant(),
)

@OptIn(ExperimentalTime::class)
fun Row.toChat() = Chat(
    id = getLong("id"),
    type = ChatType.valueOf(getString("type")),
    createdBy = getLong("created_by"),
    createdAt = getLocalDateTime("created_at").toInstant(ZoneOffset.UTC).toKotlinInstant(),
    description = getString("description"),
    name = getString("name"),
    visibility = ChatVisibility.valueOf(getString("visibility")),
)

@OptIn(ExperimentalTime::class)
fun Row.toChatMember() = ChatMember(
    active = getBoolean("active"),
    chatId = getLong("chat_id"),
    createdAt = getLocalDateTime("created_at").toInstant(ZoneOffset.UTC).toKotlinInstant(),
    id = getLong("id"),
    userId = getLong("user_id"),
    role = ChatRole.valueOf(getString("role")),
)

@OptIn(ExperimentalTime::class)
fun Row.toMessage() = Message(
    chatId = getLong("chat_id"),
    senderId = getLong("sender_id"),
    replyToId = getLong("reply_to_id"),
    clientMessageId = getString("client_message_id"),
    content = getString("content"),
    sentAt = getLocalDateTime("sent_at").toInstant(ZoneOffset.UTC).toKotlinInstant()
)

@OptIn(ExperimentalTime::class)
fun Instant4j.toKotlinInstant(): Instant = Instant.fromEpochSeconds(epochSecond, nano.toLong())

fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> value
    is Result.Failure -> throw error
}

fun <T> Result<T>.onFailure(action: (Throwable) -> Unit): Result<T> = apply {
    if (this is Result.Failure) action(error)
}

suspend fun <T> grpcResponse(
    block: suspend () -> T
): T = runCatching { block()
    block()
}.getOrElse { exception ->
    throw when (exception) {
        is StatusException -> exception
        is StatusRuntimeException -> exception

        else -> Status.INTERNAL
            .withDescription(exception.message ?: "Internal server error")
            .withCause(exception)
            .asException()
    }
}
