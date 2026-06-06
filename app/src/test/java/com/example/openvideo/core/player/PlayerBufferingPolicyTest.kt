package com.example.openvideo.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerBufferingPolicyTest {

    @Test
    fun localMediaStartsWithSmallPlaybackBuffer() {
        val profile = PlayerBufferingPolicy.profileFor("content://media/external/video/media/42")

        assertEquals(BufferingProfile.Name.LOCAL_FILE, profile.name)
        assertEquals(2_000, profile.minBufferMs)
        assertEquals(20_000, profile.maxBufferMs)
        assertEquals(250, profile.bufferForPlaybackMs)
        assertEquals(500, profile.bufferForPlaybackAfterRebufferMs)
    }

    @Test
    fun progressiveNetworkMediaUsesLargerBuffer() {
        val profile = PlayerBufferingPolicy.profileFor("https://example.com/movie.mp4")

        assertEquals(BufferingProfile.Name.NETWORK_PROGRESSIVE, profile.name)
        assertEquals(15_000, profile.minBufferMs)
        assertEquals(50_000, profile.maxBufferMs)
        assertEquals(1_500, profile.bufferForPlaybackMs)
        assertEquals(3_000, profile.bufferForPlaybackAfterRebufferMs)
    }

    @Test
    fun adaptiveStreamsPreferStreamingBufferProfile() {
        val hlsProfile = PlayerBufferingPolicy.profileFor("https://cdn.example.com/movie/master.m3u8")
        val dashProfile = PlayerBufferingPolicy.profileFor("https://cdn.example.com/movie/manifest.mpd")

        assertEquals(BufferingProfile.Name.ADAPTIVE_STREAM, hlsProfile.name)
        assertEquals(BufferingProfile.Name.ADAPTIVE_STREAM, dashProfile.name)
        assertEquals(20_000, hlsProfile.minBufferMs)
        assertEquals(60_000, hlsProfile.maxBufferMs)
        assertEquals(2_000, hlsProfile.bufferForPlaybackMs)
        assertEquals(4_000, hlsProfile.bufferForPlaybackAfterRebufferMs)
    }

    @Test
    fun adaptiveStreamDetectionIgnoresQueryStrings() {
        val hlsProfile = PlayerBufferingPolicy.profileFor("https://cdn.example.com/live/master.m3u8?token=abc")
        val dashProfile = PlayerBufferingPolicy.profileFor("https://cdn.example.com/movie/manifest.mpd?expires=1")

        assertEquals(BufferingProfile.Name.ADAPTIVE_STREAM, hlsProfile.name)
        assertEquals(BufferingProfile.Name.ADAPTIVE_STREAM, dashProfile.name)
    }

    @Test
    fun rtspStreamsUseDedicatedLowStartupProfile() {
        val profile = PlayerBufferingPolicy.profileFor("rtsp://camera.local/live")

        assertEquals(BufferingProfile.Name.RTSP_STREAM, profile.name)
        assertEquals(5_000, profile.minBufferMs)
        assertEquals(30_000, profile.maxBufferMs)
        assertEquals(1_000, profile.bufferForPlaybackMs)
        assertEquals(2_000, profile.bufferForPlaybackAfterRebufferMs)
    }
}
