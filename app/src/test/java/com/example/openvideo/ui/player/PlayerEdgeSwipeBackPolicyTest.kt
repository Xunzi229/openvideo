package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerEdgeSwipeBackPolicyTest {

    @Test
    fun finishesWhenEdgeSwipeBackDragExceedsThreshold() {
        assertTrue(
            PlayerEdgeSwipeBackPolicy.shouldFinish(
                edgeSwipeBackEnabled = true,
                isEdgeSwipe = true,
                isHorizontalSwipe = true,
                dragDxPx = 101f
            )
        )
    }

    @Test
    fun doesNotFinishWhenDragBelowThreshold() {
        assertFalse(
            PlayerEdgeSwipeBackPolicy.shouldFinish(
                edgeSwipeBackEnabled = true,
                isEdgeSwipe = true,
                isHorizontalSwipe = true,
                dragDxPx = 100f
            )
        )
    }

    @Test
    fun requiresEdgeSwipeBackEnabled() {
        assertFalse(
            PlayerEdgeSwipeBackPolicy.shouldFinish(
                edgeSwipeBackEnabled = false,
                isEdgeSwipe = true,
                isHorizontalSwipe = true,
                dragDxPx = 200f
            )
        )
    }
}
