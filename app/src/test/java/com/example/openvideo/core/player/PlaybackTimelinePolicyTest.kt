package com.example.openvideo.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackTimelinePolicyTest {

    @Test
    fun relativeSeekClampsWithinSeekableVodDuration() {
        assertEquals(
            120_000L,
            PlaybackTimelinePolicy.relativeSeekTarget(
                currentPositionMs = 115_000L,
                durationMs = 120_000L,
                deltaMs = 10_000L
            )
        )
        assertEquals(
            0L,
            PlaybackTimelinePolicy.relativeSeekTarget(
                currentPositionMs = 5_000L,
                durationMs = 120_000L,
                deltaMs = -10_000L
            )
        )
    }

    @Test
    fun relativeSeekRejectsUnknownOrLiveDuration() {
        assertNull(PlaybackTimelinePolicy.relativeSeekTarget(5_000L, 0L, 10_000L))
        assertNull(PlaybackTimelinePolicy.relativeSeekTarget(5_000L, Long.MAX_VALUE, 10_000L))
        assertNull(
            PlaybackTimelinePolicy.relativeSeekTarget(
                currentPositionMs = 5_000L,
                durationMs = -9_223_372_036_854_775_807L,
                deltaMs = 10_000L
            )
        )
    }
}
