package com.castle.application.service.chat

import chat.v1.Chat
import chat.v1.ChatServiceGrpcKt
import chat.v1.connected
import chat.v1.serverMessage
import com.castle.application.service.UserService
import io.vertx.core.internal.logging.LoggerFactory
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.Executors
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatService(
    private val userService: UserService,
) : ChatServiceGrpcKt.ChatServiceCoroutineImplBase(
    coroutineContext = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher(),
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalUuidApi::class)
    override fun streamChat(requests: Flow<Chat.ClientMessage>): Flow<Chat.ServerMessage> = flow {
        try {
            requests.collect { request ->
                when {
                    request.hasConnect() -> {
                        logger.info("Connect request: ${request.connect.userId}")
                        val userExists = userService.existsUserById(request.connect.userId)
                        val userSession = if (userExists) Uuid.random().toString() else ""

                        emit(
                            serverMessage {
                                connected = connected {
                                    success = userExists
                                    sessionId = userSession
                                }
                            }
                        )
                    }

                    request.hasSendText() -> {
                        logger.info("Received text request")
                        emit(
                            Chat.ServerMessage.newBuilder().setTextReceived(Chat.TextReceived.getDefaultInstance())
                                .build()
                        )
                    }

                    else -> {
                        logger.info("Received unknown request")
                        emit(Chat.ServerMessage.newBuilder().setError(Chat.Error.getDefaultInstance()).build())
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error streaming chat")
        }
    }
}