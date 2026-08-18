package com.example.openvideo.ui.privacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PrivacyPasswordPolicyTest {

    @Test
    fun pbkdf2PasswordFormatVerifiesWithoutRequestingUpgrade() {
        val encoded = PrivacyPasswordPolicy.encode("correct horse", ByteArray(16) { it.toByte() })

        assertTrue(encoded.startsWith("pbkdf2-sha1:120000:"))
        assertTrue(PrivacyPasswordPolicy.verify("correct horse", encoded).valid)
        assertFalse(PrivacyPasswordPolicy.verify("correct horse", encoded).needsUpgrade)
        assertFalse(PrivacyPasswordPolicy.verify("wrong", encoded).valid)
    }

    @Test
    fun legacySha256FormatStillVerifiesAndRequestsMigration() {
        val salt = "00112233445566778899aabbccddeeff"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest((salt + "legacy-password").toByteArray())
            .joinToString("") { "%02x".format(it) }

        val result = PrivacyPasswordPolicy.verify("legacy-password", "$salt:$hash")

        assertTrue(result.valid)
        assertTrue(result.needsUpgrade)
    }
}
