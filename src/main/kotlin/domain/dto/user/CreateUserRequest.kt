package com.castle.domain.dto.user

data class CreateUserRequest(
    val username: String,
    val email: String,
    val password: String
)
