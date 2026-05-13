package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerGestureHudPolicyTest {

    @Test
    fun seekHudShowsSignedDeltaTargetAndDuration() {
        val hud = PlayerGestureHudPolicy.seek(
            targetMs = 75_000,
            durationMs = 600_000,
            deltaMs = 15_000
        )

        assertEquals(PlayerGestureHudKind.SEEK, hud.kind)
        assertEquals("+00:15", hud.primaryText)
        assertEquals("01:15 / 10:00", hud.secondaryText)
    }

    @Test
    fun levelHudRoundsToPercent() {
        val hud = PlayerGestureHudPolicy.level(PlayerGestureHudKind.BRIGHTNESS, 0.426f)

        assertEquals(PlayerGestureHudKind.BRIGHTNESS, hud.kind)
        assertEquals("43%", hud.primaryText)
        assertEquals(43, hud.levelPercent)
    }

    @Test
    fun doubleTapSeekUsesScreenSideAndAccumulatesWithinTimeout() {
        val first = PlayerGestureHudPolicy.doubleTapSeek(
            previous = null,
            tapSide = PlayerSwipeSide.RIGHT,
            intervalMs = 10_000,
            nowMs = 1_000
        )
        val second = PlayerGestureHudPolicy.doubleTapSeek(
            previous = first.nextState,
            tapSide = PlayerSwipeSide.RIGHT,
            intervalMs = 10_000,
            nowMs = 1_400
        )

        assertEquals(10_000L, first.deltaMs)
        assertEquals(false, first.isAccumulated)
        assertEquals("+00:10", first.hud.primaryText)
        assertEquals(20_000L, second.deltaMs)
        assertEquals(true, second.isAccumulated)
        assertEquals("+00:20", second.hud.primaryText)
    }

    @Test
    fun doubleTapSeekResetsWhenSideChangesOrTimeoutPasses() {
        val first = PlayerGestureHudPolicy.doubleTapSeek(
            previous = null,
            tapSide = PlayerSwipeSide.RIGHT,
            intervalMs = 10_000,
            nowMs = 1_000
        )
        val sideChanged = PlayerGestureHudPolicy.doubleTapSeek(
            previous = first.nextState,
            tapSide = PlayerSwipeSide.LEFT,
            intervalMs = 10_000,
            nowMs = 1_300
        )
        val timedOut = PlayerGestureHudPolicy.doubleTapSeek(
            previous = first.nextState,
            tapSide = PlayerSwipeSide.RIGHT,
            intervalMs = 10_000,
            nowMs = 2_000
        )

        assertEquals(-10_000L, sideChanged.deltaMs)
        assertEquals("-00:10", sideChanged.hud.primaryText)
        assertEquals(10_000L, timedOut.deltaMs)
        assertEquals("+00:10", timedOut.hud.primaryText)
    }

    @Test
    fun doubleTapSeekDoesNotTreatNoSideAsForwardSeek() {
        val result = PlayerGestureHudPolicy.doubleTapSeek(
            previous = null,
            tapSide = PlayerSwipeSide.NONE,
            intervalMs = 10_000,
            nowMs = 1_000
        )

        assertEquals(0L, result.deltaMs)
        assertEquals("+00:00", result.hud.primaryText)
        assertEquals(0L, result.nextState.accumulatedMs)
        assertEquals(false, result.isAccumulated)
    }
}
