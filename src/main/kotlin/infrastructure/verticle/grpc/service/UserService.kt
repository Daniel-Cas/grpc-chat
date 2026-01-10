package com.castle.infrastructure.verticle.grpc.service

import com.castle.application.security.crypto.Cipher
import com.castle.application.usecase.UserUseCase
import com.castle.domain.dto.user.CreateUserRequest
import io.grpc.Status
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.LoggerFactory
import user.v1.User
import user.v1.UserServiceGrpcKt
import user.v1.createUserResponse
import java.util.concurrent.Executors

class UserService(
    private val cipher: Cipher,
    private val userUseCase: UserUseCase,
) : UserServiceGrpcKt.UserServiceCoroutineImplBase(
    coroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun createUser(request: User.CreateUserRequest): User.CreateUserResponse = createUserResponse {
        try {
            val user = userUseCase.create(
                CreateUserRequest(
                    username = request.username,
                    email = request.email,
                    password = cipher.encrypt(request.password),
                )
            )

            id = user.id
            username = user.username
            email = user.email
        } catch (e: Exception) {
            logger.error(e.message, e)

            throw Status.ALREADY_EXISTS.withDescription("Invalid credentials").asException()
        }
    }
}