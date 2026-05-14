package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerFirstFramePolicyTest {

    @Test
    fun renderedFirstFrameHidesScrimAndClearsWaiting() {
        val decision = PlayerFirstFramePolicy.onRenderedFirstFrame(isAwaitingFirstFrame = true)

        assertEquals(true, decision.hideScrim)
        assertEquals(false, decision.nextAwaitingFirstFrame)
    }

    @Test
    fun renderedFirstFrameStillHidesScrimWhenAwaitingStateIsStale() {
        val decision = PlayerFirstFramePolicy.onRenderedFirstFrame(isAwaitingFirstFrame = false)

        assertEquals(true, decision.hideScrim)
        assertEquals(false, decision.nextAwaitingFirstFrame)
    }

    @Test
    fun audioOnlyReadyHidesScrimWhileAwaitingFirstFrame() {
        val decision = PlayerFirstFramePolicy.onReady(
            isAwaitingFirstFrame = true,
            hasVideoTrack = false
        )

        assertEquals(true, decision.hideScrim)
        assertEquals(false, decision.nextAwaitingFirstFrame)
    }

    @Test
    fun videoReadyKeepsScrimUntilFirstFrameRenders() {
        val decision = PlayerFirstFramePolicy.onReady(
            isAwaitingFirstFrame = true,
            hasVideoTrack = true
        )

        assertEquals(false, decision.hideScrim)
        assertEquals(true, decision.nextAwaitingFirstFrame)
    }

    @Test
    fun readyDoesNothingWhenScrimIsNotAwaiting() {
        val decision = PlayerFirstFramePolicy.onReady(
            isAwaitingFirstFrame = false,
            hasVideoTrack = false
        )

        assertEquals(false, decision.hideScrim)
        assertEquals(false, decision.nextAwaitingFirstFrame)
    }

    @Test
    fun showForNewMediaMakesScrimVisibleAndAwaiting() {
        val decision = PlayerFirstFramePolicy.onShowForNewMedia()

        assertEquals(true, decision.showScrim)
        assertEquals(true, decision.nextAwaitingFirstFrame)
    }
}
