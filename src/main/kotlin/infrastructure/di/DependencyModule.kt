package com.castle.infrastructure.di

import com.castle.application.security.crypto.Cipher
import com.castle.application.service.auth.KeyManager
import com.castle.application.service.auth.PasetoService
import com.castle.application.service.user.ChatUseCase
import com.castle.application.usecase.ChatMemberUseCase
import com.castle.application.usecase.CreateChatUseCase
import com.castle.application.usecase.CreateMessageUseCase
import com.castle.application.usecase.UserUseCase
import com.castle.infrastructure.config.model.AppConfig
import com.castle.infrastructure.db.postgres.Flyway
import com.castle.infrastructure.db.postgres.Postgresql
import com.castle.infrastructure.db.postgres.repository.ChatMemberRepository
import com.castle.infrastructure.db.postgres.repository.ChatRepository
import com.castle.infrastructure.db.postgres.repository.MessageRepository
import com.castle.infrastructure.db.postgres.repository.UserRepository
import com.castle.infrastructure.db.redis.Redis
import com.castle.infrastructure.db.redis.repository.RedisChatRepository
import com.castle.infrastructure.verticle.grpc.interceptor.AuthInterceptor
import com.castle.infrastructure.verticle.grpc.service.AuthService
import com.castle.infrastructure.verticle.grpc.service.ChatService
import com.castle.infrastructure.verticle.grpc.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.Vertx
import io.vertx.core.json.jackson.DatabindCodec
import kotlin.io.encoding.Base64

object DependencyModule {
    fun initialize(config: AppConfig, vertx: Vertx) {
        val authConfig = config.auth.paseto
        val databaseConfig = config.database
        val symmetricKey = authConfig.symmetricKey
        val cryptoConfig = config.security.crypto

        ServiceRegistry {
            single { config }
            single { Flyway(databaseConfig) }
            single { Postgresql(databaseConfig.postgres, vertx) }
            single { Redis(databaseConfig.redis, vertx) }
            single {
                Cipher(
                    algorithm = cryptoConfig.algorithm,
                    keyBytes = Base64.decode(cryptoConfig.secretKey),
                )
            }
            single {
                PasetoService(
                    audience = authConfig.audience,
                    issuer = authConfig.issuer,
                    objectMapper = get<ObjectMapper>(),
                )
            }

            single {
                AuthInterceptor(
                    pasetoService = get<PasetoService>(),
                    publicKeyBase64 = authConfig.publickey,
                    symmetricKey = KeyManager.symmetricKeyFromBase64(symmetricKey),
                )
            }

            single { UserRepository(get<Postgresql>()) }
            single { UserUseCase(get<UserRepository>()) }
            single { UserService(get<Cipher>(), get<UserUseCase>()) }
            single { ChatRepository(get<Postgresql>()) }
            single { ChatUseCase(get<ChatRepository>()) }
            single { ChatMemberRepository(get<Postgresql>()) }
            single { ChatMemberUseCase(get<ChatMemberRepository>()) }
            single { RedisChatRepository(get()) }
            single { CreateChatUseCase(get()) }
            single { MessageRepository(get()) }
            single { CreateMessageUseCase(get()) }
            single { ChatService(get<ChatMemberUseCase>(), get(), get(), get()) }

            single {
                AuthService(
                    cipher = get<Cipher>(),
                    keyPair = KeyManager.generateAsymmetricKeyPair(),
                    pasetoService = get<PasetoService>(),
                    symmetricKey = KeyManager.symmetricKeyFromBase64(symmetricKey),
                    userUseCase = get<UserUseCase>(),
                )
            }
        }
    }

    fun initializeMapper() = ServiceRegistry {
        single { DatabindCodec.mapper().registerModule(KotlinModule.Builder().build()) }
    }
}
