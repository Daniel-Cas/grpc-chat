package com.castle.domain.dto.builder

import com.castle.domain.dto.auth.TokenClaims
import com.castle.domain.dto.dsl.TokenClaimsDsl
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@TokenClaimsDsl
class TokenClaimsBuilder {
    var issuer: String = ""
    var subject: String = ""
    var audience: String = ""
    var expiresInSeconds: Long = 3600
    var notBefore: Instant? = null
    var issuedAt: Instant = Clock.System.now()
    var tokenId: String = Uuid.random().toString()
    var roles: List<String> = emptyList()
    var permissions: List<String> = emptyList()

    fun build(): TokenClaims {
        require(issuer.isNotBlank()) { "Issuer cannot be empty" }
        require(subject.isNotBlank()) { "Subject cannot be empty" }
        require(audience.isNotBlank()) { "Audience cannot be empty" }

        val expiration = issuedAt.plus(expiresInSeconds.toDuration(DurationUnit.SECONDS))

        return TokenClaims(
            issuer = issuer,
            subject = subject,
            audience = audience,
            expiration = expiration.toString(),
            notBefore = notBefore?.toString(),
            issuedAt = issuedAt.toString(),
            tokenId = tokenId,
            roles = roles,
            permissions = permissions,
        )
    }
}

@OptIn(ExperimentalTime::class)
inline fun tokenClaims(block: TokenClaimsBuilder.() -> Unit): TokenClaims = TokenClaimsBuilder().apply(block).build()
