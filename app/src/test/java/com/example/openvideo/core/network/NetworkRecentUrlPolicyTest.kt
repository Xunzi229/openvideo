package com.example.openvideo.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NetworkRecentUrlPolicyTest {

    @Test
    fun displayUrlRedactsSensitiveQueryValues() {
        val display = NetworkRecentUrlPolicy.displayUrlFor(
            "https://Example.com/video.m3u8?token=secret&quality=720p&sign=abc&auth_key=private"
        )

        assertEquals(
            "https://example.com/video.m3u8?token=redacted&quality=720p&sign=redacted&auth_key=redacted",
            display
        )
        assertFalse(display.contains("secret"))
        assertFalse(display.contains("abc"))
        assertFalse(display.contains("private"))
    }

    @Test
    fun titleUsesLastPathSegmentOrHostFallback() {
        assertEquals(
            "movie.mp4",
            NetworkRecentUrlPolicy.titleFor("https://cdn.example.com/path/movie.mp4?token=secret")
        )
        assertEquals(
            "camera.local",
            NetworkRecentUrlPolicy.titleFor("rtsp://camera.local/")
        )
    }
}
