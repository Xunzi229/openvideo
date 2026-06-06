package com.example.openvideo.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkUrlPolicyTest {

    @Test
    fun normalizesSupportedPlaybackUrls() {
        assertEquals(
            NetworkUrlPolicy.Validation.Valid("https://example.com/video.m3u8"),
            NetworkUrlPolicy.validatePlaybackUrl("  <HTTPS://Example.COM/video.m3u8>  ")
        )
        assertEquals(
            NetworkUrlPolicy.Validation.Valid("rtsp://camera.local/live"),
            NetworkUrlPolicy.validatePlaybackUrl("\"rtsp://Camera.Local/live\"")
        )
        assertEquals(
            NetworkUrlPolicy.Validation.Valid("http://example.com/video.mp4?quality=720p"),
            NetworkUrlPolicy.validatePlaybackUrl("http://example.com/video.mp4?quality=720p")
        )
    }

    @Test
    fun rejectsUnsafeOrUnsupportedUrls() {
        assertInvalid("", NetworkUrlPolicy.Error.EMPTY)
        assertInvalid("example.com/video.mp4", NetworkUrlPolicy.Error.MISSING_SCHEME)
        assertInvalid("ftp://example.com/video.mp4", NetworkUrlPolicy.Error.UNSUPPORTED_SCHEME)
        assertInvalid("https:///video.mp4", NetworkUrlPolicy.Error.MISSING_HOST)
        assertInvalid("https://example.com/a bad.mp4", NetworkUrlPolicy.Error.ILLEGAL_CHARACTER)
        assertInvalid("https://example.com/video.mp4\nAuthorization: token", NetworkUrlPolicy.Error.ILLEGAL_CHARACTER)
    }

    private fun assertInvalid(input: String, error: NetworkUrlPolicy.Error) {
        val result = NetworkUrlPolicy.validatePlaybackUrl(input)
        assertTrue("Expected invalid result for $input, got $result", result is NetworkUrlPolicy.Validation.Invalid)
        assertEquals(error, (result as NetworkUrlPolicy.Validation.Invalid).error)
    }
}
