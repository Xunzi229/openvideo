package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDoubleTapSeekPolicyTest {

    @Test
    fun firstRightTapSeeksForwardFromCurrentPosition() {
        val preview = PlayerDoubleTapSeekPolicy.preview(
            previous = null,
            tapSide = PlayerSwipeSide.RIGHT,
            intervalMs = 10_000,
            nowMs = 1_000,
            anchorPositionMs = null,
            currentPositionMs = 60_000,
            durationMs = 180_000
        )

        assertEquals(10_000L, preview.deltaMs)
        assertEquals(70_000L, preview.targetMs)
        assertEquals(60_000L, preview.anchorPositionMs)
        assertTrue(preview.seekable)
        assertFalse(preview.isAccumulated)
    }

    @Test
    fun repeatedSameSideTapAccumulatesFromStableAnchor() {
        val first = PlayerDoubleTapSeekPolicy.preview(
            previous = null,
            tapSide = PlayerSwipeSide.RIGHT,
            intervalMs = 10_000,
            nowMs = 1_000,
            anchorPositionMs = null,
            currentPositionMs = 60_000,
            durationMs = 180_000
        )
        val second = PlayerDoubleTapSeekPolicy.preview(
            previous = first.nextState,
            tapSide = PlayerSwipeSide.RIGHT,
            intervalMs = 10_000,
            nowMs = 1_300,
            anchorPositionMs = first.anchorPositionMs,
            currentPositionMs = 61_000,
            durationMs = 180_000
        )

        assertEquals(20_000L, second.deltaMs)
        assertEquals(80_000L, second.targetMs)
        assertEquals(60_000L, second.anchorPositionMs)
        assertTrue(second.isAccumulated)
    }

    @Test
    fun sideChangeResetsAccumulationAndUsesCurrentPosition() {
        val first = PlayerDoubleTapSeekPolicy.preview(
            previous = null,
            tapSide = PlayerSwipeSide.RIGHT,
            intervalMs = 10_000,
            nowMs = 1_000,
            anchorPositionMs = null,
            currentPositionMs = 60_000,
            durationMs = 180_000
        )
        val changed = PlayerDoubleTapSeekPolicy.preview(
            previous = first.nextState,
            tapSide = PlayerSwipeSide.LEFT,
            intervalMs = 10_000,
            nowMs = 1_300,
            anchorPositionMs = first.anchorPositionMs,
            currentPositionMs = 90_000,
            durationMs = 180_000
        )

        assertEquals(-10_000L, changed.deltaMs)
        assertEquals(80_000L, changed.targetMs)
        assertEquals(90_000L, changed.anchorPositionMs)
        assertFalse(changed.isAccumulated)
    }

    @Test
    fun unknownDurationKeepsPreviewButDisablesSeekCommit() {
        val preview = PlayerDoubleTapSeekPolicy.preview(
            previous = null,
            tapSide = PlayerSwipeSide.RIGHT,
            intervalMs = 10_000,
            nowMs = 1_000,
            anchorPositionMs = null,
            currentPositionMs = 60_000,
            durationMs = 0
        )

        assertEquals(70_000L, preview.targetMs)
        assertFalse(preview.seekable)
        assertNull(preview.timelineFraction)
    }
}
