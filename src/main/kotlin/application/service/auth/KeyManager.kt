package com.castle.application.service.auth

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.util.Base64

/**
 * Manage generation and storage of keys to Paseto v4.
 */
object KeyManager {
    private val secureRandom = SecureRandom.getInstanceStrong()

    fun generateSymmetricKey(): ByteArray {
        val keyBytes = ByteArray(32)
        secureRandom.nextBytes(keyBytes)

        return keyBytes
    }

    fun symmetricKeyToBase64(key: ByteArray): String {
        require(key.size == 32) { "La clave debe ser de 32 bytes" }

        return Base64.getEncoder().encodeToString(key)
    }

    fun symmetricKeyFromBase64(base64: String): ByteArray {
        val key = Base64.getDecoder().decode(base64)
        require(key.size == 32) { "La clave debe ser de 32 bytes" }

        return key
    }

    fun generateAsymmetricKeyPair(): KeyPair {
        val keyPairGen = KeyPairGenerator.getInstance("Ed25519")

        return keyPairGen.generateKeyPair()
    }

    fun privateKeyToBase64(keyPair: KeyPair): String = Base64.getEncoder().encodeToString(keyPair.private.encoded)

    fun publicKeyToBase64(keyPair: KeyPair): String = Base64.getEncoder().encodeToString(keyPair.public.encoded)

    fun keyPairFromBase64(privateKeyBase64: String, publicKeyBase64: String): KeyPair {
        val privateBytes = Base64.getDecoder().decode(privateKeyBase64)
        val publicBytes = Base64.getDecoder().decode(publicKeyBase64)

        val keyFactory = java.security.KeyFactory.getInstance("Ed25519")

        val privateKeySpec = java.security.spec.PKCS8EncodedKeySpec(privateBytes)
        val privateKey = keyFactory.generatePrivate(privateKeySpec)

        val publicKeySpec = java.security.spec.X509EncodedKeySpec(publicBytes)
        val publicKey = keyFactory.generatePublic(publicKeySpec)

        return KeyPair(publicKey, privateKey)
    }

    fun publicKeyFromBase64(publicKeyBase64: String): PublicKey {
        val publicBytes = Base64.getDecoder().decode(publicKeyBase64)
        val keyFactory = java.security.KeyFactory.getInstance("Ed25519")
        val publicKeySpec = java.security.spec.X509EncodedKeySpec(publicBytes)

        return keyFactory.generatePublic(publicKeySpec)
    }
}
