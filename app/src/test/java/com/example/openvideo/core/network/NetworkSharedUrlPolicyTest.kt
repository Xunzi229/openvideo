package com.example.openvideo.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkSharedUrlPolicyTest {

    @Test
    fun sendTextExtractsFirstSupportedPlaybackUrl() {
        assertEquals(
            "https://example.com/video.m3u8?quality=720p",
            NetworkSharedUrlPolicy.extractPlaybackUrl(
                action = "android.intent.action.SEND",
                mimeType = "text/plain",
                sharedText = "Watch later: https://Example.com/video.m3u8?quality=720p",
                dataString = null
            )
        )
    }

    @Test
    fun viewIntentUsesDataUrl() {
        assertEquals(
            "rtsp://camera.local/live",
            NetworkSharedUrlPolicy.extractPlaybackUrl(
                action = "android.intent.action.VIEW",
                mimeType = null,
                sharedText = null,
                dataString = "RTSP://Camera.Local/live"
            )
        )
    }

    @Test
    fun unsupportedOrMalformedSharesReturnNull() {
        assertNull(
            NetworkSharedUrlPolicy.extractPlaybackUrl(
                action = "android.intent.action.SEND",
                mimeType = "text/plain",
                sharedText = "ftp://example.com/video.mp4",
                dataString = null
            )
        )
        assertNull(
            NetworkSharedUrlPolicy.extractPlaybackUrl(
                action = "android.intent.action.SEND",
                mimeType = "image/png",
                sharedText = "https://example.com/video.mp4",
                dataString = null
            )
        )
    }
}
