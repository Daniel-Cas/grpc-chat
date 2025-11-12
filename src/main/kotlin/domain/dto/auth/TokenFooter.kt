package com.castle.domain.dto.auth

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TokenFooter(
    @param:JsonProperty("kid") val keyId: String,
    @param:JsonProperty("ver") val version: String = "1.0",
    @param:JsonProperty("typ") val type: String = "access",
)
