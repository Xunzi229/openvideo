package com.example.openvideo.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackNotificationSeekPolicyTest {

    @Test
    fun seekPositionUsesZoneCenterFraction() {
        val durationMs = 120_000L
        assertEquals(5_000L, PlaybackNotificationSeekPolicy.seekPositionMs(0, durationMs))
        assertEquals(65_000L, PlaybackNotificationSeekPolicy.seekPositionMs(6, durationMs))
        assertEquals(115_000L, PlaybackNotificationSeekPolicy.seekPositionMs(11, durationMs))
    }

    @Test
    fun seekPositionClampsInvalidInput() {
        assertEquals(0L, PlaybackNotificationSeekPolicy.seekPositionMs(-1, 0L))
        assertEquals(
            PlaybackNotificationSeekPolicy.seekPositionMs(11, 100_000L),
            PlaybackNotificationSeekPolicy.seekPositionMs(99, 100_000L)
        )
    }
}
