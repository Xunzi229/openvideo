package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.GestureAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGesturePolicyTest {

    @Test
    fun mapsSensitivityToSwipeSlopPixels() {
        assertEquals(60, PlayerGesturePolicy.gestureSlopPx(1))
        assertEquals(50, PlayerGesturePolicy.gestureSlopPx(2))
        assertEquals(40, PlayerGesturePolicy.gestureSlopPx(3))
    }

    @Test
    fun detectsSwipeSideFromScreenHalf() {
        assertEquals(PlayerSwipeSide.LEFT, PlayerGesturePolicy.swipeSide(x = 499f, screenWidthPx = 1000))
        assertEquals(PlayerSwipeSide.RIGHT, PlayerGesturePolicy.swipeSide(x = 500f, screenWidthPx = 1000))
        assertEquals(PlayerSwipeSide.NONE, PlayerGesturePolicy.swipeSide(x = 100f, screenWidthPx = 0))
    }

    @Test
    fun detectsEdgeSwipeFromFivePercentScreenEdges() {
        assertEquals(50f, PlayerGesturePolicy.edgeThresholdPx(screenWidthPx = 1000), 0.001f)
        assertEquals(true, PlayerGesturePolicy.isEdgeSwipe(x = 40f, screenWidthPx = 1000))
        assertEquals(true, PlayerGesturePolicy.isEdgeSwipe(x = 960f, screenWidthPx = 1000))
        assertEquals(false, PlayerGesturePolicy.isEdgeSwipe(x = 500f, screenWidthPx = 1000))
    }

    @Test
    fun choosesDominantSwipeAxisAfterSlop() {
        assertEquals(PlayerSwipeAxis.NONE, PlayerGesturePolicy.dominantAxis(dx = 20f, dy = 10f, slopPx = 50))
        assertEquals(PlayerSwipeAxis.HORIZONTAL, PlayerGesturePolicy.dominantAxis(dx = 80f, dy = 20f, slopPx = 50))
        assertEquals(PlayerSwipeAxis.VERTICAL, PlayerGesturePolicy.dominantAxis(dx = 20f, dy = 80f, slopPx = 50))
    }

    @Test
    fun mapsHorizontalAndVerticalSwipeToSeekDelta() {
        assertEquals(30_000L, PlayerGesturePolicy.horizontalSeekDeltaMs(dx = 500f, screenWidthPx = 1000))
        assertEquals(-30_000L, PlayerGesturePolicy.horizontalSeekDeltaMs(dx = -500f, screenWidthPx = 1000))
        assertEquals(30_000L, PlayerGesturePolicy.verticalSeekDeltaMs(dy = -500f, screenHeightPx = 1000))
    }

    @Test
    fun mapsVerticalDragToBrightnessOrVolumeValue() {
        assertEquals(0.7f, PlayerGesturePolicy.verticalLevel(anchor = 0.5f, dy = -200f, screenHeightPx = 1000), 0.001f)
        assertEquals(0.3f, PlayerGesturePolicy.verticalLevel(anchor = 0.5f, dy = 200f, screenHeightPx = 1000), 0.001f)
        assertEquals(0.01f, PlayerGesturePolicy.verticalLevel(anchor = 0.5f, dy = 1000f, screenHeightPx = 1000, min = 0.01f), 0.001f)
    }

    @Test
    fun horizontalSeekOnReleaseRequiresSeekAction() {
        assertTrue(
            PlayerGesturePolicy.shouldApplyHorizontalSeekOnRelease(
                isHorizontalSwipe = true,
                horizontalSwipeAction = GestureAction.SEEK
            )
        )
        assertFalse(
            PlayerGesturePolicy.shouldApplyHorizontalSeekOnRelease(
                isHorizontalSwipe = true,
                horizontalSwipeAction = GestureAction.BRIGHTNESS
            )
        )
    }

    @Test
    fun excludesBottomFifthFromVerticalLevelGestures() {
        assertTrue(PlayerGesturePolicy.allowsVerticalLevelGesture(yPx = 800f, screenHeightPx = 1000))
        assertFalse(PlayerGesturePolicy.allowsVerticalLevelGesture(yPx = 801f, screenHeightPx = 1000))
        assertFalse(PlayerGesturePolicy.allowsVerticalLevelGesture(yPx = 999f, screenHeightPx = 1000))
    }

    @Test
    fun verticalGestureActionMapsBySide() {
        assertEquals(
            GestureAction.BRIGHTNESS,
            PlayerGesturePolicy.verticalGestureAction(
                PlayerSwipeSide.LEFT,
                GestureAction.BRIGHTNESS,
                GestureAction.VOLUME
            )
        )
        assertEquals(
            GestureAction.NONE,
            PlayerGesturePolicy.verticalGestureAction(
                PlayerSwipeSide.NONE,
                GestureAction.BRIGHTNESS,
                GestureAction.VOLUME
            )
        )
    }
}
