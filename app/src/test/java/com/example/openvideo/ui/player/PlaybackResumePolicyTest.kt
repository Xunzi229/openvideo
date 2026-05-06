package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackResumePolicyTest {

    @Test
    fun restoresSavedPositionBeforeTheEndingWindow() {
        val target = PlaybackResumePolicy.restoreTarget(
            savedPositionMs = 120_000,
            durationMs = 300_000
        )

        assertEquals(120_000L, target)
    }

    @Test
    fun doesNotRestoreWhenSavedPositionIsNearTheEnd() {
        val target = PlaybackResumePolicy.restoreTarget(
            savedPositionMs = 295_000,
            durationMs = 300_000
        )

        assertNull(target)
    }

    @Test
    fun restoresWhenDurationIsNotKnownYet() {
        val target = PlaybackResumePolicy.restoreTarget(
            savedPositionMs = 42_000,
            durationMs = 0
        )

        assertEquals(42_000L, target)
    }

    @Test
    fun ignoresInvalidSavedPositions() {
        assertNull(PlaybackResumePolicy.restoreTarget(savedPositionMs = 0, durationMs = 300_000))
        assertNull(PlaybackResumePolicy.restoreTarget(savedPositionMs = -1, durationMs = 300_000))
    }
}
