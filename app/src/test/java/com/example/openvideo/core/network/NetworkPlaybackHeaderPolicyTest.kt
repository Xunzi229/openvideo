package com.example.openvideo.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NetworkPlaybackHeaderPolicyTest {

    @Test
    fun userAgentIdentifiesOpenVideoWithoutSensitiveDeviceDetails() {
        assertEquals(
            "OpenVideo/0.4.0 (Android)",
            NetworkPlaybackHeaderPolicy.userAgent("0.4.0")
        )
        assertEquals(
            "OpenVideo (Android)",
            NetworkPlaybackHeaderPolicy.userAgent("")
        )
    }

    @Test
    fun defaultRequestPropertiesOnlyIncludeValidHttpReferer() {
        assertEquals(
            mapOf("Referer" to "https://example.com/library"),
            NetworkPlaybackHeaderPolicy.defaultRequestProperties("HTTPS://Example.COM/library")
        )
        assertEquals(emptyMap<String, String>(), NetworkPlaybackHeaderPolicy.defaultRequestProperties(null))
        assertEquals(emptyMap<String, String>(), NetworkPlaybackHeaderPolicy.defaultRequestProperties("ftp://example.com"))
        assertEquals(emptyMap<String, String>(), NetworkPlaybackHeaderPolicy.defaultRequestProperties("https://user:pass@example.com"))
    }

    @Test
    fun diagnosticHeadersRedactSensitiveValues() {
        val safe = NetworkPlaybackHeaderPolicy.redactForDiagnostics(
            mapOf(
                "User-Agent" to "OpenVideo/0.4.0 (Android)",
                "Authorization" to "Bearer secret",
                "Cookie" to "session=secret",
                "X-Api-Key" to "secret",
                "Referer" to "https://example.com"
            )
        )

        assertEquals("OpenVideo/0.4.0 (Android)", safe["User-Agent"])
        assertEquals("https://example.com", safe["Referer"])
        assertEquals("redacted", safe["Authorization"])
        assertEquals("redacted", safe["Cookie"])
        assertEquals("redacted", safe["X-Api-Key"])
        assertFalse(safe.values.any { it.contains("secret") })
    }
}
