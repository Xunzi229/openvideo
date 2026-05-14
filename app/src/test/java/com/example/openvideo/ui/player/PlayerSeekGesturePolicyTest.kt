package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSeekGesturePolicyTest {

    @Test
    fun horizontalSeekDeltaScalesWithGestureSensitivity() {
        assertEquals(15_000L, PlayerSeekGesturePolicy.horizontalDeltaMs(dx = 500f, screenWidthPx = 1000, sensitivity = 1))
        assertEquals(30_000L, PlayerSeekGesturePolicy.horizontalDeltaMs(dx = 500f, screenWidthPx = 1000, sensitivity = 2))
        assertEquals(45_000L, PlayerSeekGesturePolicy.horizontalDeltaMs(dx = 500f, screenWidthPx = 1000, sensitivity = 3))
    }

    @Test
    fun horizontalPreviewUsesGestureAnchorInsteadOfChangingCurrentPosition() {
        val preview = PlayerSeekGesturePolicy.horizontalPreview(
            anchorPositionMs = 60_000,
            dx = 500f,
            screenWidthPx = 1000,
            durationMs = 180_000,
            sensitivity = 2
        )

        assertEquals(30_000L, preview.deltaMs)
        assertEquals(90_000L, preview.targetMs)
        assertEquals(0.5f, preview.timelineFraction!!, 0.001f)
        assertTrue(preview.seekable)
    }

    @Test
    fun horizontalPreviewClampsToTimelineBounds() {
        val rewind = PlayerSeekGesturePolicy.horizontalPreview(
            anchorPositionMs = 10_000,
            dx = -1_000f,
            screenWidthPx = 1_000,
            durationMs = 120_000,
            sensitivity = 3
        )
        val forward = PlayerSeekGesturePolicy.horizontalPreview(
            anchorPositionMs = 115_000,
            dx = 1_000f,
            screenWidthPx = 1_000,
            durationMs = 120_000,
            sensitivity = 3
        )

        assertEquals(0L, rewind.targetMs)
        assertTrue(rewind.isClamped)
        assertEquals(120_000L, forward.targetMs)
        assertTrue(forward.isClamped)
    }

    @Test
    fun horizontalPreviewKeepsUnknownDurationNonNegativeButNotSeekable() {
        val preview = PlayerSeekGesturePolicy.horizontalPreview(
            anchorPositionMs = 5_000,
            dx = -500f,
            screenWidthPx = 1000,
            durationMs = 0,
            sensitivity = 2
        )

        assertEquals(0L, preview.targetMs)
        assertFalse(preview.seekable)
        assertEquals(null, preview.timelineFraction)
    }

    @Test
    fun verticalSeekPreviewUsesSameSensitivityWindowWithUpAsForward() {
        val preview = PlayerSeekGesturePolicy.verticalPreview(
            anchorPositionMs = 60_000,
            dy = -500f,
            screenHeightPx = 1000,
            durationMs = 180_000,
            sensitivity = 2
        )

        assertEquals(30_000L, preview.deltaMs)
        assertEquals(90_000L, preview.targetMs)
        assertEquals(0.5f, preview.timelineFraction!!, 0.001f)
    }

    @Test
    fun verticalPreviewKeepsUnknownDurationNonSeekable() {
        val preview = PlayerSeekGesturePolicy.verticalPreview(
            anchorPositionMs = 5_000,
            dy = -500f,
            screenHeightPx = 1000,
            durationMs = 0,
            sensitivity = 2
        )

        assertEquals(35_000L, preview.targetMs)
        assertFalse(preview.seekable)
        assertEquals(null, preview.timelineFraction)
    }
}
