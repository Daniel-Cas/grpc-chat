package com.castle.application.usecase

import com.castle.domain.dto.user.CreateUserRequest
import com.castle.domain.entity.User
import com.castle.infrastructure.db.postgres.repository.UserRepository
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

class UserUseCase(
    private val userRepository: UserRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalTime::class)
    suspend fun create(createUserRequest: CreateUserRequest): User = try {
        userRepository.create(
            User(
                username = createUserRequest.username,
                email = createUserRequest.email,
                password = createUserRequest.password,
            )
        )
    } catch (e: Exception) {
        logger.error("[CREATE] Error in creating user", e)

        throw e
    }

    suspend fun getById(userId: String): User? = userRepository.findById(userId)

    suspend fun getByUsername(username: String): User? = userRepository.findByUsername(username)

    suspend fun update() {

    }
    suspend fun delete() {

    }
}