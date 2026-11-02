package com.example.varsivo

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt.toHex()
    }

    fun hashPassword(password: String, salt: String): String {
        val saltBytes = salt.hexToByteArray()

        val spec = PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            120_000,
            256
        )

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded

        return hash.toHex()
    }

    fun verifyPassword(
        password: String,
        storedHash: String,
        salt: String
    ): Boolean {
        val newHash = hashPassword(password, salt)

        return MessageDigest.isEqual(
            newHash.toByteArray(),
            storedHash.toByteArray()
        )
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.hexToByteArray(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}