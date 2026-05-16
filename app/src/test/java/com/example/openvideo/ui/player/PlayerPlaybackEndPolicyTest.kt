package com.example.openvideo.ui.player

import com.example.openvideo.core.prefs.LoopMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerPlaybackEndPolicyTest {

    @Test
    fun singleVideoStopsAtEndWhenNoLoopOrReturnBehaviorIsRequested() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 1,
            autoPlayNext = true,
            loopMode = LoopMode.OFF,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            returnToListWhenDone = false
        )

        assertEquals(PlayerPlaybackEndAction.STOP_AT_END, decision.action)
        assertNull(decision.nextIndex)
        assertNull(decision.seekPositionMs)
    }

    @Test
    fun singleLoopReplaysCurrentVideoFromStart() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 1,
            autoPlayNext = true,
            loopMode = LoopMode.SINGLE,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            returnToListWhenDone = false
        )

        assertEquals(PlayerPlaybackEndAction.REPLAY_CURRENT, decision.action)
        assertEquals(0L, decision.seekPositionMs)
        assertNull(decision.nextIndex)
    }

    @Test
    fun listLoopWithAutoNextAdvancesToNextQueueItem() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 3,
            autoPlayNext = true,
            loopMode = LoopMode.LIST,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            returnToListWhenDone = false
        )

        assertEquals(PlayerPlaybackEndAction.PLAY_NEXT, decision.action)
        assertEquals(1, decision.nextIndex)
        assertNull(decision.seekPositionMs)
    }

    @Test
    fun activeAbLoopReplaysFromPointABeforeQueueAdvance() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 3,
            autoPlayNext = true,
            loopMode = LoopMode.LIST,
            abLoopState = PlayerAbLoopState.LOOPING,
            abLoopPointA = 42_000L,
            returnToListWhenDone = false
        )

        assertEquals(PlayerPlaybackEndAction.REPLAY_CURRENT, decision.action)
        assertEquals(42_000L, decision.seekPositionMs)
        assertNull(decision.nextIndex)
    }

    @Test
    fun returnToListBehaviorWinsWhenQueueCannotAdvance() {
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = 0,
            queueSize = 1,
            autoPlayNext = false,
            loopMode = LoopMode.OFF,
            abLoopState = PlayerAbLoopState.IDLE,
            abLoopPointA = -1L,
            returnToListWhenDone = true
        )

        assertEquals(PlayerPlaybackEndAction.RETURN_TO_LIST, decision.action)
        assertNull(decision.nextIndex)
        assertNull(decision.seekPositionMs)
    }
}
