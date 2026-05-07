package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntroOutroSkipPolicyTest {

    @Test
    fun skipsIntroWhenPlaybackStartsBeforeIntroEnd() {
        val target = IntroOutroSkipPolicy.skipTarget(
            enabled = true,
            currentPositionMs = 500,
            durationMs = 120_000,
            introSeconds = 30,
            outroSeconds = 0,
            hasSkippedIntro = false,
            hasSkippedOutro = false
        )

        assertEquals(IntroOutroSkipPolicy.Target(30_000, IntroOutroSkipPolicy.Kind.INTRO), target)
    }

    @Test
    fun doesNotRewindWhenRestoredPastIntro() {
        val target = IntroOutroSkipPolicy.skipTarget(
            enabled = true,
            currentPositionMs = 45_000,
            durationMs = 120_000,
            introSeconds = 30,
            outroSeconds = 0,
            hasSkippedIntro = false,
            hasSkippedOutro = false
        )

        assertNull(target)
    }

    @Test
    fun skipsOutroWhenPlaybackReachesOutroStart() {
        val target = IntroOutroSkipPolicy.skipTarget(
            enabled = true,
            currentPositionMs = 111_000,
            durationMs = 120_000,
            introSeconds = 0,
            outroSeconds = 10,
            hasSkippedIntro = true,
            hasSkippedOutro = false
        )

        assertEquals(IntroOutroSkipPolicy.Target(120_000, IntroOutroSkipPolicy.Kind.OUTRO), target)
    }

    @Test
    fun ignoresInvalidDurationForOutro() {
        val target = IntroOutroSkipPolicy.skipTarget(
            enabled = true,
            currentPositionMs = 111_000,
            durationMs = 0,
            introSeconds = 0,
            outroSeconds = 10,
            hasSkippedIntro = true,
            hasSkippedOutro = false
        )

        assertNull(target)
    }
}
