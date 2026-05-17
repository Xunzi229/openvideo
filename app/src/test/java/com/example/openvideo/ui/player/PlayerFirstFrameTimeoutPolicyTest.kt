package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerFirstFrameTimeoutPolicyTest {

    @Test
    fun scheduleDelayReturnsDefaultWhenVideoTrackPresentAndNoFirstFrameYet() {
        val delay = PlayerFirstFrameTimeoutPolicy.scheduleDelayMs(
            hasVideoTrack = true,
            firstFrameRendered = false,
            alreadyTimedOut = false
        )
        assertEquals(PlayerFirstFrameTimeoutPolicy.DEFAULT_TIMEOUT_MS, delay)
    }

    @Test
    fun scheduleDelayReturnsNullWhenNoVideoTrack() {
        val delay = PlayerFirstFrameTimeoutPolicy.scheduleDelayMs(
            hasVideoTrack = false,
            firstFrameRendered = false,
            alreadyTimedOut = false
        )
        assertNull(delay)
    }

    @Test
    fun scheduleDelayReturnsNullWhenFirstFrameAlreadyRendered() {
        val delay = PlayerFirstFrameTimeoutPolicy.scheduleDelayMs(
            hasVideoTrack = true,
            firstFrameRendered = true,
            alreadyTimedOut = false
        )
        assertNull(delay)
    }

    @Test
    fun scheduleDelayReturnsNullWhenAlreadyMarkedTimedOut() {
        val delay = PlayerFirstFrameTimeoutPolicy.scheduleDelayMs(
            hasVideoTrack = true,
            firstFrameRendered = false,
            alreadyTimedOut = true
        )
        assertNull(delay)
    }

    @Test
    fun customTimeoutIsHonoredAndCoercedToZero() {
        val delay = PlayerFirstFrameTimeoutPolicy.scheduleDelayMs(
            hasVideoTrack = true,
            firstFrameRendered = false,
            alreadyTimedOut = false,
            timeoutMs = -100L
        )
        assertEquals(0L, delay)
    }

    @Test
    fun isFirstFrameLateReturnsFalseBeforePrepareReady() {
        assertFalse(
            PlayerFirstFrameTimeoutPolicy.isFirstFrameLate(
                prepareReadyElapsedMs = null,
                firstFrameElapsedMs = 500L
            )
        )
    }

    @Test
    fun isFirstFrameLateReturnsTrueWhenFirstFrameMissing() {
        assertTrue(
            PlayerFirstFrameTimeoutPolicy.isFirstFrameLate(
                prepareReadyElapsedMs = 1_000L,
                firstFrameElapsedMs = null
            )
        )
    }

    @Test
    fun isFirstFrameLateReturnsTrueWhenAboveThreshold() {
        assertTrue(
            PlayerFirstFrameTimeoutPolicy.isFirstFrameLate(
                prepareReadyElapsedMs = 1_000L,
                firstFrameElapsedMs = 1_000L + PlayerFirstFrameTimeoutPolicy.DEFAULT_TIMEOUT_MS
            )
        )
    }

    @Test
    fun isFirstFrameLateReturnsFalseWithinThreshold() {
        assertFalse(
            PlayerFirstFrameTimeoutPolicy.isFirstFrameLate(
                prepareReadyElapsedMs = 1_000L,
                firstFrameElapsedMs = 1_500L
            )
        )
    }
}
