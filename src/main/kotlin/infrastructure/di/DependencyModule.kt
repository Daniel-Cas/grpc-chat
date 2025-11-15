package com.castle.infrastructure.di

import com.castle.application.service.UserService
import com.castle.application.service.auth.AuthService
import com.castle.application.service.auth.KeyManager
import com.castle.application.service.auth.PasetoService
import com.castle.application.service.chat.ChatService
import com.castle.infrastructure.verticle.grpc.interceptor.AuthInterceptor
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.json.JsonObject

object DependencyModule {

    fun initialize(config: JsonObject) {
        val authConfig = config.getJsonObject("auth").getJsonObject("paseto")
        val symmetricKey = authConfig.getString("symmetric-key")

        ServiceRegistry {
            single { ObjectMapper().apply { findAndRegisterModules() } }

            single {
                PasetoService(
                    issuer = authConfig.getString("issuer"),
                    audience = authConfig.getString("audience"),
                    objectMapper = get<ObjectMapper>(),
                )
            }

            single {
                AuthInterceptor(
                    pasetoService = get<PasetoService>(),
                    symmetricKey = KeyManager.symmetricKeyFromBase64(symmetricKey),
                    publicKeyBase64 = authConfig.getString("public-key"),
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

            single { ChatService(get()) }
        }
    }
}
