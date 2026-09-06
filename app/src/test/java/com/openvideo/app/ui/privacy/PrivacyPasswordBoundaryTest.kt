package com.openvideo.app.ui.privacy

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PrivacyPasswordBoundaryTest {
    @Test fun malformedIterationsSaltAndHashesNeverAuthenticate() {
        val salt = "01".repeat(16)
        val hash = "ab".repeat(32)
        val malformed = listOf("", "unknown:1:$salt:$hash", "pbkdf2-sha1:x:$salt:$hash",
            "pbkdf2-sha1:0:$salt:$hash", "pbkdf2-sha1:1000001:$salt:$hash",
            "pbkdf2-sha1:1:0:$hash", "pbkdf2-sha1:1:zz:$hash", "pbkdf2-sha1:1:$salt:0",
            "pbkdf2-sha1:1:$salt:zz", "legacy:zz") +
            listOf(0, 7, 65).map { "pbkdf2-sha1:1:${"01".repeat(it)}:$hash" } +
            listOf(0, 15, 65).map { "pbkdf2-sha1:1:$salt:${"ab".repeat(it)}" }
        malformed.forEach { assertEquals(it, PrivacyPasswordPolicy.Verification(false, false), PrivacyPasswordPolicy.verify("secret", it)) }
    }

    @Test fun olderHashUpgradesAndRandomSaltsProduceDistinctValidCredentials() {
        val salt = ByteArray(8) { 1 }
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(PBEKeySpec("secret".toCharArray(), salt, 1, 256)).encoded
        val stored = "pbkdf2-sha1:1:${"01".repeat(8)}:${key.joinToString("") { "%02x".format(it) }}"
        assertEquals(PrivacyPasswordPolicy.Verification(true, true), PrivacyPasswordPolicy.verify("secret", stored))
        val first = PrivacyPasswordPolicy.encode("secret")
        val second = PrivacyPasswordPolicy.encode("secret")
        assertNotEquals(first, second)
        assertEquals(PrivacyPasswordPolicy.Verification(true, false), PrivacyPasswordPolicy.verify("secret", first))
    }
}
