package com.castle.domain.dto.auth

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@OptIn(ExperimentalTime::class)
data class TokenClaims(
    @param:JsonProperty("iss") val issuer: String,
    @param:JsonProperty("sub") val subject: String,
    @param:JsonProperty("aud") val audience: String,
    @param:JsonProperty("exp") val expiration: String,
    @param:JsonProperty("nbf") val notBefore: String? = null,
    @param:JsonProperty("iat") val issuedAt: String,
    @param:JsonProperty("jti") val tokenId: String,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
) {
    fun hasRole(role: String) = role in roles
    fun hasPermission(permission: String) = permission in permissions
    fun hasAnyRole(vararg roles: String) = roles.any { it in roles }
    fun hasAllRoles(vararg roles: String) = roles.all { it in roles }
    fun hasAnyPermission(vararg perms: String) = perms.any { it in permissions }
    fun hasAllPermissions(vararg perms: String) = perms.all { it in permissions }
    fun isExpired() = Clock.System.now() > Instant.parse(expiration)
    fun isNotYetValid() = notBefore?.let { Clock.System.now() < Instant.parse(it) } ?: false
}
