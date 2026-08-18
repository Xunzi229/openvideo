package com.example.openvideo.ui.privacy

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PrivacyPasswordPolicy {
    private const val FORMAT = "pbkdf2-sha1"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val MAX_ACCEPTED_ITERATIONS = 1_000_000

    data class Verification(val valid: Boolean, val needsUpgrade: Boolean)

    fun encode(password: String, salt: ByteArray = randomSalt()): String {
        val hash = derive(password, salt, ITERATIONS)
        return "$FORMAT:$ITERATIONS:${salt.toHex()}:${hash.toHex()}"
    }

    fun verify(password: String, stored: String): Verification {
        val parts = stored.split(':')
        if (parts.size == 4 && parts[0] == FORMAT) {
            val iterations = parts[1].toIntOrNull()
                ?.takeIf { it in 1..MAX_ACCEPTED_ITERATIONS }
                ?: return Verification(false, false)
            val salt = parts[2].hexToBytesOrNull() ?: return Verification(false, false)
            val expected = parts[3].hexToBytesOrNull() ?: return Verification(false, false)
            if (salt.size !in 8..64 || expected.size !in 16..64) return Verification(false, false)
            val actual = derive(password, salt, iterations)
            return Verification(
                valid = MessageDigest.isEqual(actual, expected),
                needsUpgrade = iterations < ITERATIONS
            )
        }

        if (parts.size == 2) {
            val expected = parts[1].hexToBytesOrNull() ?: return Verification(false, false)
            val actual = MessageDigest.getInstance("SHA-256")
                .digest((parts[0] + password).toByteArray(Charsets.UTF_8))
            return Verification(
                valid = MessageDigest.isEqual(actual, expected),
                needsUpgrade = true
            )
        }
        return Verification(false, false)
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun randomSalt(): ByteArray = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0 || any { it.digitToIntOrNull(16) == null }) return null
        return ByteArray(length / 2) { index -> substring(index * 2, index * 2 + 2).toInt(16).toByte() }
    }
}
