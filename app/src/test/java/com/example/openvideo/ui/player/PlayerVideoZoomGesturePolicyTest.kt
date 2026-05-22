package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVideoZoomGesturePolicyTest {

    @Test
    fun multiTouchHandledOnlyWhenZoomAllowed() {
        assertTrue(PlayerVideoZoomGesturePolicy.handlesMultiTouch(zoomAllowed = true, pointerCount = 2))
        assertFalse(PlayerVideoZoomGesturePolicy.handlesMultiTouch(zoomAllowed = false, pointerCount = 2))
        assertFalse(PlayerVideoZoomGesturePolicy.handlesMultiTouch(zoomAllowed = true, pointerCount = 1))
    }

    @Test
    fun singleFingerInterceptOnlyWhenZoomActive() {
        assertFalse(
            PlayerVideoZoomGesturePolicy.interceptsSingleFingerGestures(
                zoomAllowed = true,
                manual = PlayerVideoZoomState.IDENTITY
            )
        )
        assertTrue(
            PlayerVideoZoomGesturePolicy.interceptsSingleFingerGestures(
                zoomAllowed = true,
                manual = PlayerVideoZoomState(scale = 1.5f)
            )
        )
    }

    @Test
    fun doubleTapResetsOnlyWhenZoomActiveAndAllowed() {
        assertFalse(
            PlayerVideoZoomGesturePolicy.doubleTapResetsZoom(
                zoomAllowed = true,
                manual = PlayerVideoZoomState.IDENTITY
            )
        )
        assertTrue(
            PlayerVideoZoomGesturePolicy.doubleTapResetsZoom(
                zoomAllowed = true,
                manual = PlayerVideoZoomState(scale = 1.4f)
            )
        )
    }
}
