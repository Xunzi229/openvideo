package com.example.openvideo.ui.sources

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceCredentialPrivacyPolicyTest {

    @Test
    fun buildNoticeExplainsStorageExportAndDiagnosticsPolicy() {
        val notice = SourceCredentialPrivacyPolicy.buildNotice(
            labels = SourceCredentialPrivacyLabels.englishDefaults()
        )

        assertEquals("Credential privacy", notice.title)
        assertEquals(
            listOf(
                "Passwords and tokens are stored with Android Keystore backed encrypted storage.",
                "Exports include source names and addresses only; passwords, tokens, cookies, and headers are excluded.",
                "Diagnostics and crash reports must redact full URLs, usernames, passwords, tokens, cookies, and headers."
            ),
            notice.items
        )
    }
}
