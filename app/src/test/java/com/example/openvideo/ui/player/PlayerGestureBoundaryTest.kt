package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.GestureAction
import org.junit.Assert.*
import org.junit.Test

class PlayerGestureBoundaryTest {
    @Test fun zeroSizedScreensNeverSeekAndClampLevelAnchors() {
        assertFalse(PlayerGesturePolicy.isEdgeSwipe(1f, 0))
        assertEquals(PlayerSwipeSide.NONE, PlayerGesturePolicy.swipeSide(1f, 0))
        assertFalse(PlayerGesturePolicy.allowsVerticalLevelGesture(1f, 0))
        assertEquals(0L, PlayerGesturePolicy.horizontalSeekDeltaMs(10f, 0))
        assertEquals(0L, PlayerGesturePolicy.verticalSeekDeltaMs(10f, 0))
        assertEquals(1f, PlayerGesturePolicy.horizontalLevel(2f, 10f, 0), 0f)
        assertEquals(0f, PlayerGesturePolicy.verticalLevel(-1f, 10f, 0), 0f)
        assertTrue(PlayerGesturePolicy.isEdgeSwipe(99f, 100))
        assertFalse(PlayerGesturePolicy.isEdgeSwipe(50f, 100))
        assertTrue(PlayerGesturePolicy.allowsVerticalLevelGesture(80f, 100))
        assertFalse(PlayerGesturePolicy.allowsVerticalLevelGesture(81f, 100))
    }

    @Test fun releaseActionsRequireBothSwipeAndSeekAndSlopUsesBothAxes() {
        for (swipe in listOf(false, true)) for (action in GestureAction.entries) {
            assertEquals(swipe && action == GestureAction.SEEK, PlayerGesturePolicy.shouldApplyHorizontalSeekOnRelease(swipe, action))
            assertEquals(swipe && action == GestureAction.SEEK, PlayerGesturePolicy.shouldApplyVerticalSeekOnRelease(swipe, action))
        }
        assertEquals(PlayerSwipeAxis.NONE, PlayerGesturePolicy.dominantAxis(40f, 40f, 40))
        assertEquals(PlayerSwipeAxis.VERTICAL, PlayerGesturePolicy.dominantAxis(40f, 41f, 40))
        assertEquals(PlayerSwipeAxis.HORIZONTAL, PlayerGesturePolicy.dominantAxis(41f, 40f, 40))
        assertFalse(PlayerGesturePolicy.isValidDoubleTapSeekSide(PlayerSwipeSide.NONE))
        assertTrue(PlayerGesturePolicy.isValidDoubleTapSeekSide(PlayerSwipeSide.LEFT))
        assertEquals(GestureAction.NONE, PlayerGesturePolicy.verticalGestureAction(PlayerSwipeSide.NONE, GestureAction.SEEK, GestureAction.SEEK))
    }

    @Test fun speedChoicesAndSheetSettingsHaveStableBoundaries() {
        assertEquals(listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f), PlayerPlaybackSpeedOptions.entries)
        assertEquals(16, PlayerQuickEntryDialogPolicy.sheetPaddingPx(2f))
        assertFalse(PlayerSettingsSheetStylePolicy.supportsBackdropBlur(30))
        assertTrue(PlayerSettingsSheetStylePolicy.supportsBackdropBlur(31))
        assertEquals(PlayerSettingsSheetStyle(0f, 1f, 0), PlayerSettingsSheetStylePolicy.compute(-1, 101, -1, 2f))
        assertEquals(PlayerSettingsSheetStyle(1f, 0f, 128), PlayerSettingsSheetStylePolicy.compute(100, 0, 100, 2f))
    }
}
