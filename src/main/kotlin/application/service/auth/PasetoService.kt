package com.castle.application.service.auth

import com.castle.domain.dto.auth.TokenClaims
import com.castle.domain.dto.auth.TokenFooter
import com.castle.domain.dto.builder.tokenClaims
import com.castle.shared.extractFooter
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.internal.logging.LoggerFactory
import org.paseto4j.commons.PrivateKey as PasetoPrivateKey
import org.paseto4j.commons.PublicKey as PasetoPublicKey
import org.paseto4j.commons.SecretKey
import org.paseto4j.commons.Version
import org.paseto4j.version4.Paseto
import org.paseto4j.version4.PasetoLocal
import org.paseto4j.version4.PasetoPublic
import java.security.PrivateKey
import java.security.PublicKey
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Create and validate paseto tokens
 */
class PasetoService(
    private val issuer: String,
    private val audience: String,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        const val DEFAULT_ACCESS_TOKEN_TTL = 900L
        const val DEFAULT_REFRESH_TOKEN_TTL = 604_800L
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun createLocalToken(
        subject: String,
        roles: List<String> = emptyList(),
        permissions: List<String> = emptyList(),
        ttlSeconds: Long = DEFAULT_ACCESS_TOKEN_TTL,
        symmetricKey: ByteArray,
        tokenFooter: TokenFooter? = null,
        implicitAssertion: String = "",
    ): String = try {
        require(subject.isNotBlank()) { "Subject no puede estar vacío" }
        require(ttlSeconds > 0) { "TTL debe ser positivo" }
        require(symmetricKey.size == 32) { "La clave debe ser de 32 bytes" }

        val claims = tokenClaims {
            issuer = this@PasetoService.issuer
            this.subject = subject
            audience = this@PasetoService.audience
            expiresInSeconds = ttlSeconds
            issuedAt = Clock.System.now()
            tokenId = Uuid.random().toString()
            this.roles = roles
            this.permissions = permissions
        }

        val message = objectMapper.writeValueAsString(claims)
        val footer = tokenFooter?.let { objectMapper.writeValueAsString(it) } ?: ""
        val secretKey = SecretKey(symmetricKey, Version.V4)

        logger.debug("Creando token v4.local para subject=$subject")

        PasetoLocal.encrypt(secretKey, message, footer, implicitAssertion)
    } catch (e: Exception) {
        logger.error("Error creando token v4.local", e)
        throw Exception("Error al crear token local", e)
    }

    fun validateLocalToken(
        token: String,
        symmetricKey: ByteArray,
    ): TokenClaims {
        require(token.startsWith("v4.local.")) { "Token inválido: debe ser v4.local" }
        require(symmetricKey.size == 32) { "La clave debe ser de 32 bytes" }

        return try {
            val secretKey = SecretKey(symmetricKey, Version.V4)
            val footer = token.extractFooter()
            val decrypted = PasetoLocal.decrypt(secretKey, token, footer)

            objectMapper.readValue(decrypted, TokenClaims::class.java).also {
                validateClaims(it)
                logger.debug("Token v4.local validado: sub=${it.subject}")
            }
        } catch (e: Exception) {
            logger.error("Error validando token v4.local: ${e.message}", e)

            throw Exception("Error al validar token local", e)
        }
    }

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    fun createPublicToken(
        subject: String,
        roles: List<String> = emptyList(),
        permissions: List<String> = emptyList(),
        ttlSeconds: Long = DEFAULT_ACCESS_TOKEN_TTL,
        privateKey: PrivateKey,
        tokenFooter: TokenFooter? = null,
        implicitAssertion: String = "",
    ): String = try {
        require(subject.isNotBlank()) { "Subject no puede estar vacío" }
        require(ttlSeconds > 0) { "TTL debe ser positivo" }

        val now = Clock.System.now()

        val claims = tokenClaims {
            issuer = this@PasetoService.issuer
            this.subject = subject
            audience = this@PasetoService.audience
            expiresInSeconds = now.plus(ttlSeconds.toDuration(DurationUnit.SECONDS)).epochSeconds
            issuedAt = now
            tokenId = Uuid.random().toString()
            this.roles = roles
            this.permissions = permissions
        }

        val message = objectMapper.writeValueAsString(claims)
        val footer = tokenFooter?.let { objectMapper.writeValueAsString(it) } ?: ""
        val keyPair = PasetoPrivateKey(privateKey, Version.V4)

        logger.debug("Creando token v4.public para subject=$subject")

        Paseto.sign(keyPair, message, footer, implicitAssertion)
    } catch (e: Exception) {
        logger.error("Error creando token v4.public", e)

        throw Exception("Error al crear token público", e)
    }

    fun validatePublicToken(
        token: String,
        publicKey: PublicKey,
        implicitAssertion: String = ""
    ): TokenClaims {
        require(token.startsWith("v4.public.")) { "Token inválido: debe ser v4.public" }

        return try {
            val pubKey = PasetoPublicKey(publicKey, Version.V4)
            val footer = token.extractFooter()
            val parsed = PasetoPublic.parse(pubKey, token, footer, implicitAssertion)

            objectMapper.readValue(parsed, TokenClaims::class.java).also {
                validateClaims(it)
                logger.debug("Token v4.public validado: sub=${it.subject}")
            }

        } catch (e: Exception) {
            logger.error("Error validando token v4.public: ${e.message}", e)
            throw Exception("Error al validar token público", e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun validateClaims(claims: TokenClaims) {
        require(claims.issuer == issuer) { "Issuer inválido: ${claims.issuer}" }
        require(claims.audience == audience) { "Audience inválido: ${claims.audience}" }
        require(!claims.isExpired()) { "Token expirado" }
        require(!claims.isNotYetValid()) { "Token no válido aún" }
    }
}
