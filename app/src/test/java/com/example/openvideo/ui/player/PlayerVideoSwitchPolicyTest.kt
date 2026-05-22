package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerVideoSwitchPolicyTest {

    @Test
    fun newVideoResetsMediaBoundPlaybackState() {
        val state = PlayerVideoSwitchPolicy.resetForNewVideo()

        assertEquals(false, state.hasSkippedIntro)
        assertEquals(false, state.hasSkippedOutro)
        assertEquals(PlayerAbLoopState.IDLE, state.abLoopState)
        assertEquals(-1L, state.abLoopPointA)
        assertEquals(-1L, state.abLoopPointB)
        assertNull(state.pendingSeekTarget)
        assertNull(state.seekGestureAnchorPositionMs)
        assertNull(state.doubleTapSeekState)
        assertNull(state.doubleTapSeekAnchorPositionMs)
        assertEquals(false, state.keepGestureHudAfterActionUp)
    }

    @Test
    fun newVideoStartsWithFreshFirstFrameScrimState() {
        val state = PlayerVideoSwitchPolicy.resetForNewVideo()

        assertEquals(true, state.awaitFirstFrame)
        assertEquals(PlayerVideoZoomState.IDENTITY, state.manualVideoZoom)
    }
}
