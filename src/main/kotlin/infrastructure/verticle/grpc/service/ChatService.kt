package com.castle.infrastructure.verticle.grpc.service

import chat.v1.Chat
import chat.v1.ChatServiceGrpcKt
import chat.v1.chatMemberResponse
import chat.v1.chatMessage
import chat.v1.chatStreamResponse
import chat.v1.createChatResponse
import chat.v1.messageCreatedEvent
import chat.v1.subscriptionResponse
import chat.v1.userProfile
import com.castle.application.usecase.ChatMemberUseCase
import com.castle.application.usecase.CreateChatUseCase
import com.castle.application.usecase.CreateMessageUseCase
import com.castle.domain.dto.request.CreateChatRequest
import com.castle.domain.entity.ChatMember
import com.castle.domain.entity.Message
import com.castle.domain.enums.ChatRole
import com.castle.domain.enums.ChatType
import com.castle.domain.enums.ChatVisibility
import com.castle.infrastructure.db.redis.repository.RedisChatRepository
import com.castle.infrastructure.verticle.grpc.interceptor.AuthInterceptor
import com.castle.shared.getOrThrow
import com.castle.shared.grpcResponse
import com.google.protobuf.Timestamp
import io.grpc.Status
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import types.v1.Enums
import types.v1.timestamp
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class ChatService(
    private val chatMemberUseCase: ChatMemberUseCase,
    private val createChatUseCase: CreateChatUseCase,
    private val redisChatRepository: RedisChatRepository,
    private val createMessageUseCase: CreateMessageUseCase,
) : ChatServiceGrpcKt.ChatServiceCoroutineImplBase(
    coroutineContext = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher(),
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalTime::class)
    override suspend fun createChat(request: Chat.CreateChatRequest): Chat.CreateChatResponse = grpcResponse {
        createChatResponse {
            val userId = AuthInterceptor.CLAIMS_CONTEXT_KEY.get().subject

            logger.info("[CREATE] Creating chat requested by user {}", userId)

            val createChatRequest = CreateChatRequest(
                createdBy = userId.toLong(),
                description = request.description,
                memberIds = request.memberIdsList.toSet(),
                name = request.name,
                type = ChatType.valueOf(request.type.name),
                visibility = ChatVisibility.valueOf(request.visibility.name),
            )
            val chat = createChatUseCase(createChatRequest).getOrThrow()
            val memberIds = createChatRequest.memberIds + setOf(userId.toLong())
            val chatMemberRequest = memberIds.map {
                ChatMember(
                    chatId = chat.id,
                    role = if (it == userId.toLong()) ChatRole.OWNER else ChatRole.MEMBER,
                    userId = it,
                )
            }

            val chatMembers = chatMemberUseCase(chatMemberRequest).getOrThrow()

            created = true
            chatId = chat.id
            type = Enums.ChatType.valueOf(chat.type.name)
            members.addAll(
                chatMembers.map { member ->
                    chatMemberResponse {
                        this.userId = member.userId
                        role = Enums.ChatRole.valueOf(member.role.name)
                    }
                }
            )
            createdAt = timestamp {
                value = Timestamp.newBuilder()
                    .setSeconds(chat.createdAt.epochSeconds)
                    .setNanos(chat.createdAt.nanosecondsOfSecond)
                    .build()
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override fun stream(requests: Flow<Chat.ChatStreamRequest>): Flow<Chat.ChatStreamResponse> = flow {
        val userId = AuthInterceptor.CLAIMS_CONTEXT_KEY.get().subject.toLong()

        requests.collect { request ->
            when {
                request.hasSubscription() -> emit(
                    chatStreamResponse {
                        subscription = subscriptionResponse {
                            val subscribers = redisChatRepository.getSubscribers(request.subscription.chatId)

                            if (userId !in subscribers) {
                                val chatMember = chatMemberUseCase.findByChatIdAndUserId(
                                    chatId = request.subscription.chatId,
                                    userId = userId,
                                )

                                if (chatMember.isEmpty()) {
                                    throw Status.NOT_FOUND.withDescription("No chat member found").asException()
                                }

                                val session = redisChatRepository.addSubscriber(request.subscription.chatId, userId)

                                requireNotNull(session) { "Failed saving session" }

                                subscribed = true
                                members.addAll(
                                    redisChatRepository.getSubscribers(request.subscription.chatId).map {
                                        userProfile {
                                            id = it
                                        }
                                    }
                                )
                            } else {
                                subscribed = true
                                members.addAll(
                                    redisChatRepository.getSubscribers(request.subscription.chatId).map {
                                        userProfile {
                                            id = it
                                        }
                                    }
                                )
                            }
                        }
                    }
                )

                request.hasSendMessage() -> messageCreatedEvent {
                    val clientId = request.sendMessage.clientMessageId
                    val chatId = request.sendMessage.chatId
                    val senderId = request.sendMessage.senderId
                    val content = request.sendMessage.content

                    logger.info("[CHAT] New message from client: {}", clientId)

                    message = chatMessage {
                        val message = createMessageUseCase(
                            Message(
                                chatId = chatId,
                                senderId = senderId,
                                content = content,
                            )
                        ).getOrThrow()

                        messageCreatedEvent {
                            clientMessageId = message.clientMessageId ?: ""
                            chatMessage {
                                id = message.id
                                this.chatId = message.chatId

                                sender = userProfile {
                                    id = userId
                                    username = ""
                                    avatar = ""
                                }

                                this.content = content
                                createdAt = timestamp {
                                    value = Timestamp.newBuilder()
                                        .setSeconds(message.sentAt.epochSeconds)
                                        .setNanos(message.sentAt.nanosecondsOfSecond)
                                        .build()                                }
                            }
                        }
                    }
                }
            }
        }
    }
}