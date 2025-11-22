package com.castle.application.security.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

class Cipher(
    algorithm: String,
    keyBytes: ByteArray,
) {
    private val secretKey: SecretKeySpec
    private val algorithmWithoutPadding: String = "$algorithm/GCM/NoPadding"

    init {
        require(keyBytes.size == 32) { "Key must be 32 bytes" }

        secretKey = SecretKeySpec(keyBytes, algorithm)
    }

    fun encrypt(plainText: String, cipherText: ByteArray): String {
        val initializationVector = ByteArray(12).apply {
            SecureRandom().nextBytes(this)
        }
        val cipher = Cipher.getInstance(algorithmWithoutPadding).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, initializationVector))
        }
        val encryptedData = cipher.doFinal(plainText.toByteArray())

        return Base64.encode(initializationVector + encryptedData)
    }

    fun decrypt(cipherText: ByteArray): String {
        val decodedData = Base64.decode(cipherText)
        val initializationVector = decodedData.copyOfRange(0, 12)
        val encryptedData = decodedData.copyOfRange(12, decodedData.size)
        val cipher = Cipher.getInstance(algorithmWithoutPadding).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, initializationVector))
        }

        return cipher.doFinal(encryptedData).toString(Charsets.UTF_8)
    }
}
