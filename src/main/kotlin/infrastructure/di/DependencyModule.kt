package com.castle.infrastructure.di

import com.castle.application.security.crypto.Cipher
import com.castle.application.service.UserService
import com.castle.application.service.auth.AuthService
import com.castle.application.service.auth.KeyManager
import com.castle.application.service.auth.PasetoService
import com.castle.application.service.chat.ChatService
import com.castle.infrastructure.config.model.AppConfig
import com.castle.infrastructure.db.postgres.Flyway
import com.castle.infrastructure.db.postgres.Postgresql
import com.castle.infrastructure.verticle.grpc.interceptor.AuthInterceptor
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

            single {
                AuthService(
                    keyPair = KeyManager.generateAsymmetricKeyPair(),
                    pasetoService = get<PasetoService>(),
                    symmetricKey = KeyManager.symmetricKeyFromBase64(symmetricKey),
                )
            }

            single { UserService() }

            single { ChatService(get<UserService>()) }
        }
    }

    fun initializeMapper() = ServiceRegistry {
        single { DatabindCodec.mapper().registerModule(KotlinModule.Builder().build()) }
    }
}
